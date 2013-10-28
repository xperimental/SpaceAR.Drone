package ch.gdgch.devfest.spacear.drone;

import android.hardware.usb.UsbDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

public interface DroneInput {

    void setListener(MouseListener listener);
    void resume();
    void pause();
    UsbDevice findDevice();
    boolean openDevice(UsbDevice device);
    void closeDevice();
    boolean handleGenericMotionEvent(MotionEvent event);
    boolean handleKeyEvent(KeyEvent event);

}
