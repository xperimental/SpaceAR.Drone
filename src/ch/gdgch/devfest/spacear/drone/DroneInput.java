package ch.gdgch.devfest.spacear.drone;

import android.hardware.usb.UsbDevice;

public interface DroneInput {

    void setListener(MouseListener listener);
    void resume();
    void pause();
    UsbDevice findDevice();
    boolean openDevice(UsbDevice device);
    void closeDevice();

}
