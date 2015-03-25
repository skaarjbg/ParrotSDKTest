package co.flyver.parrotsdktest;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.VideoView;

import com.parrot.arsdk.arcommands.ARCommand;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;
import com.parrot.arsdk.arnetwork.ARNETWORK_ERROR_ENUM;
import com.parrot.arsdk.arsal.ARNativeData;
import com.parrot.arsdk.arsal.ARSALPrint;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import co.flyver.parrotsdktest.devicecontroller.DeviceController;
import co.flyver.parrotsdktest.devicecontroller.FrameDecoder;
import co.flyver.parrotsdktest.devicecontroller.PositionCommandContainer;
import co.flyver.parrotsdktest.devicecontroller.VideoStreamReader;

public class MainActivity extends ActionBarActivity implements ARDiscoveryServicesDevicesListUpdatedReceiverDelegate {

    private static final String TAG = "MainActivity";
    private ServiceConnection discoveryServiceConnection;
    private IBinder discoveryServiceBinder;
    private ARDiscoveryService discoveryService;
    private BroadcastReceiver ardiscoveryServicesDevicesListUpdatedReceiver;
    private DeviceController deviceController;
    private VideoStreamReader videoStreamReader;

    static {
        try {
            System.loadLibrary("arsal");
            System.loadLibrary("arsal_android");
            System.loadLibrary("arnetworkal");
            System.loadLibrary("arnetworkal_android");
            System.loadLibrary("arnetwork");
            System.loadLibrary("arnetwork_android");
            System.loadLibrary("arcommands");
            System.loadLibrary("arcommands_android");
            System.loadLibrary("ardiscovery");
            System.loadLibrary("ardiscovery_android");
            System.loadLibrary("arstream");
            System.loadLibrary("arstream_android");

            ARSALPrint.enableDebugPrints();

        } catch (Exception e) {
            Log.e(TAG, "Oops (LoadLibrary)", e);
        }
    }

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
        for(ARDiscoveryDeviceService service : list) {
            Log.d(TAG, service.getName());
        }
        if(list.get(0).getName().equals("BebopDrone-A036160")) {
            startDeviceController(list.get(0));
            startVideo();
        }
    }

    public void addListeners() {
        Button button;

        button = (Button) findViewById(R.id.button_start);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Start pressed!");
                PositionCommandContainer position = new PositionCommandContainer();
                position.flag = 1;
                position.gaz = 50;
                position.pitch = 50;
                position.roll = 50;
                position.yaw = 50;
                position.psi = 0;
                deviceController.setDronePosition(position);
            }
        });

        button = (Button) findViewById(R.id.button_stop);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Stop pressed!");
                PositionCommandContainer position = new PositionCommandContainer();
                position.flag = 0;
                position.gaz = 0;
                position.pitch = 0;
                position.roll = 0;
                position.yaw = 0;
                position.psi = 0;
                deviceController.setDronePosition(position);
            }
        });

        button = (Button) findViewById(R.id.button_takeoff);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "TAKEOFF PRESSED");
                ARCommand cmd = new ARCommand();
                cmd.setARDrone3PilotingTakeOff();
                deviceController.getNetManager().sendData(DeviceController.iobufferC2dNack, cmd, null, true);
                cmd.dispose();
            }
        });

        button = (Button) findViewById(R.id.button_land);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "LAND PRESSED");
                ARCommand cmd = new ARCommand();
                cmd.setARDrone3PilotingLanding();
                deviceController.getNetManager().sendData(DeviceController.iobufferC2dNack, cmd, null, true);
                cmd.dispose();
            }
        });
        button = (Button) findViewById(R.id.button_video);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ARNETWORK_ERROR_ENUM error_enum;
                ARCommand cmd = new ARCommand();
                cmd.setARDrone3MediaStreamingVideoEnable((byte) 1);
                error_enum = deviceController.getNetManager().sendData(DeviceController.iobufferC2dNack, cmd, null, true);
                Log.d(TAG, "Video Stream Requested: ".concat(error_enum.toString()));
                cmd.dispose();
            }
        });
        button = (Button) findViewById(R.id.button_picture);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ARNETWORK_ERROR_ENUM error_enum;
                ARCommand cmd = new ARCommand();
                cmd.setARDrone3MediaRecordPictureV2();
                error_enum = deviceController.getNetManager().sendData(DeviceController.iobufferC2dNack, cmd, null, true);
                Log.d(TAG, "Take picture: ".concat(error_enum.toString()));
                cmd.dispose();
            }
        });
    }

    private void startVideo() {
        final VideoView view = (VideoView) findViewById(R.id.videoView);
        view.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
            }
        });
        videoStreamReader = new VideoStreamReader(deviceController.getNetManager(),
                deviceController.getNetConfig(),
                deviceController.getVideoFragmentSize(),
                deviceController.getVideoFragmentNumber());

        videoStreamReader.setCallback(new VideoStreamReader.onFrameReceievedCallback() {
            @Override
            public boolean run(ARNativeData data) {
                FrameDecoder frameDecoder = new FrameDecoder();
                Log.d(TAG, data.toString());
                File fd = new File(Environment.getExternalStorageDirectory().getPath().concat("/file-".concat(String.valueOf(System.nanoTime()))));
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(fd);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    if (fos != null) {
                        ByteBuffer buffer = frameDecoder.decode(data.getByteData());
                        if(buffer != null) {
                            fos.write(buffer.array());
                        } else {
                            return false;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                view.setVideoPath(fd.getPath());
                view.start();
//                Log.d(TAG, Arrays.toString(data.getByteData()));
                return false;
            }
        });
        videoStreamReader.init();
    }
}
