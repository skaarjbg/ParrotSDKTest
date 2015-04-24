package co.flyver.parrotsdktest.devicecontroller;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_GENERATOR_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCommand;
import com.parrot.arsdk.arcommands.ARCommandARDrone3MediaRecordStatePictureStateChangedListener;
import com.parrot.arsdk.arcommands.ARCommandARDrone3PilotingStateFlyingStateChangedListener;
import com.parrot.arsdk.arcommands.ARCommandARDrone3PilotingStateSpeedChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCommonStateAllStatesChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCommonStateBatteryStateChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonSettingsStateAllSettingsChangedListener;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_ERROR_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryConnection;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceNetService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.arnetwork.ARNETWORK_ERROR_ENUM;
import com.parrot.arsdk.arnetwork.ARNetworkIOBufferParam;
import com.parrot.arsdk.arnetwork.ARNetworkManager;
import com.parrot.arsdk.arnetworkal.ARNETWORKAL_ERROR_ENUM;
import com.parrot.arsdk.arnetworkal.ARNetworkALManager;
import com.parrot.arsdk.arsal.ARSALPrint;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;

import co.flyver.parrotsdktest.devicecontroller.config.ARDroneNetworkConfig;
import co.flyver.parrotsdktest.devicecontroller.containers.PositionCommandContainer;

/**
 * Created by Petar Petrov on 3/6/15.
 */
public class DeviceController implements ARCommandCommonSettingsStateAllSettingsChangedListener,
        ARCommandCommonCommonStateAllStatesChangedListener,
        ARCommandARDrone3PilotingStateFlyingStateChangedListener,
        ARCommandARDrone3MediaRecordStatePictureStateChangedListener,
        ARCommandCommonCommonStateBatteryStateChangedListener,
        ARCommandARDrone3PilotingStateSpeedChangedListener {

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
    private ARNetworkManagerExtended netManager;
    private ARDiscoveryConnection connection;
    private boolean mediaOpened;
    private ARDroneNetworkConfig netConfig = new ARDroneNetworkConfig();

    private Thread rxThread;
    private Thread txThread;

    private List<ReaderThread> readerThreads;

    private LooperThread looperThread;

    private PositionCommandContainer dronePosition;
    private ARDiscoveryDeviceService deviceService;

    private int c2dPort;
    private int d2cPort;

    private int videoFragmentSize;
    private int videoFragmentNumber;

    private Semaphore settingsSemaphore;
    private Semaphore stateSemaphore;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private Listener listener;

    public PositionCommandContainer getDronePosition() {
        return dronePosition;
    }

    public int getVideoFragmentSize() {
        return videoFragmentSize;
    }

    public int getVideoFragmentNumber() {
        return videoFragmentNumber;
    }

    public ARDroneNetworkConfig getNetConfig() {
        return netConfig;
    }

    public void setDronePosition(PositionCommandContainer dronePosition) {
        this.dronePosition = dronePosition;
    }

    public ARNetworkManager getNetManager() {
        return netManager;
    }

    @Override
    public void onCommonCommonStateBatteryStateChangedUpdate(byte percent) {
        Log.d(TAG, "Battery changed: " + percent);
    }

    @Override
    public void onARDrone3MediaRecordStatePictureStateChangedUpdate(byte state, byte mass_storage_id) {
        Log.d(TAG, "picture taken: " + state + " " + mass_storage_id);
        listener.pictureReady();
    }

    @Override
    public void onARDrone3PilotingStateSpeedChangedUpdate(float speedX, float speedY, float speedZ) {
        listener.speedChanged(speedX, speedY, speedZ);
    }


    public interface Listener {
        public void onDisconnect();
        public void onUpdateBattery(final byte percent);
        public void onFlyingStateChanged(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state);
        public void pictureReady();
        public void speedChanged(float speedX, float speedY, float speedZ);

    }

    public DeviceController(android.content.Context context, ARDiscoveryDeviceService service) {
        dronePosition = new PositionCommandContainer();
        deviceService = service;
        this.context = context;
        readerThreads = new ArrayList<>();
        settingsSemaphore = new Semaphore(0);
        stateSemaphore = new Semaphore(0);
    }

    public boolean start() {
        Log.d(TAG, "start ...");

        boolean failed = false;

        registerListeners();

        failed = startNetwork();

        if (!failed) {
            /* start the reader threads */
            startReadThreads();
        }

        if (!failed) {
                /* start the looper thread */
            startLooperThread();
        }

//        registerStream();

        if (!failed) {
            failed = !setTime();
        }

        if (!failed) {
            failed = !getInitialSettings();
        }

        if (!failed) {
            failed = !getInitialState();
        }

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
            netManager.setDisconnectedCallback(new ARNetworkManagerExtended.Disconnected() {
                @Override
                public void disconnected() {
                    stop();
                }
            });

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
                    netConfig.addStreamReaderIOBuffer(videoFragmentSize, videoFragmentNumber);
                } catch (JSONException e) {
                    e.printStackTrace();
                    error = ARDISCOVERY_ERROR_ENUM.ARDISCOVERY_ERROR;
                }
                return error;
            }
        };
        connection.ControllerConnection(discoveryPort, discoveryIp);
    }

    public void registerStream() {
        ARNETWORK_ERROR_ENUM error_enum;
        ARCommand cmd = new ARCommand();
        cmd.setARDrone3MediaStreamingVideoEnable((byte) 1);
        error_enum = netManager.sendData(DeviceController.iobufferC2dNack, cmd, null, true);
        Log.d(TAG, "Video Stream Requested: ".concat(error_enum.toString()));
        cmd.dispose();
    }

    public void stop() {
        for(ReaderThread t : readerThreads) {
           t.stopThread();
        }
        rxThread.interrupt();
        txThread.interrupt();
        looperThread.stopThread();
        alManager.closeWifiNetwork();
        netManager.stop();
        try {
            netManager.finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
//        looperThread.interrupt();
    }

    private boolean getInitialSettings() {

        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();
        cmdError = cmd.setCommonSettingsAllSettings();
        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK)
        {
            /* Send data with ARNetwork */
            // The commands sent by event should be sent to an buffer acknowledged  ; here iobufferC2dAck
            ARNETWORK_ERROR_ENUM netError = netManager.sendData (iobufferC2dAck, cmd, null, true);

            if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_OK)
            {
                ARSALPrint.e(TAG, "netManager.sendData() failed. " + netError.toString());
                sentStatus = false;
            }

            cmd.dispose();
        }

        if (!sentStatus)
        {
            ARSALPrint.e(TAG, "Failed to send AllSettings command.");
        }
        else
        {
            try
            {
                settingsSemaphore.acquire();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                sentStatus = false;
            }
        }
        return sentStatus;

    }

    private boolean getInitialState() {

        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setCommonCommonAllStates();
        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK) {
            /* Send data with ARNetwork */
            // The commands sent by event should be sent to an buffer acknowledged  ; here iobufferC2dAck
            ARNETWORK_ERROR_ENUM netError = netManager.sendData(iobufferC2dAck, cmd, null, true);

            if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_OK) {
                ARSALPrint.e(TAG, "netManager.sendData() failed. " + netError.toString());
                sentStatus = false;
            }

            cmd.dispose();
        }

        if (!sentStatus) {
            ARSALPrint.e(TAG, "Failed to send AllStates command.");
        } else {
            try {
                stateSemaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
                sentStatus = false;
            }
        }
        return sentStatus;
    }

    private boolean setTime() {
        boolean failed = true;

        ARCommand cmd = new ARCommand();
        String dateFormatted = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Log.d(TAG, dateFormatted);
        cmd.setCommonCommonCurrentDate(dateFormatted);
        failed = sendARCommand(cmd);
        cmd.dispose();

        cmd = new ARCommand();
        String formatted = new SimpleDateFormat("'T'HHmmssZZZ", Locale.getDefault()).format(new Date());
//            formatted = formatted.substring(0, 22) + ":" + formatted.substring(22);
        Log.d(TAG, formatted);
        cmd.setCommonCommonCurrentTime(formatted);
        failed = sendARCommand(cmd);
        cmd.dispose();
        return failed;
    }

    public void takeOff() {

    }

    public void land() {

    }

    public void ascend() {

    }

    public void descend() {

    }

    private boolean sendARCommand(ARCommand command) {
        ARNETWORK_ERROR_ENUM error_enum;
        error_enum = netManager.sendData(DeviceController.iobufferC2dNack, command, null, true);
        command.dispose();
        return error_enum == ARNETWORK_ERROR_ENUM.ARNETWORK_OK;
    }

    private void registerListeners() {
        ARCommand.setCommonSettingsStateAllSettingsChangedListener(this);
        ARCommand.setCommonCommonStateAllStatesChangedListener(this);
        ARCommand.setARDrone3MediaRecordStatePictureStateChangedListener(this);
        ARCommand.setCommonCommonStateBatteryStateChangedListener(this);
        ARCommand.setARDrone3PilotingStateSpeedChangedListener(this);
    }

    @Override
    public void onCommonCommonStateAllStatesChangedUpdate() {
        settingsSemaphore.release();
    }

    @Override
    public void onCommonSettingsStateAllSettingsChangedUpdate() {
        settingsSemaphore.release();
    }

    @Override
    public void onARDrone3PilotingStateFlyingStateChangedUpdate(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {
        Log.d(TAG, "Flying state changed");
    }
}
