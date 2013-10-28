package ch.gdgch.devfest.spacear.drone;

import android.content.Context;
import android.hardware.input.InputManager;
import android.hardware.usb.UsbDevice;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class GameControllerInput implements DroneInput, InputManager.InputDeviceListener {

    private static final int BUTTON_TAKEOFF = KeyEvent.KEYCODE_BUTTON_A;
    private static final int BUTTON_LAND = KeyEvent.KEYCODE_BUTTON_B;
    private static final int UPDATE_TIMER = 30;
    private static final int TRANSLATE_THRESHOLD = 50;
    private static final int ROTATE_THRESHOLD = 50;

    private final InputManager inputManager;
    private MouseListener listener;
    private boolean connected;
    private int deviceId;
    private int buttons;
    private Thread generatorThread;
    private int[] translation = new int[3];
    private int[] rotation = new int[3];

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
        resetState();

        if (listener != null) {
            listener.onDeviceUpdate(connected);
            listener.onButtonPress(buttons);
        }
    }

    private void resetState() {
        buttons = 0;
        for (int i = 0; i < 3; i++) {
            translation[i] = 0;
            rotation[i] = 0;
        }
    }

    public GameControllerInput(Context context) {
        this.inputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
    }

    @Override
    public void setListener(MouseListener listener) {
        this.listener = listener;
    }

    @Override
    public void resume() {
        inputManager.registerInputDeviceListener(this, null);

        stopThread();
        generatorThread = new GeneratorThread();
        generatorThread.start();
    }

    @Override
    public void pause() {
        inputManager.unregisterInputDeviceListener(this);

        stopThread();
    }

    private void stopThread() {
        if (generatorThread != null) {
            generatorThread.interrupt();
            generatorThread = null;
        }
    }

    @Override
    public UsbDevice findDevice() {
        return null;
    }

    @Override
    public boolean openDevice(UsbDevice device) {
        return false;
    }

    @Override
    public void closeDevice() {

    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        this.deviceId = deviceId;
        setConnected(true);
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        this.deviceId = -1;
        setConnected(false);
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {

    }

    @Override
    public boolean handleGenericMotionEvent(MotionEvent event) {
        if (isConnected() && event.isFromSource(InputDevice.SOURCE_CLASS_JOYSTICK)) {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                translation[0] = (int) (event.getAxisValue(MotionEvent.AXIS_X) * 400);
                translation[1] = (int) (event.getAxisValue(MotionEvent.AXIS_Y) * 400);
                float alt = event.getAxisValue(MotionEvent.AXIS_GAS) - event.getAxisValue(MotionEvent.AXIS_BRAKE);
                translation[2] = (int) (-alt * 400);
                rotation[2] = (int) (event.getAxisValue(MotionEvent.AXIS_Z) * 400);
                clampValues();
                return true;
            }
        }
        return false;
    }

    private void clampValues() {
        for (int i = 0; i < 3; i++) {
            if (Math.abs(translation[i]) < TRANSLATE_THRESHOLD) {
                translation[i] = 0;
            }
            if (Math.abs(rotation[i]) < ROTATE_THRESHOLD) {
                rotation[i] = 0;
            }
        }
    }

    @Override
    public boolean handleKeyEvent(KeyEvent event) {
        if (isConnected() && event.getDeviceId() == deviceId) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (event.getKeyCode()) {
                    case BUTTON_TAKEOFF:
                        updateButton(1, true);
                        break;
                    case BUTTON_LAND:
                        updateButton(2, true);
                        break;
                }
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                switch (event.getKeyCode()) {
                    case BUTTON_TAKEOFF:
                        updateButton(1, false);
                        break;
                    case BUTTON_LAND:
                        updateButton(2, false);
                        break;
                }
            }
            return true;
        }
        return false;
    }

    private void updateButton(int index, boolean pressed) {
        if (pressed) {
            buttons |= index;
        } else {
            buttons &= ~index;
        }
        fireButton(buttons);
    }

    private void fireButton(int state) {
        if (listener != null) {
            listener.onButtonPress(state);
        }
    }

    private class GeneratorThread extends Thread {

        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    if (listener != null) {
                        listener.onUpdateMovement(translation[0], translation[1], translation[2], rotation[0], rotation[1], rotation[2]);
                    }
                    Thread.sleep(UPDATE_TIMER);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
