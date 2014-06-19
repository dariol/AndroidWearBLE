package org.skylight1.hrm;

import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

public class BluetoothLeService extends Service {

	private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private Handler mTimerHandler = new Handler();
    private boolean mTimerEnabled = false;

	protected int retries;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static String RSSI_DATA =
            "com.example.bluetooth.le.RSSI_DATA";

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "connected to GATT server");
                mBluetoothGatt.discoverServices();
//                readRSSIContinually(true);  // optional

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "disconnected from GATT server");
                broadcastUpdate(intentAction);
            }
        }
        @Override 
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status){
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(RSSI_DATA, rssi);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	
				if(gatt.getServices().size()<8 && retries<5) {
					retries++;
					Log.w(TAG,"found only "+gatt.getServices().size()+" services, retrying discoverServices...");
					try {Thread.sleep(50);} catch(InterruptedException ioe){}
					gatt.discoverServices();
					return;
				}

				startMonitoring(gatt, true);

                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        private void startMonitoring(BluetoothGatt gatt, boolean value) {
			if(gatt==null) {
				return;
			}
			BluetoothGattService service = gatt.getService(BleServices.SVC_HEART_RATE);
			BluetoothGattCharacteristic characteristic = null;
			if(service==null) {
				Log.i(TAG,"service is null, trying creating one and characteristic");
				service = new BluetoothGattService(BleServices.SVC_HEART_RATE,0);
				characteristic = new BluetoothGattCharacteristic(BleCharacteristics.CHAR_HEART_RATE_MEASUREMENT, 16, 0);
				service.addCharacteristic(characteristic);
			} else {
				characteristic = service.getCharacteristic(BleCharacteristics.CHAR_HEART_RATE_MEASUREMENT);
				if(characteristic==null) {
					Log.i(TAG,"characteristic is null");
					return;
				}
			}
			setCharacteristicNotification(characteristic, value);	
			Log.i(TAG,"setCharacteristicNotification "+value);
		}
		@Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };
    
    public void readRSSIContinually(final boolean enable) {
    	mTimerEnabled = enable;    	
    	if(mConnectionState != STATE_CONNECTED || mBluetoothGatt == null || mTimerEnabled == false) {
    		mTimerEnabled = false;
    		return;
    	}    	
    	mTimerHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if(mBluetoothGatt == null ||
				  mBluetoothAdapter == null ||
				  mConnectionState != STATE_CONNECTED) {
					mTimerEnabled = false;
					return;
				}
				try {
					mBluetoothGatt.readRemoteRssi();
					readRSSIContinually(mTimerEnabled);
				} catch(Exception doe) {
					Log.e(TAG,"failed to read RSSI: "+doe);
					readRSSIContinually(false);
				}
			}
    	}, 2000);
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for profiles.
    	// Data parsing is carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx
        String result = BleProfiles.processReadCharacteristic(characteristic);
		intent.putExtra(EXTRA_DATA, result);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final int rssi) {
		final Intent intent = new Intent(action);
		intent.putExtra(EXTRA_DATA, rssi);
		sendBroadcast(intent);
	}

    
    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }
        return true;
    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)  && mBluetoothGatt != null) {
            Log.d(TAG, "using an existing mBluetoothGatt for connection");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        mConnectionState = STATE_CONNECTING;

        Runnable task = new Runnable() {
			public void run() {
		        mBluetoothGatt = device.connectGatt(BluetoothLeService.this, false, mGattCallback);
		        Log.d(TAG, "device.connectGatt()");
		        mBluetoothDeviceAddress = address;
			}
		};
//	    new Handler(Looper.getMainLooper()).post(task);
		new Thread(task).start();
		
        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        if(mBluetoothGatt != null) {
			readRSSIContinually(false);
	        mBluetoothGatt.disconnect();
        }
    }
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
		mBluetoothGatt.writeCharacteristic(characteristic);
	}
    
	/**
	 * Enables or disables notification on a given characteristic.
	 * 
	 * @param characteristic
	 *            Characteristic to act on.
	 * @param enabled
	 *            If true, enable notification. False otherwise.
	 */
	public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
		if (mBluetoothGatt == null || mConnectionState != STATE_CONNECTED) {
			return false;
		}
		if (mBluetoothAdapter == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return false;
		} else if (mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothGatt not available");
			return false;
		}

		if (!mBluetoothGatt.setCharacteristicNotification(characteristic, enabled)) {
			Log.w(TAG, "setCharacteristicNotification failed");
			return false;
		}
		UUID descriptorUUID = BleProfiles.getdescriptor(characteristic);

		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorUUID);
		if (descriptor != null) {
			if (enabled) {
				if(!descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
					Log.e(TAG,"setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) FAILED");
				}
			} else {
				if(!descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
					Log.e(TAG,"setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) FAILED");
				}
			}
			boolean ok = mBluetoothGatt.writeDescriptor(descriptor);
			if(!ok) {
				Log.e(TAG,"bluetoothGatt.writeDescriptor "+descriptor.getUuid().toString()+" FAILED");				
			}
			return ok;
		} else {
			return false;
		}
	}

//    public List<BluetoothGattService> getSupportedGattServices() {
 //       if (mBluetoothGatt == null) return null;
  //      return mBluetoothGatt.getServices();
   // }
}
