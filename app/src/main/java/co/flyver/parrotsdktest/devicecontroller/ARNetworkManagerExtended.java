package co.flyver.parrotsdktest.devicecontroller;

import android.util.Log;

import com.parrot.arsdk.arnetwork.ARNETWORK_MANAGER_CALLBACK_RETURN_ENUM;
import com.parrot.arsdk.arnetwork.ARNETWORK_MANAGER_CALLBACK_STATUS_ENUM;
import com.parrot.arsdk.arnetwork.ARNetworkIOBufferParam;
import com.parrot.arsdk.arnetwork.ARNetworkManager;
import com.parrot.arsdk.arnetworkal.ARNetworkALManager;
import com.parrot.arsdk.arsal.ARNativeData;

/**
 * Created by Petar Petrov on 3/6/15.
 */
class ARNetworkManagerExtended extends ARNetworkManager {
    public ARNetworkManagerExtended(ARNetworkALManager osSpecificManager, ARNetworkIOBufferParam[] inputParamArray, ARNetworkIOBufferParam[] outputParamArray, int timeBetweenPingsMs) {
        super(osSpecificManager, inputParamArray, outputParamArray, timeBetweenPingsMs);
    }

    private static final String TAG = "ARNetworkManagerExtend";

    @Override
    public ARNETWORK_MANAGER_CALLBACK_RETURN_ENUM onCallback(int ioBufferId, ARNativeData data, ARNETWORK_MANAGER_CALLBACK_STATUS_ENUM status, Object customData) {
        ARNETWORK_MANAGER_CALLBACK_RETURN_ENUM retVal = ARNETWORK_MANAGER_CALLBACK_RETURN_ENUM.ARNETWORK_MANAGER_CALLBACK_RETURN_DEFAULT;

        if (status == ARNETWORK_MANAGER_CALLBACK_STATUS_ENUM.ARNETWORK_MANAGER_CALLBACK_STATUS_TIMEOUT) {
            retVal = ARNETWORK_MANAGER_CALLBACK_RETURN_ENUM.ARNETWORK_MANAGER_CALLBACK_RETURN_DATA_POP;
        }

        return retVal;
    }

    @Override
    public void onDisconnect(ARNetworkALManager arNetworkALManager) {
        Log.d(TAG, "onDisconnect ...");
    }
}

