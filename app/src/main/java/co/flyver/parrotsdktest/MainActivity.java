package co.flyver.parrotsdktest;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;

import java.util.List;

import co.flyver.parrotsdktest.devicecontroller.DeviceController;

public class MainActivity extends ActionBarActivity {

    private ServiceConnection discoveryServiceConnection;
    private IBinder discoveryServiceBinder;
    private ARDiscoveryService discoveryService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startServices();
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
                startDeviceController();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        Intent intent = new Intent(getApplicationContext(), ARDiscoveryService.class);
        startService(intent);
        getApplicationContext().bindService(intent, discoveryServiceConnection, BIND_AUTO_CREATE);
    }

    private void startDeviceController() {
        List<ARDiscoveryDeviceService> list = discoveryService.getDeviceServicesArray();
        DeviceController deviceController = new DeviceController(getApplicationContext(), list.get(0));
        deviceController.start();
    }
}
