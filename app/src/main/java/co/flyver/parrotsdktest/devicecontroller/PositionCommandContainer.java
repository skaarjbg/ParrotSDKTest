package co.flyver.parrotsdktest.devicecontroller;

/**
 * Created by Petar Petrov on 3/6/15.
 */
public class PositionCommandContainer {

    public byte flag;
    public byte roll;
    public byte pitch;
    public byte yaw;
    public byte gaz;
    public float psi;

    public PositionCommandContainer() {
        flag = 0;
        roll = 0;
        pitch = 0;
        yaw = 0;
        gaz = 0;
        psi = 0.0f;
    }
}
