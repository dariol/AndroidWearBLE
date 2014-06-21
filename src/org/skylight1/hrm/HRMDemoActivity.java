package org.skylight1.hrm;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.preview.support.v4.app.NotificationManagerCompat;
import android.preview.support.wearable.notifications.WearableNotifications;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.TextView;


public class HRMDemoActivity extends Activity {
	public static final String TAG = "HRMDemoActivity";
	private static final String EXTRA_EVENT_ID = "EXTRA_EVENT_ID";

	private TextView mStatus;
	private TextView mText;
	private int HIGH_LIMIT = 91;
	private int LOW_LIMIT = 86;
	
    private String mDeviceName = "Wahoo HRM V1.7";
    private String mDeviceAddress = "DC:BB:C5:15:AF:86";
//    private String mDeviceName = "Wahoo HRM V2.1";
//    private String mDeviceAddress = "C4:18:B8:93:54:50";

    private boolean mConnected;
    private BluetoothLeService mBluetoothLeService;
	String prevStrBeatMessage = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_hrmdemo);
		mText = (TextView) findViewById(R.id.hrm_text);
		mStatus = (TextView) findViewById(R.id.hrm_status);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE); 
    }
    
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            mBluetoothLeService.connect(mDeviceAddress);
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

		@Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                mStatus.setText("connected");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                mStatus.setText("disconnected");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
//                Log.e(TAG,"SERVICES_DISCOVERED");
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                Log.e(TAG,"DATA: "+intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                
                setDisplay(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                
            } else if (BluetoothLeService.RSSI_DATA.equals(action)) {
            	Log.e(TAG,"RSSI: "+intent.getIntExtra(BluetoothLeService.EXTRA_DATA,0));
            	displayRSSI(intent.getIntExtra(BluetoothLeService.EXTRA_DATA,0));
	        }
        }
    };
	private String previousBeat;

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
    	if(mBluetoothLeService!=null) {
    		mBluetoothLeService.disconnect();
    	}
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }
    
    
    private void displayRSSI(int rssi) {
    	int result = rssi; 
    	// for proximity level use: tx+power - rssi, for rough distance use:
    	// d = (rssi-A)/-20 (where A = rssi at one meter)
    	String rssiString = Integer.toString(result);
    	Log.i(TAG,"rssi: "+rssiString);
	}

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.RSSI_DATA);
        return intentFilter;
    }

    
    private void setDisplay(final String beat) {
    	if(beat != null) {
    		if(previousBeat!=beat) {
    			mText.setText(beat);
    			sendAndroidWearNotification(beat);
			}
			previousBeat = beat;
    	}
    }
    
    public void sendAndroidWearNotification(String beatStr) {
	    final int notificationId = 1;
    	int beat = 0;
    	try {
    		beat = Integer.parseInt(beatStr);
    	} catch(NumberFormatException nfe) {
    		beat = 0;
    	}
    	if(beat==0) {
    		return;
    	}
 
    	String strBeatMessage = getString(R.string.fastmessage);
    	int iconBeatMessage = R.drawable.ic_fastheart;
    	if(beat < HIGH_LIMIT && beat > LOW_LIMIT) {
    		return;
    	} else if(beat < LOW_LIMIT) {
    		strBeatMessage = getString(R.string.slowmessage);
    		iconBeatMessage = R.drawable.ic_slowheart;
    	}
    	if(prevStrBeatMessage !=strBeatMessage) {
	    	NotificationManagerCompat.from(this).cancelAll();
			Intent viewIntent = new Intent(this, HRMDemoActivity.class);
			viewIntent.putExtra(EXTRA_EVENT_ID, notificationId);
			PendingIntent viewPendingIntent = PendingIntent.getActivity(this, 0, viewIntent, 0);
			long pattern[] = {0,1000};
			NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
			        .setContentTitle("HRM Demo")
			        .setContentText(strBeatMessage)
			        .setSmallIcon(R.drawable.ic_launcher)
			        .setPriority(NotificationCompat.PRIORITY_MAX)
			        .setVibrate(pattern)
			        .setLargeIcon(BitmapFactory.decodeResource(getResources(), iconBeatMessage))
			        .setContentIntent(viewPendingIntent);
	
	        Notification notification = new WearableNotifications.Builder(notificationBuilder).build();
	
			NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
			notificationManager.notify(notificationId, notification);
    	}
    	prevStrBeatMessage=strBeatMessage;
	}
}
