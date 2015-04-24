package co.flyver.parrotsdktest.devicecontroller.datatransfer;

import android.os.Environment;
import android.util.Log;

import com.parrot.arsdk.ardatatransfer.ARDataTransferException;
import com.parrot.arsdk.ardatatransfer.ARDataTransferManager;
import com.parrot.arsdk.ardatatransfer.ARDataTransferMedia;
import com.parrot.arsdk.ardatatransfer.ARDataTransferMediasDownloader;
import com.parrot.arsdk.arutils.ARUtilsException;
import com.parrot.arsdk.arutils.ARUtilsManager;

/**
 * Created by Petar Petrov on 4/7/15.
 */
public class DataTransferManager {

    private final static String TAG = "DataTransferManager";
    private final static String IP = "192.168.42.1";
    private final static int PORT = 21;
    private final static String LOGIN = "anonymous";
    private final static String REMOTE_PATH = "internal_000";
    private final static String LOCAL_PATH = Environment.getExternalStorageDirectory().getPath().concat("co.flyver/droneselfie/pictures/");

    ARDataTransferManager manager;
    ARUtilsManager ftpQueueManager;
    ARUtilsManager ftpListManager;
    ARDataTransferMediasDownloader mediasDownloader;

    public DataTransferManager() {
        try {
            manager = new ARDataTransferManager();
            Log.d(TAG, "DataTransferManager created");
        } catch (ARDataTransferException e) {
            e.printStackTrace();
        }
    }

    public void init() {
        try {
            ftpQueueManager = new ARUtilsManager();
            ftpListManager = new ARUtilsManager();
        } catch (ARUtilsException e) {
            e.printStackTrace();
        }
        ftpQueueManager.initWifiFtp(IP, PORT, LOGIN, "");
        ftpListManager.initWifiFtp(IP, PORT, LOGIN, "");

        mediasDownloader = manager.getARDataTransferMediasDownloader();
        try {
            mediasDownloader.createMediasDownloader(ftpListManager, ftpQueueManager, REMOTE_PATH, LOCAL_PATH);
        } catch (ARDataTransferException e) {
            e.printStackTrace();
        }

    }

    public void getMediaList() {
        try {
            int list = mediasDownloader.getAvailableMediasSync(true);
            Log.d(TAG, String.valueOf(list));
        } catch (ARDataTransferException e) {
            e.printStackTrace();
        }
    }

    public void getMedia(int index) {
        try {
            ARDataTransferMedia media = mediasDownloader.getAvailableMediaAtIndex(index);
            Log.d(TAG, media.toString());
        } catch (ARDataTransferException e) {
            e.printStackTrace();
        }
    }
}
