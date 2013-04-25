package ch.gdgch.devfest.spacear.drone;

import android.os.Handler;
import android.os.HandlerThread;
import com.codeminders.ardrone.ARDrone;

import java.io.IOException;
import java.net.InetAddress;

public class DroneControl {

    public static final byte[] DEFAULT_DRONE_IP = {(byte) 192, (byte) 168, (byte) 1, (byte) 1};

    private static final int NAV_DATA_TIMEOUT = 10000;
    private static final int VIDEO_TIMEOUT = 60000;

    private final HandlerThread mThread;
    private final Handler mHandler;
    private final ARDrone mDrone;
    private DroneListener mListener;
    private DroneState mCurrentState = DroneState.DISCONNECTED;

    public DroneState getCurrentState() {
        return mCurrentState;
    }

    public void setListener(DroneListener listener) {
        mListener = listener;
        if (mListener != null) {
            mListener.onDroneState(mCurrentState);
        }
    }

    public DroneControl(InetAddress addr) {
        mThread = new HandlerThread("DroneControlThread");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mDrone = new ARDrone(addr, NAV_DATA_TIMEOUT, VIDEO_TIMEOUT);
    }

    public void connect() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    updateState(DroneState.CONNECTING);
                    mDrone.connect();
                    mDrone.clearEmergencySignal();
                    mDrone.waitForReady(NAV_DATA_TIMEOUT);
                    mDrone.playLED(1, 10, 4);
                    mDrone.setCombinedYawMode(false);
                    updateState(DroneState.CONNECTED);
                } catch (IOException e) {
                    updateState(DroneState.DISCONNECTED);
                }
            }
        });
    }

    public void disconnect() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mDrone.disconnect();
                    updateState(DroneState.DISCONNECTED);
                } catch (IOException e) {
                }
            }
        });
    }

    private void updateState(DroneState state) {
        mCurrentState = state;
        if (mListener != null) {
            mListener.onDroneState(state);
        }
    }

    public void takeoff() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mDrone.clearEmergencySignal();
                    mDrone.trim();
                    mDrone.takeOff();
                    updateState(DroneState.FLYING);
                } catch (IOException e) {
                }
            }
        });
    }

    public void land() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mDrone.land();
                    updateState(DroneState.LANDED);
                } catch (IOException e) {
                }
            }
        }) ;
    }

    public void move(final float roll, final float nick, final float alt, final float yaw) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mDrone.move(roll, nick, alt, yaw);
                } catch (IOException e) {
                }
            }
        });
    }

    public enum DroneState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        LANDED,
        FLYING
    }

    public interface DroneListener {

        void onDroneState(DroneState state);

    }

}
