package co.flyver.parrotsdktest.devicecontroller;

import com.parrot.arsdk.arcommands.ARCOMMANDS_DECODER_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCommand;
import com.parrot.arsdk.arnetwork.ARNETWORK_ERROR_ENUM;
import com.parrot.arsdk.arnetwork.ARNetworkManager;
import com.parrot.arsdk.arsal.ARSALPrint;

/**
 * Created by Petar Petrov on 3/6/15.
 */
class ReaderThread extends LooperThread {
    private final String TAG = "ReaderThread";
    int bufferId;
    ARCommand dataRecv = new ARCommand(128 * 1024);//TODO define
    ARNetworkManager netManager;

    public ReaderThread(int bufferId, ARNetworkManager manager) {
        this.bufferId = bufferId;
        dataRecv = new ARCommand(128 * 1024);//TODO define
        netManager = manager;
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onloop() {
        boolean skip = false;
        ARNETWORK_ERROR_ENUM netError;

            /* read data*/
        netError = netManager.readDataWithTimeout(bufferId, dataRecv, 1000); //TODO define

        if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_OK) {
            if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_ERROR_BUFFER_EMPTY) {
//                    ARSALPrint.e (TAG, "ReaderThread readDataWithTimeout() failed. " + netError + " bufferId: " + bufferId);
            }

            skip = true;
        }

        if (skip == false) {
            ARCOMMANDS_DECODER_ERROR_ENUM decodeStatus = dataRecv.decode();
            if ((decodeStatus != ARCOMMANDS_DECODER_ERROR_ENUM.ARCOMMANDS_DECODER_OK) && (decodeStatus != ARCOMMANDS_DECODER_ERROR_ENUM.ARCOMMANDS_DECODER_ERROR_NO_CALLBACK) && (decodeStatus != ARCOMMANDS_DECODER_ERROR_ENUM.ARCOMMANDS_DECODER_ERROR_UNKNOWN_COMMAND)) {
                ARSALPrint.e(TAG, "ARCommand.decode() failed. " + decodeStatus);
            }
        }
    }

    @Override
    public void onStop() {
        dataRecv.dispose();
        super.onStop();
    }
}

