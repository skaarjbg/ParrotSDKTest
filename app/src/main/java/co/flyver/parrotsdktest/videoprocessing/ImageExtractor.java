package co.flyver.parrotsdktest.videoprocessing;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import co.flyver.parrotsdktest.FFMPEGWrapper.FFMPEGWrapper;

/**
 * Created by Petar Petrov on 3/30/15.
 */
public class ImageExtractor {

    public FFMPEGWrapper wrapper;
    byte[] data;
    Context context;
    onImageReady callback;

    public ImageExtractor(Context context) {
        File file = new File(Environment.getExternalStorageDirectory().getPath().concat("/co.flyver/droneselfie/pictures"));
        file.mkdirs();
        wrapper = new FFMPEGWrapper(context, new FFMPEGWrapper.onReadyCallback<File>() {
            @Override
            public void ready(File file) {
                callback.imageReady(file);
            }
        });
    }

    public void setCallback(onImageReady callback) {
        this.callback = callback;
    }

    public void setCurrentFrame(byte[] data) {
        this.data = new byte[data.length];
        this.data = data;
    }

    public void decodeFrame() {
        File file = new File(Environment.getExternalStorageDirectory().getPath().concat("/co.flyver/droneselfie/pictures/file-".concat(String.valueOf(System.nanoTime()))));
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            if (fos != null) {
                fos.write(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        wrapper.extractImage(file);
    }

    public interface onImageReady {
        public void imageReady(File file);
    }
}
