package ch.gdgch.devfest.spacear.drone;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class SpaceNavigator {

    private static final String TAG = "SpaceNavigator";
    private static final byte TYPE_TRANSLATE = 1;
    private static final byte TYPE_ROTATE = 2;
    private static final byte TYPE_BUTTON = 3;

    private static final int USB_VENDOR_ID = 1133;

    private final UsbManager mUsbManager;
    private UsbInterface mInterface;
    private UsbEndpoint mEndpointIn;
    private UsbDeviceConnection mDeviceConnection;
    private WaiterThread mWaiterThread;
    private MouseListener mListener;
    private boolean mResumed;

    public void setListener(MouseListener listener) {
        mListener = listener;
    }

    public SpaceNavigator(Context context) {
        this.mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    public boolean openDevice(UsbDevice device) {
        closeDevice();

        int interfaceCount = device.getInterfaceCount();
        for (int i = 0; i < interfaceCount; i++) {
            UsbInterface intf = device.getInterface(i);
            int endpointCount = intf.getEndpointCount();
            for (int e = 0; e < endpointCount; e++) {
                UsbEndpoint endpoint = intf.getEndpoint(e);
                if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_INT
                        && endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                    mDeviceConnection = mUsbManager.openDevice(device);
                    if (mDeviceConnection.claimInterface(intf, true)) {
                        mInterface = intf;
                        mEndpointIn = endpoint;

                        if (mResumed) {
                            startThread();
                        }

                        if (mListener != null) {
                            mListener.onDeviceUpdate(true);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void closeDevice() {
        if (mDeviceConnection != null) {
            if (mResumed && mWaiterThread != null && mWaiterThread.isAlive()) {
                stopThread();
            }

            mDeviceConnection.releaseInterface(mInterface);
            mDeviceConnection.close();
            mDeviceConnection = null;
            mInterface = null;
            mEndpointIn = null;

            if (mListener != null) {
                mListener.onDeviceUpdate(false);
            }
        }
    }

    public UsbDevice findDevice() {
        for (UsbDevice device : mUsbManager.getDeviceList().values()) {
            if (SpaceNavigator.isMouse(device)) {
                return device;
            }
        }
        return null;
    }

    public void resume() {
        mResumed = true;
        updateThreadState();
    }

    public void pause() {
        mResumed = false;
        updateThreadState();
    }

    private void updateThreadState() {
        if (mResumed) {
            if (mDeviceConnection != null && (mWaiterThread == null || !mWaiterThread.isAlive())) {
                startThread();
            }
        } else {
            if (mWaiterThread != null && mWaiterThread.isAlive()) {
                stopThread();
            }
        }
    }

    private void stopThread() {
        mWaiterThread.interrupt();
        mWaiterThread = null;
    }

    private void startThread() {
        mWaiterThread = new WaiterThread();
        mWaiterThread.start();
    }

    private static int bytesToInt(byte lsb, byte msb) {
        int x = (((int) (msb) << 8) & 0xff00) | (((int) lsb) & 0x00ff);

        // Negative?
        if ((x & 0x8000) != 0)
            x |= 0xffff0000;

        return x;
    }

    public static boolean isMouse(UsbDevice device) {
        return device.getVendorId() == USB_VENDOR_ID;
    }

    private class WaiterThread extends Thread {

        public boolean mStop;

        public void run() {
            int translateX = 0, translateY = 0, translateZ = 0;
            int rotateX = 0, rotateY = 0, rotateZ = 0;
            boolean hasTranslate = false;
            boolean hasRotation = false;

            while (mResumed && !isInterrupted()) {
                byte[] bytes = new byte[7];
                int TIMEOUT = 0;
                int length = 7;

                mDeviceConnection.bulkTransfer(mEndpointIn, bytes, length, TIMEOUT);

                // Translation packet comes in before rotation packet. Wait
                // until you have both before
                // doing anything with the data
                switch (bytes[0]) {
                    case TYPE_TRANSLATE:
                        translateX = bytesToInt(bytes[1], bytes[2]);
                        translateY = bytesToInt(bytes[3], bytes[4]);
                        translateZ = bytesToInt(bytes[5], bytes[6]);
                        hasTranslate = true;
                        break;
                    case TYPE_ROTATE:
                        rotateX = bytesToInt(bytes[1], bytes[2]);
                        rotateY = bytesToInt(bytes[3], bytes[4]);
                        rotateZ = bytesToInt(bytes[5], bytes[6]);
                        hasRotation = true;
                        break;
                    case TYPE_BUTTON:
                        if (mListener != null) {
                            mListener.onButtonPress(bytes[1]);
                        }
                        break;
                    default:
                        Log.w(TAG, "Unknown packet type: " + bytes[0]);
                }

                if (hasTranslate && hasRotation) {
                    hasTranslate = hasRotation = false;
                    if (mListener != null) {
                        mListener.onUpdateMovement(translateX, translateY, translateZ, rotateX, rotateY, rotateZ);
                    }
                }
            }
        }
    }

    public interface MouseListener {

        void onUpdateMovement(int translateX, int translateY, int translateZ, int rotateX, int rotateY, int rotateZ);

        void onButtonPress(int buttons);

        void onDeviceUpdate(boolean connected);

    }

}
