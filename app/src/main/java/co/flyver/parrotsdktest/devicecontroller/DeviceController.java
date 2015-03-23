package co.flyver.parrotsdktest.devicecontroller;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.parrot.arsdk.ardiscovery.ARDISCOVERY_ERROR_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryConnection;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceNetService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.arnetwork.ARNetworkIOBufferParam;
import com.parrot.arsdk.arnetwork.ARNetworkManager;
import com.parrot.arsdk.arnetworkal.ARNETWORKAL_ERROR_ENUM;
import com.parrot.arsdk.arnetworkal.ARNetworkALManager;
import com.parrot.arsdk.arsal.ARSALPrint;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
    private ARDiscoveryConnection connection;
    private boolean mediaOpened;
    private ARDroneNetworkConfig netConfig = new ARDroneNetworkConfig();

    private Thread rxThread;
    private Thread txThread;

    private List<ReaderThread> readerThreads;

    private LooperThread looperThread;
    private VideoThread videoThread;

    private PositionCommandContainer dronePosition;
    private ARDiscoveryDeviceService deviceService;

    private int c2dPort;
    private int d2cPort;

    private int videoFragmentSize;
    private int videoFragmentNumber;

    public PositionCommandContainer getDronePosition() {
        return dronePosition;
    }

    public void setDronePosition(PositionCommandContainer dronePosition) {
        this.dronePosition = dronePosition;
    }

    public ARNetworkManager getNetManager() {
        return netManager;
    }

    public DeviceController(android.content.Context context, ARDiscoveryDeviceService service) {
        dronePosition = new PositionCommandContainer();
        deviceService = service;
        this.context = context;
        readerThreads = new ArrayList<>();
    }

    public boolean start() {
        Log.d(TAG, "start ...");

        boolean failed = false;

        failed = startNetwork();

        if (!failed) {
            /* start the reader threads */
            startReadThreads();
        }

        if (!failed) {
                /* start the looper thread */
            startLooperThread();
        }

        startVideo();

        return failed;
    }

    private boolean startNetwork() {
        ARNETWORKAL_ERROR_ENUM netALError;
        boolean failed = false;
        int pingDelay = 0; /* 0 means default, -1 means no ping */

        /* Create the looper ARNetworkALManager */
        alManager = new ARNetworkALManager();


        /* setup ARNetworkAL for wifi */
        Log.d(TAG, "alManager.ARDiscoveryDeviceNetService ");

        ARDiscoveryDeviceNetService netDevice = (ARDiscoveryDeviceNetService) deviceService.getDevice();
        String discoveryIp = netDevice.getIp();
        int discoveryPort = netDevice.getPort();

        initiateConnection(discoveryPort, discoveryIp);

        Log.d(TAG, discoveryIp + " " + discoveryPort);

        netALError = alManager.initWifiNetwork(discoveryIp, c2dPort, d2cPort, 5);

        if (netALError == ARNETWORKAL_ERROR_ENUM.ARNETWORKAL_OK) {
            mediaOpened = true;
        } else {
            ARSALPrint.e(TAG, "error occured: " + netALError.toString());
            failed = true;
        }

//        ARDiscoveryDeviceBLEService bleDevice = (ARDiscoveryDeviceBLEService) deviceService.getDevice();
//
//        netALError = alManager.initBLENetwork(context, bleDevice.getBluetoothDevice(), 1, bleNotificationIDs);

        if (netALError == ARNETWORKAL_ERROR_ENUM.ARNETWORKAL_OK) {
            mediaOpened = true;
        pingDelay = 0; /* Disable ping for BLE networks */
        } else {
            ARSALPrint.e(TAG, "error occured: " + netALError.toString());
            failed = true;
        }

        if (!failed) {
            /* Create the ARNetworkManager */
            netManager = new ARNetworkManagerExtended(alManager, netConfig.getC2dParams(), netConfig.getD2cParams(), pingDelay);

            if (!netManager.isCorrectlyInitialized()) {
                ARSALPrint.e(TAG, "new ARNetworkManager failed");
                failed = true;
            }
        }

        if (!failed) {
            /* Create and start Tx and Rx threads */
            rxThread = new Thread(netManager.m_receivingRunnable);
            rxThread.start();

            txThread = new Thread(netManager.m_sendingRunnable);
            txThread.start();
        }
        Log.d(TAG, "network started: ".concat(String.valueOf(!failed)));

        return failed;
    }

    private void startReadThreads() {
        /* Create the reader threads */
        for (int bufferId : netConfig.getCommandsIOBuffers()) {
            ReaderThread readerThread = new ReaderThread(bufferId, netManager);
            readerThreads.add(readerThread);
        }

        /* Mark all reader threads as started */
        for (ReaderThread readerThread : readerThreads) {
            readerThread.start();
        }
        Log.d(TAG, "Reader threads started: ".concat(String.valueOf(readerThreads.size())));
    }

    private void startLooperThread() {
        /* Create the looper thread */
        looperThread = new ControllerThread(this, netManager);

        /* Start the looper thread. */
        looperThread.start();
        Log.d(TAG, "Looper thread started");
    }

    private void initiateConnection(int discoveryPort, String discoveryIp) {
        d2cPort = netConfig.getInboundPort();
        connection = new ARDiscoveryConnection() {
            @Override
            protected String onSendJson() {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put(ARDISCOVERY_CONNECTION_JSON_D2CPORT_KEY, d2cPort);
                    jsonObject.put(ARDISCOVERY_CONNECTION_JSON_CONTROLLER_NAME_KEY, Build.MODEL);
                    jsonObject.put(ARDISCOVERY_CONNECTION_JSON_CONTROLLER_TYPE_KEY, Build.DEVICE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return jsonObject.toString();
            }

            @Override
            protected ARDISCOVERY_ERROR_ENUM onReceiveJson(String dataRx, String ip) {
                ARDISCOVERY_ERROR_ENUM error = ARDISCOVERY_ERROR_ENUM.ARDISCOVERY_OK;
                Log.d(TAG, "JSON RECEIVED: ".concat(dataRx.concat(" from: ".concat(ip))));
                try {
                    JSONObject jsonObject = new JSONObject(dataRx);
                    c2dPort = jsonObject.getInt(ARDISCOVERY_CONNECTION_JSON_C2DPORT_KEY);
                    videoFragmentSize = jsonObject.getInt(ARDISCOVERY_CONNECTION_JSON_ARSTREAM_FRAGMENT_SIZE_KEY);
                    videoFragmentNumber = jsonObject.getInt(ARDISCOVERY_CONNECTION_JSON_ARSTREAM_FRAGMENT_MAXIMUM_NUMBER_KEY);
                } catch (JSONException e) {
                    e.printStackTrace();
                    error = ARDISCOVERY_ERROR_ENUM.ARDISCOVERY_ERROR;
                }
                return error;
            }
        };
        connection.ControllerConnection(discoveryPort, discoveryIp);
    }

    public void startVideo() {
        videoThread = new VideoThread(netManager, netConfig, videoFragmentSize, videoFragmentNumber);
        videoThread.start();
    }
}
