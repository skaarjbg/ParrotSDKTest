package co.flyver.parrotsdktest.devicecontroller;

/**
 * Created by Petar Petrov on 3/6/15.
 */
abstract class LooperThread extends Thread {
    private boolean isAlive;
    private boolean isRunning;

    public LooperThread() {
        this.isRunning = false;
        this.isAlive = true;
    }

    @Override
    public void run() {
        this.isRunning = true;

        onStart();

        while (this.isAlive) {
            if(Thread.currentThread().isInterrupted()) {
                return;
            }
            onloop();
        }
        onStop();

        this.isRunning = false;
    }

    public void onStart() {

    }

    public abstract void onloop();

    public void onStop() {

    }

    public void stopThread() {
        isAlive = false;
    }

    public boolean isRunning() {
        return this.isRunning;
    }
}

