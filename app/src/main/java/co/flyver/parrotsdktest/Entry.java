package co.flyver.parrotsdktest;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import com.parrot.arsdk.arsal.ARSALPrint;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

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
            System.loadLibrary("ardatatransfer");
            System.loadLibrary("ardatatransfer_android");
            System.loadLibrary("arutils");
            System.loadLibrary("arutils_android");
            System.loadLibrary("armedia");
            System.loadLibrary("armedia_android");

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
//        String path = Environment.getExternalStorageDirectory().getPath().concat("/co.flyver/droneselfie/log/");
//        File dir = new File(path);
//        dir.mkdirs();
//        String name = "Log-".concat(String.valueOf(System.currentTimeMillis()));
//        File file = new File(path.concat(name));
//        try {
//            file.createNewFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        String logcatCmd = "logcat co.flyver.parrotsdktest:v *:S -f ".concat(file.getAbsolutePath());
//        try {
//            Runtime.getRuntime().exec(logcatCmd);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
