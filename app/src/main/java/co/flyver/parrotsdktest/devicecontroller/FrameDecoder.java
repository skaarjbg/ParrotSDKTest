package co.flyver.parrotsdktest.devicecontroller;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import com.parrot.arsdk.arsal.ARNativeData;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by Petar Petrov on 3/25/15.
 */
public class FrameDecoder implements VideoStreamReader.onFrameReceievedCallback {

    private final static String TAG = FrameDecoder.class.getSimpleName();
    MediaCodec codec;
    MediaFormat format;
    ArrayList<ARNativeData> input;
    ArrayList<ARNativeData> output;

    @Override
    public boolean run(ARNativeData data) {

        return false;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public FrameDecoder() {
        try {
            codec = MediaCodec.createDecoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
        }

        format = MediaFormat.createVideoFormat("video/avc", 800, 600);

        format.setString(MediaFormat.KEY_MIME, "video/avc");
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100000);
        format.setInteger(MediaFormat.KEY_WIDTH, 800);
        format.setInteger(MediaFormat.KEY_HEIGHT, 600);
        format.setInteger(MediaFormat.KEY_MAX_WIDTH, 800);
        format.setInteger(MediaFormat.KEY_MAX_HEIGHT, 600);
        format.setInteger(MediaFormat.KEY_PUSH_BLANK_BUFFERS_ON_STOP, 1);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 100);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 25);

        codec.configure(format, null, null, 0);
        codec.start();
    }

    public void loop(byte[] data) {
        int freeBuffer = codec.dequeueInputBuffer(10000);
        ByteBuffer inputBuffers[] = codec.getInputBuffers();
        ByteBuffer outputBuffers[] = codec.getOutputBuffers();
        Log.e(TAG, "freeBuffer: " + String.valueOf(freeBuffer));
        if (freeBuffer >= 0) {
            ByteBuffer buffer = inputBuffers[freeBuffer];
            buffer.clear();
            buffer.put(data);
            codec.queueInputBuffer(freeBuffer, 0, 128, 33, 0);
        }

        int outputBufferIndex = codec.dequeueOutputBuffer(new MediaCodec.BufferInfo(), 10000);
        if (outputBufferIndex >= 0) {
            ByteBuffer output = codec.getOutputBuffer(outputBufferIndex);
            codec.releaseOutputBuffer(outputBufferIndex, false);
        } else if(outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            outputBuffers = codec.getOutputBuffers();
        } else if(outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            format = codec.getOutputFormat();
        }
    }
}
