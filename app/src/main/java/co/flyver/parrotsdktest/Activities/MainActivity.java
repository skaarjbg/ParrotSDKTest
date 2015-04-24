package co.flyver.parrotsdktest.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcommands.ARCommand;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;
import com.parrot.arsdk.arnetwork.ARNETWORK_ERROR_ENUM;
import com.parrot.arsdk.arsal.ARNativeData;
import com.triggertrap.seekarc.SeekArc;

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import co.flyver.parrotsdktest.R;
import co.flyver.parrotsdktest.devicecontroller.DeviceController;
import co.flyver.parrotsdktest.devicecontroller.containers.PositionCommandContainer;
import co.flyver.parrotsdktest.devicecontroller.datatransfer.FTPConnection;
import co.flyver.parrotsdktest.videoprocessing.ImageExtractor;
import co.flyver.parrotsdktest.videoprocessing.VideoStreamReader;

public class MainActivity extends Activity implements ARDiscoveryServicesDevicesListUpdatedReceiverDelegate, DeviceController.Listener {

    private static final String TAG = "MainActivity";
    ImageExtractor imageExtractor;
    private ServiceConnection discoveryServiceConnection;
    private IBinder discoveryServiceBinder;
    private ARDiscoveryService discoveryService;
    private BroadcastReceiver ardiscoveryServicesDevicesListUpdatedReceiver;
    private DeviceController deviceController;
    private VideoStreamReader videoStreamReader;
    private boolean frameArrived = false;
    private FTPConnection ftpConnection;
    private boolean connected = false;
    private boolean readyForSelfie = false;
    private int seekArcProgress;
    AlertDialog dialog;
    ImageButton button;
    TextView speedXView;
    TextView speedYView;
    TextView speedZView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            int conn = savedInstanceState.getInt("connected");
            connected = conn == 1;
        }

        setContentView(R.layout.activity_main);
        startServices();
        initBroadcastReceiver();
        registerReceivers();
        addListeners();
        registerShutdownHook();
        registerUncaughtExceptionHook();
        speedXView = (TextView) findViewById(R.id.text_speedX);
        speedYView = (TextView) findViewById(R.id.text_speedY);
        speedZView = (TextView) findViewById(R.id.text_speedZ);

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("connected", connected ? 1 : 0);
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
        deviceController.setListener(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                deviceController.start();
            }
        }).start();
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
        try {
            startDeviceController(list.get(0));
            ftpConnection = new FTPConnection("192.168.42.1", 21, "anonymous", "");
            ImageButton button = (ImageButton) findViewById(R.id.imageButton);
            button.setBackgroundResource(R.drawable.disabled_btn);

            button.setBackgroundResource(R.drawable.selfie_btn);
            connected = true;
            readyForSelfie = true;

        } catch (IndexOutOfBoundsException e) {
//            deviceController.stop();
            System.exit(0);
        }
    }

    public void addListeners() {

        final ImageButton imageButton;
        imageButton = (ImageButton) findViewById(R.id.dummy_image_view);

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connected && readyForSelfie) {
                    Log.d(TAG, "Taking off");

                    ARCommand cmd;
                    cmd = new ARCommand();
//                    cmd.setARDrone3PilotingTakeOff();
//                    sendARCommand(cmd);
//                    cmd.dispose();
//
//                    new Timer().schedule(new TimerTask() {
//                        @Override
//                        public void run() {
//                            PositionCommandContainer commandContainer = new PositionCommandContainer();
//                            commandContainer.gaz = (byte) seekArcProgress;
//                            deviceController.setDronePosition(commandContainer);
//                            new Timer().schedule(new TimerTask() {
//                                @Override
//                                public void run() {
//                                    PositionCommandContainer commandContainer = new PositionCommandContainer();
//                                    commandContainer.gaz = (byte) 0;
//                                    deviceController.setDronePosition(commandContainer);
//                                }
//                            }, 1500);
//                        }
//                    }, 3000);

                    button = (ImageButton) findViewById(R.id.imageButton);
                    button.setBackgroundResource(R.drawable.disabled_btn);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ARCommand cmd = new ARCommand();
                            cmd.setARDrone3MediaRecordPicture((byte) 0);
                            sendARCommand(cmd);
                            Log.d(TAG, "Taking picture");
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            dialog = new AlertDialog.Builder(MainActivity.this).setMessage("Please wait while the picture is being transferred.").show();
                                        }
                                    });
                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            ARCommand cmd = new ARCommand();
                                            cmd.setARDrone3PilotingLanding();
                                            Log.d(TAG, "Landing");
                                            sendARCommand(cmd);
                                        }
                                    }, 4000);
                                }
                            }).start();
                        }
                    }, 3000);
                } else {
                    WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                    wifi.disconnect();
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                }
            }
        });

        com.triggertrap.seekarc.SeekArc seekArc = (SeekArc) findViewById(R.id.seekArc);
        seekArc.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
            @Override
            public void onProgressChanged(SeekArc seekArc, int progress, boolean fromUser) {
//                switch(progress % 25) {
//                    case 1: seekArcProgress = 25;
//                        break;
//                    case 2: seekArcProgress = 50;
//                        break;
//                    case 3: seekArcProgress = 75;
//                        break;
//                    case 4: seekArcProgress = 100;
//                }
                seekArcProgress = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekArc seekArc) {

            }

            @Override
            public void onStopTrackingTouch(SeekArc seekArc) {
            }
        });

        Button button;

        button = (Button) findViewById(R.id.button_takeoff);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ARCommand cmd = new ARCommand();

                cmd = new ARCommand();
                cmd.setARDrone3PilotingTakeOff();
                sendARCommand(cmd);
                cmd.dispose();
            }
        });

        button = (Button) findViewById(R.id.button_land);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ARCommand cmd = new ARCommand();
                cmd.setARDrone3PilotingLanding();
                sendARCommand(cmd);
                cmd.dispose();
            }
        });

        button = (Button) findViewById(R.id.button_emergency);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ARCommand command = new ARCommand();
                command.setARDrone3PilotingEmergency();
                sendARCommand(command);
            }
        });

        button = (Button) findViewById(R.id.button_ascend);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PositionCommandContainer commandContainer = new PositionCommandContainer();
                commandContainer.gaz = 10;
                deviceController.setDronePosition(commandContainer);
            }
        });

        button = (Button) findViewById(R.id.button_descend);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PositionCommandContainer commandContainer = new PositionCommandContainer();
                commandContainer.gaz = -10;
                deviceController.setDronePosition(commandContainer);
            }
        });

//        button = (Button) findViewById(R.id.button);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                ARCommand cmd = new ARCommand();
//                cmd.setARDrone3MediaRecordPicture((byte) 0);
//                sendARCommand(cmd);
//            }
//        });
//        button = (Button) findViewById(R.id.button2);
//        button.setOnClickListener(new View.OnClickListener() !failed{
//            @Override
//            public void onClick(View v) {
//                String[] files = ftpConnection.listFilesCurrentDirectory();
//                for (String file : files) {
//                    Log.d(TAG, file);
//                }
//            }
//        });
//        button = (Button) findViewById(R.id.button3);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                ftpConnection.getFileCurrentDirectory(ftpConnection.getLastFileNameCurrentDirectory());
//            }
//        });
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }


    @Override
    public void onDisconnect() {

    }

    @Override
    public void onUpdateBattery(byte percent) {
        Log.d(TAG, "Battery updated: " + percent);

    }

    @Override
    public void onFlyingStateChanged(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {

    }

    @Override
    public void pictureReady() {
        File picture = ftpConnection.getFileCurrentDirectory(ftpConnection.getLastFileNameCurrentDirectory());

        Intent imageActivityIntent = new Intent(MainActivity.this, ImageTakenActivity.class);
        Log.d(TAG, "Picture path: ".concat(picture.getName()));
        imageActivityIntent.putExtra("filepath", picture.getName());
        dialog.dismiss();
        startActivity(imageActivityIntent);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setBackgroundResource(R.drawable.selfie_btn);
            }
        });
    }

    @Override
    public void speedChanged(final float speedX, final float speedY, final float speedZ) {
//        Log.d(TAG, String.format("Speed: %f, %f, %f", speedX, speedY, speedZ));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                speedXView.setText(String.valueOf(speedX));
                speedYView.setText(String.valueOf(speedY));
                speedZView.setText(String.valueOf(speedZ));
            }
        });
    }

    private void registerShutdownHook() {
        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                ARCommand command = new ARCommand();
                command.setARDrone3PilotingEmergency();
                sendARCommand(command);
                command.dispose();
                System.exit(0);
            }
        }));
    }

    private void registerUncaughtExceptionHook() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                ARCommand command = new ARCommand();
                command.setARDrone3PilotingEmergency();
                sendARCommand(command);
                command.dispose();
            }
        });
    }
}
