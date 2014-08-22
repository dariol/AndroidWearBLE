package org.skylight1.hrm;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class HRMDemoActivity extends Activity {
	public static final String TAG = "HRMDemoActivity";
	private static final String EXTRA_EVENT_ID = "EXTRA_EVENT_ID";
    private static final long SCAN_PERIOD = 10000;
    private static final int REQUEST_ENABLE_BT = 1;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

	private TextView mStatus;
	private TextView mText;
	
	//TODO: have Android phone app's Settings set these limits!
	private int HIGH_LIMIT = 91;
	private int LOW_LIMIT = 86;
	
	//default device for development testing...
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

        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
		
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE); 
        
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        //setListAdapter(mLeDeviceListAdapter);
    }
    
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
//            scanLeDevice(true);
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
		long pattern[] = {0,1000}; 
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
    
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        final Intent intent = new Intent(this, HRMDemoActivity.class);
//        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
//        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }
    
    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = HRMDemoActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    Log.e(TAG, device.getName()+" "+device.getAddress());
                }
            });
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
