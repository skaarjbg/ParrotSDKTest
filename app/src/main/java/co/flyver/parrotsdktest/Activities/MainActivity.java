package co.flyver.parrotsdktest.Activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.parrot.arsdk.arcommands.ARCommand;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;
import com.parrot.arsdk.arnetwork.ARNETWORK_ERROR_ENUM;
import com.parrot.arsdk.arsal.ARNativeData;

import java.io.File;
import java.util.List;

import co.flyver.parrotsdktest.R;
import co.flyver.parrotsdktest.devicecontroller.DeviceController;
import co.flyver.parrotsdktest.videoprocessing.ImageExtractor;
import co.flyver.parrotsdktest.videoprocessing.VideoStreamReader;

public class MainActivity extends Activity implements ARDiscoveryServicesDevicesListUpdatedReceiverDelegate {

    private static final String TAG = "MainActivity";
    ImageExtractor imageExtractor;
    private ServiceConnection discoveryServiceConnection;
    private IBinder discoveryServiceBinder;
    private ARDiscoveryService discoveryService;
    private BroadcastReceiver ardiscoveryServicesDevicesListUpdatedReceiver;
    private DeviceController deviceController;
    private VideoStreamReader videoStreamReader;
    private boolean frameArrived = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startServices();
        initBroadcastReceiver();
        registerReceivers();
        addListeners();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startServices() {

        discoveryServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                discoveryServiceBinder = service;
                discoveryService = ((ARDiscoveryService.LocalBinder) service).getService();
                discoveryService.start();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        Intent intent = new Intent(getApplicationContext(), ARDiscoveryService.class);
        startService(intent);
        getApplicationContext().bindService(intent, discoveryServiceConnection, BIND_AUTO_CREATE);
    }

    private void startDeviceController(ARDiscoveryDeviceService service) {
        deviceController = new DeviceController(getApplicationContext(), service);
        deviceController.start();
    }

    private void registerReceivers() {
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.registerReceiver(ardiscoveryServicesDevicesListUpdatedReceiver, new IntentFilter(ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated));

    }

    private void initBroadcastReceiver() {
        ardiscoveryServicesDevicesListUpdatedReceiver = new ARDiscoveryServicesDevicesListUpdatedReceiver(this);
    }

    @Override
    public void onServicesDevicesListUpdated() {
        List<ARDiscoveryDeviceService> list;
        list = discoveryService.getDeviceServicesArray();
        for (ARDiscoveryDeviceService service : list) {
            Log.d(TAG, service.getName());
        }
        if (list.get(0).getName().equals("BebopDrone-A036160")) {
            startDeviceController(list.get(0));
            startVideoStream();
        }
    }

    public void addListeners() {
        ImageButton imageButton;
        imageButton = (ImageButton) findViewById(R.id.imageButton);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ARCommand cmd = new ARCommand();
                cmd.setARDrone3PilotingTakeOff();
                sendARCommand(cmd);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        imageExtractor.decodeFrame();
                    }
                }, 10000);
            }
        });
    }

    private void startVideoStream() {
        imageExtractor = new ImageExtractor(getApplicationContext());
        imageExtractor.setCallback(new ImageExtractor.onImageReady() {
            @Override
            public void imageReady(final File file) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        ARCommand command = new ARCommand();
                        command.setARDrone3PilotingLanding();
                        sendARCommand(command);
                        Intent imageActivityIntent = new Intent(MainActivity.this, ImageTakenActivity.class);
                        imageActivityIntent.putExtra("filepath", file.getAbsolutePath());
                        startActivity(imageActivityIntent);
                    }
                });
            }
        });

        videoStreamReader = new VideoStreamReader(deviceController.getNetManager(),
                deviceController.getNetConfig(),
                deviceController.getVideoFragmentSize(),
                deviceController.getVideoFragmentNumber());

        videoStreamReader.setCallback(new VideoStreamReader.onFrameReceievedCallback() {
            @Override
            public boolean run(ARNativeData data) {
//                Log.d("FRAME: ", String.valueOf(data.getDataSize()));
                if (data.getDataSize() > 30000) {
//                    Log.d("FRAME", "Frame changed");
                    frameArrived = true;
                    imageExtractor.setCurrentFrame(data.getByteData());
//                    imageExtractor.decodeFrame();
                }
                return true;
            }
        });
        videoStreamReader.init();
    }

    private boolean sendARCommand(ARCommand command) {
        ARNETWORK_ERROR_ENUM error_enum;
        error_enum = deviceController.getNetManager().sendData(DeviceController.iobufferC2dNack, command, null, true);
        command.dispose();
        return error_enum == ARNETWORK_ERROR_ENUM.ARNETWORK_OK;
    }
}
