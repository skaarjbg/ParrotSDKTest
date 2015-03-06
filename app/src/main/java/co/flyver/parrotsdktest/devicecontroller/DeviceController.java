package co.flyver.parrotsdktest.devicecontroller;

import android.content.Context;
import android.util.Log;

import com.parrot.arsdk.ardiscovery.ARDiscoveryConnection;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceBLEService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.arnetwork.ARNetworkIOBufferParam;
import com.parrot.arsdk.arnetwork.ARNetworkManager;
import com.parrot.arsdk.arnetworkal.ARNETWORKAL_ERROR_ENUM;
import com.parrot.arsdk.arnetworkal.ARNETWORKAL_FRAME_TYPE_ENUM;
import com.parrot.arsdk.arnetworkal.ARNetworkALManager;
import com.parrot.arsdk.arsal.ARSALPrint;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Created by Petar Petrov on 3/6/15.
 */
public class DeviceController {

    private final static String TAG = "DeviceController";
    public final static int iobufferC2dNack = 10;
    public final static int iobufferC2dAck = 11;
    public final static int iobufferC2dEmergency = 12;

    public static int iobufferD2cNavdata = (ARNetworkALManager.ARNETWORKAL_MANAGER_BLE_ID_MAX / 2) - 1;
    public static int iobufferD2cEvents = (ARNetworkALManager.ARNETWORKAL_MANAGER_BLE_ID_MAX / 2) - 2;
    public static int ackOffset = (ARNetworkALManager.ARNETWORKAL_MANAGER_BLE_ID_MAX / 2);
    protected static List<ARNetworkIOBufferParam> c2dParams = new ArrayList<>();
    protected static List<ARNetworkIOBufferParam> d2cParams = new ArrayList<>();
    protected static int commandsBuffers[] = {};
    protected static int bleNotificationIDs[] = new int[]{iobufferD2cNavdata, iobufferD2cEvents, (iobufferC2dAck + ackOffset), (iobufferC2dEmergency + ackOffset)};

    private Context context;

    private ARNetworkALManager alManager;
    private ARNetworkManager netManager;
    private boolean mediaOpened;

    private Thread rxThread;
    private Thread txThread;

    private List<ReaderThread> readerThreads;

    private LooperThread looperThread;

    private PositionCommandContainer currentPosition;
    private ARDiscoveryDeviceService deviceService;

    static {
        c2dParams.clear();
        c2dParams.add(new ARNetworkIOBufferParam(iobufferC2dNack,
                ARNETWORKAL_FRAME_TYPE_ENUM.ARNETWORKAL_FRAME_TYPE_DATA,
                20,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_INFINITE_NUMBER,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_INFINITE_NUMBER,
                1,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_DATACOPYMAXSIZE_USE_MAX,
                true));
        c2dParams.add(new ARNetworkIOBufferParam(iobufferC2dAck,
                ARNETWORKAL_FRAME_TYPE_ENUM.ARNETWORKAL_FRAME_TYPE_DATA_WITH_ACK,
                20,
                500,
                3,
                20,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_DATACOPYMAXSIZE_USE_MAX,
                false));
        c2dParams.add(new ARNetworkIOBufferParam(iobufferC2dEmergency,
                ARNETWORKAL_FRAME_TYPE_ENUM.ARNETWORKAL_FRAME_TYPE_DATA_WITH_ACK,
                1,
                100,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_INFINITE_NUMBER,
                1,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_DATACOPYMAXSIZE_USE_MAX,
                false));

        d2cParams.clear();
        d2cParams.add(new ARNetworkIOBufferParam(iobufferD2cNavdata,
                ARNETWORKAL_FRAME_TYPE_ENUM.ARNETWORKAL_FRAME_TYPE_DATA,
                20,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_INFINITE_NUMBER,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_INFINITE_NUMBER,
                20,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_DATACOPYMAXSIZE_USE_MAX,
                false));
        d2cParams.add(new ARNetworkIOBufferParam(iobufferD2cEvents,
                ARNETWORKAL_FRAME_TYPE_ENUM.ARNETWORKAL_FRAME_TYPE_DATA_WITH_ACK,
                20,
                500,
                3,
                20,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_DATACOPYMAXSIZE_USE_MAX,
                false));

        commandsBuffers = new int[]{
                iobufferD2cNavdata,
                iobufferD2cEvents,
        };

    }

    public PositionCommandContainer getCurrentPosition() {
        return currentPosition;
    }

    public DeviceController (android.content.Context context, ARDiscoveryDeviceService service)
    {
        currentPosition = new PositionCommandContainer();
        deviceService = service;
        this.context = context;
        readerThreads = new ArrayList<>();
    }

    public boolean start()
    {
        Log.d(TAG, "start ...");

        boolean failed = false;

//        registerARCommandsListener ();

        failed = startNetwork();

        if (!failed)
        {
            /* start the reader threads */
            startReadThreads();
        }

        if (!failed)
        {
                /* start the looper thread */
            startLooperThread();
        }

        return failed;
    }

    private boolean startNetwork()
    {
        ARNETWORKAL_ERROR_ENUM netALError;
        boolean failed = false;
        int pingDelay = 0; /* 0 means default, -1 means no ping */

        /* Create the looper ARNetworkALManager */
        alManager = new ARNetworkALManager();


        /* setup ARNetworkAL for BLE */

        ARDiscoveryDeviceBLEService bleDevice = (ARDiscoveryDeviceBLEService) deviceService.getDevice();

        netALError = alManager.initBLENetwork(context, bleDevice.getBluetoothDevice(), 1, bleNotificationIDs);

        if (netALError == ARNETWORKAL_ERROR_ENUM.ARNETWORKAL_OK)
        {
            mediaOpened = true;
            pingDelay = -1; /* Disable ping for BLE networks */
        }
        else
        {
            ARSALPrint.e(TAG, "error occured: " + netALError.toString());
            failed = true;
        }

        if (!failed)
        {
            /* Create the ARNetworkManager */
            netManager = new ARNetworkManagerExtended(alManager, c2dParams.toArray(new ARNetworkIOBufferParam[c2dParams.size()]), d2cParams.toArray(new ARNetworkIOBufferParam[d2cParams.size()]), pingDelay);

            if (!netManager.isCorrectlyInitialized())
            {
                ARSALPrint.e (TAG, "new ARNetworkManager failed");
                failed = true;
            }
        }

        if (!failed)
        {
            /* Create and start Tx and Rx threads */
            rxThread = new Thread (netManager.m_receivingRunnable);
            rxThread.start();

            txThread = new Thread (netManager.m_sendingRunnable);
            txThread.start();
        }
        Log.d(TAG, "network started: ".concat(String.valueOf(failed)));

        return failed;
    }

    private void startReadThreads()
    {
        /* Create the reader threads */
        for (int bufferId : commandsBuffers)
        {
            ReaderThread readerThread = new ReaderThread(bufferId, netManager);
            readerThreads.add(readerThread);
        }

        /* Mark all reader threads as started */
        for (ReaderThread readerThread : readerThreads)
        {
            readerThread.start();
        }
        Log.d(TAG, "Reader threads started: ".concat(String.valueOf(readerThreads.size())));
    }

    private void startLooperThread()
    {
        /* Create the looper thread */
        looperThread = new ControllerThread(this, netManager);

        /* Start the looper thread. */
        looperThread.start();
        Log.d(TAG, "Looper thread started");
    }
}
