package co.flyver.parrotsdktest.devicecontroller;

import android.os.SystemClock;
import android.util.Log;

import com.parrot.arsdk.arcommands.ARCOMMANDS_GENERATOR_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCommand;
import com.parrot.arsdk.arnetwork.ARNETWORK_ERROR_ENUM;
import com.parrot.arsdk.arnetwork.ARNetworkManager;
import com.parrot.arsdk.arsal.ARSALPrint;

import java.util.Date;

import co.flyver.parrotsdktest.devicecontroller.containers.PositionCommandContainer;

/**
 * Created by Petar Petrov on 3/6/15.
 */
class ControllerThread extends LooperThread {
    private static final String TAG = "ControllerThread";
    ARNetworkManager netManager;
    DeviceController deviceControllerRef;

    public ControllerThread(DeviceController deviceController, ARNetworkManager netManager) {
        this.deviceControllerRef = deviceController;
        this.netManager = netManager;
    }

    @Override
    public void onloop() {
        long lastTime = SystemClock.elapsedRealtime();

//        sendPing();
        sendPCMD();

//        long sleepTime = (SystemClock.elapsedRealtime() + 50) - lastTime;
        long sleepTime = 25;

        try {
            sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean sendPCMD() {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();
        PositionCommandContainer dataPCMD = deviceControllerRef.getDronePosition();

        cmdError = cmd.setARDrone3PilotingPCMD(dataPCMD.flag, dataPCMD.roll, dataPCMD.pitch, dataPCMD.yaw, dataPCMD.gaz, dataPCMD.psi);
//        Log.d(TAG, "cmd sent: ".concat(cmdError.toString()));
        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK) {
            /* Send data with ARNetwork */
            // The commands sent in loop should be sent to a buffer not acknowledged ; here iobufferC2dNack
            ARNETWORK_ERROR_ENUM netError = netManager.sendData(DeviceController.iobufferC2dNack, cmd, null, true);

            if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_OK) {
                ARSALPrint.e(TAG, "netManager.sendData() failed. " + netError.toString());
                sentStatus = false;
            }

            cmd.dispose();
        }

        if (!sentStatus) {
            ARSALPrint.e(TAG, "Failed to send PCMD command.");
        }

        return sentStatus;
    }

    private void sendPing() {
        ARCommand cmd = new ARCommand();
        cmd.setCommonCommonCurrentDate(new Date().toString());
        ARNETWORK_ERROR_ENUM netError = netManager.sendData(DeviceController.iobufferC2dNack, cmd, null, true);
        Log.d(TAG, "Trying to send ping: ".concat(netError.toString()));
        cmd.dispose();
    }
}
