package co.flyver.parrotsdktest.devicecontroller;

import android.util.Log;

import com.parrot.arsdk.arnetwork.ARNetworkManager;
import com.parrot.arsdk.arsal.ARNativeData;
import com.parrot.arsdk.arstream.ARSTREAM_READER_CAUSE_ENUM;
import com.parrot.arsdk.arstream.ARStreamReader;
import com.parrot.arsdk.arstream.ARStreamReaderListener;

/**
 * Created by Petar Petrov on 3/17/15.
 */
public class VideoThread extends LooperThread {
    private static final String TAG = VideoThread.class.getSimpleName();
    private ARStreamReader reader;
    private ARStreamReaderListener listener;
    private ARNetworkManager manager;
    private ARDroneNetworkConfig netConfig;

    private VideoThread() {
    }

    public VideoThread(ARNetworkManager manager, ARDroneNetworkConfig netConfig, int videoFragmentSize, int videoFragmentNumber) {
        this.manager = manager;
        this.netConfig = netConfig;
        ARNativeData data = new ARNativeData(1024);
        listener = new ARStreamReaderListener() {
            @Override
            public ARNativeData didUpdateFrameStatus(ARSTREAM_READER_CAUSE_ENUM cause,
                                                     ARNativeData currentFrame,
                                                     boolean isFlushFrame,
                                                     int nbSkippedFrames,
                                                     int newBufferCapacity) {
                Log.d(TAG, currentFrame.toString());
                return currentFrame;
            }
        };
        netConfig.addStreamReaderIOBuffer(videoFragmentSize, videoFragmentNumber);
        reader = new ARStreamReader(manager,
                netConfig.getVideoDataIOBuffer(),
                netConfig.getVideoAckIOBuffer(),
                data,
                listener,
                videoFragmentSize,
                netConfig.getDefaultVideoMaxAckInterval());

        new Thread(reader.getDataRunnable()).run();
        new Thread(reader.getAckRunnable()).run();
//        reader.getDataRunnable().run();
//        reader.getAckRunnable().run();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onloop() {
    }
}
