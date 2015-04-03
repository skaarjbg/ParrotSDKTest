package co.flyver.parrotsdktest.FFMPEGWrapper;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Petar Petrov on 3/27/15.
 */
public class FFMPEGManager {

    public static final String TAG = "FFMPEGChecker";
    public static final String PATH = "/co.flyver/ffmpeg/";
    public static final String VIDEOSPATH = "/co.flyver/video/";
    static String envpath = Environment.getDataDirectory().toString().concat("/data/");
    static Context context;

    public static void init(Context context) {
        FFMPEGManager.context = context;
        File ffmpeg = new File(envpath.concat(PATH));
        if (!ffmpeg.exists()) {
            copyFiles(context);
        }
        File rtpvideopath = new File(envpath.concat(VIDEOSPATH));
        if(!rtpvideopath.exists()) {
            rtpvideopath.mkdirs();
        }
    }

    private static void copyFiles(Context context) {
        String destPath = context.getFilesDir().getPath().concat("/");
        File path = new File(destPath);
        path.mkdirs();
        String[] files = null;
        AssetManager manager = context.getResources().getAssets();
        try {
            files = manager.list("ffmpeg");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (files != null) {
            for (String file : files) {
                InputStream inputStream;
                OutputStream outputStream;

                try {
                    inputStream = manager.open("ffmpeg/".concat(file));
                    outputStream = new FileOutputStream(destPath.concat(file));
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, read);
                    }
                    inputStream.close();
                    outputStream.flush();
                    outputStream.close();
                    Log.d(TAG, destPath.concat(file));
                    File file1 = new File(destPath.concat(file));
                    file1.setExecutable(true);
                    file1.setReadable(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
