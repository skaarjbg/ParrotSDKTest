package co.flyver.parrotsdktest;

import android.app.Application;
import android.util.Log;

import com.parrot.arsdk.arsal.ARSALPrint;

/**
 * Created by Petar Petrov on 4/3/15.
 */
public class Entry extends Application {
    private static final String TAG = "Entry";

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
    public Entry() {
        super();
    }

    @Override
    public void onCreate() {
    }
}
