package ch.gdgch.devfest.spacear.drone;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.widget.Toast;
import com.codeminders.ardrone.ARDrone;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

public class SpaceMouseControlActivity extends Activity implements SpaceNavigator.MouseListener, DroneControl.DroneListener {

    private static final String ACTION_USB_PERMISSION = "ch.gdgch.devfest.spacear.drone.USB_PERMISSION";

	private final static int AXIS_MAX=400;
    private SpaceNavigator mMouse;
    private DroneControl mDrone;
    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private IntentFilter mIntentFilter;
    private TextView conn_tv,nick_tv,roll_tv,yaw_tv,alt_tv,btns_tv,drone_state_tv;
    private ProgressBar nick_pb,roll_pb,yaw_pb,alt_pb;

    private CheckBox use_rotation_axis_cb;

	private SeekBar sensitivity_seek;
    public long[] translate = new long[3];
    public long[] rotation = new long[3];
    public byte btns;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_space_mouse_control);
        mMouse = new SpaceNavigator(this);
        mMouse.setListener(this);
        InetAddress addr = null;
        try {
            addr = InetAddress.getByAddress(DroneControl.DEFAULT_DRONE_IP);
            mDrone = new DroneControl(addr);
            mDrone.setListener(this);
        } catch (UnknownHostException e) {
            Toast.makeText(this, "Can not resolve drone IP.", Toast.LENGTH_SHORT).show();
        }
        mUsbManager = (UsbManager) getSystemService(USB_SERVICE);

		conn_tv=(TextView)findViewById(R.id.connection);
		
		roll_tv=(TextView)findViewById(R.id.roll);
		nick_tv=(TextView)findViewById(R.id.nick);
		yaw_tv=(TextView)findViewById(R.id.yaw);
		alt_tv=(TextView)findViewById(R.id.alt);
		btns_tv=(TextView)findViewById(R.id.btns);
		
		use_rotation_axis_cb=(CheckBox)findViewById(R.id.use_rotation_axis_cb);
		sensitivity_seek=(SeekBar)findViewById(R.id.sensitivity_seek);
		
		sensitivity_seek.setMax(100);
		sensitivity_seek.setProgress(70);
		
		roll_pb=(ProgressBar)findViewById(R.id.roll_progress);
		nick_pb=(ProgressBar)findViewById(R.id.nick_progress);
		alt_pb=(ProgressBar)findViewById(R.id.alt_progress);
		yaw_pb=(ProgressBar)findViewById(R.id.yaw_progress);
		
		roll_pb.setMax(2*AXIS_MAX);
		nick_pb.setMax(2*AXIS_MAX);
		
		yaw_pb.setMax(2*AXIS_MAX);
		alt_pb.setMax(2*AXIS_MAX);
		
		drone_state_tv=(TextView)findViewById(R.id.drone_state);
		
		mHandler.post(new UpdateRunnable());
		
		conn_tv.setText("false");

        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

		// listen for new devices
		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		mIntentFilter.addAction(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, mIntentFilter);

		// check for existing devices
        checkForDevice();
	}

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        checkForDevice();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mMouse.resume();
        mDrone.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mDrone.disconnect();
        mMouse.pause();
    }

    @Override
    public void onUpdateMovement(int translateX, int translateY, int translateZ, int rotateX, int rotateY, int rotateZ) {
        translate[0] = translateX;
        translate[1] = translateY;
        translate[2] = translateZ;
        rotation[0] = rotateX;
        rotation[1] = rotateY;
        rotation[2] = rotateZ;
        mHandler.post(new UpdateRunnable());
    }

    @Override
    public void onButtonPress(int buttons) {
        btns = (byte) buttons;

        mHandler.post(new UpdateRunnable());
    }

    @Override
    public void onDeviceUpdate(boolean connected) {
        conn_tv.setText(connected ? "true" : "false");
    }

    @Override
    public void onDroneState(DroneControl.DroneState state) {
        mHandler.post(new UpdateRunnable());
    }

    private void checkForDevice() {
        UsbDevice device = mMouse.findDevice();
        if (device == null) {
            Toast.makeText(this, "No control device found!", Toast.LENGTH_SHORT).show();
        } else {
            mUsbManager.requestPermission(device, mPermissionIntent);
        }
    }



    Handler mHandler=new Handler();
	
	class UpdateRunnable implements Runnable {

		@Override
		public void run() {
			
			int act_nick, act_roll,act_alt,act_yaw;
			
			if (!use_rotation_axis_cb.isChecked()) {
				act_nick=(int)translate[1];
				act_roll=(int)translate[0];
			} else {
				act_nick=(int)rotation[0];
				act_roll=(int)-rotation[1];
				
			}
			act_alt=(int)translate[2];
			act_yaw=(int)rotation[2];
			
			nick_pb.setProgress((int)act_nick+AXIS_MAX);
			roll_pb.setProgress((int)act_roll+AXIS_MAX);
			alt_pb.setProgress((int)act_alt+AXIS_MAX);
			yaw_pb.setProgress((int)act_yaw+AXIS_MAX);

			nick_tv.setText("" + act_nick);
			roll_tv.setText("" + act_roll);
			alt_tv.setText("" + act_alt);
			yaw_tv.setText("" + act_yaw);
			
			btns_tv.setText("" + btns);

            DroneControl.DroneState state = mDrone.getCurrentState();
			drone_state_tv.setText(state.toString());

			
			if (state == DroneControl.DroneState.FLYING)
            {
                mDrone.move(act_roll/600f, act_nick/600f,act_alt/-800f, act_yaw/360f);
            }
			
			if ((btns==1)&&(state == DroneControl.DroneState.CONNECTED || state == DroneControl.DroneState.LANDED)) {
				mDrone.takeoff();
			}
			
			if ((btns==2)&&(state == DroneControl.DroneState.FLYING)) {
				mDrone.land();
			}
		}
		
	}
	
	BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
            UsbDevice device = (UsbDevice) intent
                    .getParcelableExtra(UsbManager.EXTRA_DEVICE);

           if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                if (SpaceNavigator.isMouse(device)) {
                    mMouse.closeDevice();
                }
			} else if (ACTION_USB_PERMISSION.equals(action)) {
                boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                if (granted) {
                    if (!mMouse.openDevice(device)) {
                        Toast.makeText(SpaceMouseControlActivity.this, "Failed to open device!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SpaceMouseControlActivity.this, "No permission to access device!", Toast.LENGTH_SHORT).show();
                }
            }
		}
	};

}
