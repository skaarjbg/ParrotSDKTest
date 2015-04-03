package co.flyver.parrotsdktest.FFMPEGWrapper;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Petar Petrov on 3/27/15.
 */
public class FFMPEGWrapper {

    private static final String TAG = FFMPEGManager.class.getSimpleName();
    String ffmpegpath;
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Context context;
    private onReadyCallback callback;
    private String destPath = Environment.getExternalStorageDirectory().getPath().concat("/co.flyver/droneselfie/pictures/");

    public FFMPEGWrapper(Context context, onReadyCallback callback) {
        this.callback = callback;
        this.context = context;
        ffmpegpath = context.getFilesDir().getPath().concat("/ffmpeg");
    }

    public void convertToMPEG4(File file) {
        final String filename = file.getAbsolutePath();
        final String cmd = ffmpegpath + " -i " + filename + " -vcodec copy -acodec mp2 " + filename + ".mp4";
        Log.d(TAG, cmd);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Process ffmpegProcess = Runtime.getRuntime().exec(cmd);
                    ffmpegProcess.waitFor();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                //noinspection unchecked
                callback.ready(new File(filename.concat(".mp4")));
            }
        });
    }

    public void extractImage(final File file) {
//        ffmpeg -i file-108105180269592.mp4 -r 1  -b:a 50000 -vcodec mjpeg  -s svga -q:v 1 -f image2 pic-%2d.jpg
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                final String filename = file.getAbsolutePath();
                final String cmd = ffmpegpath + " -i " + filename + " -r 1 -s wuxga -qscale:v 3 -f image2 " + destPath.concat(file.getName()) + ".jpeg";
                try {
                    Log.d(TAG, cmd);
                    Process ffmpegProcess = Runtime.getRuntime().exec(cmd);
                    Log.d(TAG, String.valueOf(ffmpegProcess.getInputStream().read()));
                    class StreamReader extends Thread {
                        InputStream is;

                        StreamReader(InputStream is) {
                            this.is = is;
                        }
                        public void run() {
                            InputStreamReader inputStreamReader = new InputStreamReader(is);
                            BufferedReader br = new BufferedReader(inputStreamReader);
                            String line;
                            try {
                                while((line = br.readLine())!= null) {
                                    Log.d(TAG, line);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    StreamReader streamReader = new StreamReader(ffmpegProcess.getInputStream());
                    streamReader.start();
                    StreamReader errorStreamReader = new StreamReader(ffmpegProcess.getErrorStream());
                    errorStreamReader.start();

                    ffmpegProcess.waitFor();
                    File forDelete = new File(filename);
                    forDelete.delete();
                    Log.d(TAG, filename.concat(".jpeg"));
                    File f1 = new File(filename.concat(".jpeg"));
//                    Bitmap bitmap = BitmapFactory.decodeFile(f1.getAbsolutePath());
                    //noinspection unchecked
                    callback.ready(f1);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public interface onReadyCallback<T> {
        public void ready(T t);
    }
}
