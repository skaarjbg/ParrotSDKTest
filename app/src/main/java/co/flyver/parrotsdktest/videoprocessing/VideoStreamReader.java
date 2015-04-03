package co.flyver.parrotsdktest.videoprocessing;

import com.parrot.arsdk.arnetwork.ARNetworkManager;
import com.parrot.arsdk.arsal.ARNativeData;
import com.parrot.arsdk.arstream.ARSTREAM_READER_CAUSE_ENUM;
import com.parrot.arsdk.arstream.ARStreamReader;
import com.parrot.arsdk.arstream.ARStreamReaderListener;

import co.flyver.parrotsdktest.devicecontroller.config.ARDroneNetworkConfig;

/**
 * Created by Petar Petrov on 3/17/15.
 */
public class VideoStreamReader {

    public interface onFrameReceievedCallback {
        public boolean run(ARNativeData data);
    }

    private static final String TAG = VideoStreamReader.class.getSimpleName();
    private ARStreamReader reader;
    private ARStreamReaderListener listener;
    private ARNetworkManager manager;
    private ARDroneNetworkConfig netConfig;
    private onFrameReceievedCallback callback;
    private int videoFragmentSize;
    private int videoFragmentNumber;
    private ARNativeData data;

    private VideoStreamReader() {
    }

    public VideoStreamReader(ARNetworkManager manager, ARDroneNetworkConfig netConfig, int videoFragmentSize, int videoFragmentNumber) {
        this.manager = manager;
        this.netConfig = netConfig;
        this.videoFragmentNumber = videoFragmentNumber;
        this.videoFragmentSize = videoFragmentSize;
        data = new ARNativeData(128 * 1024);
    }

    public void init() {
        listener = new ARStreamReaderListener() {
            @Override
            public ARNativeData didUpdateFrameStatus(ARSTREAM_READER_CAUSE_ENUM cause,
                                                     ARNativeData currentFrame,
                                                     boolean isFlushFrame,
                                                     int nbSkippedFrames,
                                                     int newBufferCapacity) {
                callback.run(currentFrame);
                return currentFrame;
            }
        };
//        netConfig.addStreamReaderIOBuffer(videoFragmentSize, videoFragmentNumber);
        reader = new ARStreamReader(manager,
                netConfig.getVideoDataIOBuffer(),
                netConfig.getVideoAckIOBuffer(),
                data,
                listener,
                videoFragmentSize,
                netConfig.getDefaultVideoMaxAckInterval());

        new Thread(reader.getDataRunnable()).start();
        new Thread(reader.getAckRunnable()).start();
    }

    public void setCallback(onFrameReceievedCallback callback) {
        this.callback = callback;
    }
}
