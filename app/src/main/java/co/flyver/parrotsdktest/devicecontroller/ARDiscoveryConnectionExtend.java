package co.flyver.parrotsdktest.devicecontroller;

import android.util.Log;

import com.parrot.arsdk.ardiscovery.ARDISCOVERY_ERROR_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryConnection;

/**
 * Created by Petar Petrov on 3/12/15.
 */
public class ARDiscoveryConnectionExtend  extends ARDiscoveryConnection {
    public static final String TAG = "ARDiscoveryConnectionEx";

    public ARDiscoveryConnectionExtend() {
        super();
    }

    @Override
    protected String onSendJson() {
        Log.d(TAG, "JSON SENT!");
        return null;
    }

    @Override
    protected ARDISCOVERY_ERROR_ENUM onReceiveJson(String dataRx, String ip) {
        Log.e(TAG, "JSON RECEIVED ".concat(dataRx.concat(" from IP: ".concat(ip))));
        return ARDISCOVERY_ERROR_ENUM.ARDISCOVERY_OK;
    }
}
