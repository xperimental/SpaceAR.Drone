package ch.gdgch.devfest.spacear.drone;

public interface MouseListener {

    void onUpdateMovement(int translateX, int translateY, int translateZ, int rotateX, int rotateY, int rotateZ);

    void onButtonPress(int buttons);

    void onDeviceUpdate(boolean connected);

}
