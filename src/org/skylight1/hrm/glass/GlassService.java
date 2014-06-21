package org.skylight1.hrm.glass;

import org.skylight1.hrm.BleCharacteristics;
import org.skylight1.hrm.BluetoothLeService;
import org.skylight1.hrm.R;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.glass.timeline.LiveCard;

/**
 * The main application service that manages the lifetime of the compass live card and the objects
 * that help out with orientation tracking and landmarks.
 */
public class GlassService extends Service {
	protected static final String TAG = "GlassService";
    private static final String LIVE_CARD_TAG = "hrm";
	private RemoteViews mLiveCardView;
    private TextToSpeech mSpeech;
    private LiveCard mLiveCard;    
    String previousString;
    boolean speakAloud;
    
  private String mDeviceName = "Wahoo HRM V1.7";
  private String mDeviceAddress = "DC:BB:C5:15:AF:86";
//  private String mDeviceName = "Wahoo HRM V2.1";
//  private String mDeviceAddress = "C4:18:B8:93:54:50";
  
    private boolean mConnected;
    private BluetoothLeService mBluetoothLeService;

    private final HRMDemoBinder mBinder = new HRMDemoBinder();

    /**
     * A binder that gives other components access to the speech capabilities provided by the
     * service.
     */
    public class HRMDemoBinder extends Binder {
        public void readAloud() {
            Resources res = getResources();
            String headingText = "enable speaking heart rate";
            mSpeech.speak(headingText, TextToSpeech.QUEUE_FLUSH, null);
            speakAloud = true;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
            }
        });
        
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        
//            Intent chooseIntent = new Intent(getBaseContext(), CardScrollActivity.class);
//            chooseIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            getApplication().startActivity(chooseIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
	@Override
    public void onDestroy() {
        if (mLiveCard != null && mLiveCard.isPublished()) {
            mLiveCard.unpublish();
            mLiveCard = null;
        }
        mSpeech.shutdown();
        mSpeech = null;
        
        unregisterReceiver(mGattUpdateReceiver);
        
    	mBluetoothLeService.disconnect();        
        
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }
	
	///////////
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                return;
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
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            	Log.i(TAG,"ACTION_GATT_SERVICES_DISCOVERED");
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            	
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

            	Log.d(TAG,"DATA: "+intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));

            } else if (BluetoothLeService.RSSI_DATA.equals(action)) {
//	            displayRSSI(intent.getIntExtra(BluetoothLeService.EXTRA_DATA,0));
	        }
        }
    };


	protected byte getWriteValue() {
		return BleCharacteristics.ALERT_LEVEL_HIGH;
	}
	
	private int previousBeat;

    private void setDisplay(int beat) {
    	if(beat != 0) {
    			if(previousBeat!=beat) {
    				if(speakAloud) {
    					mSpeech.speak(""+beat, TextToSpeech.QUEUE_FLUSH, null);
    				}
    	            mLiveCardView.setTextViewText(R.id.hrm_text, ""+beat);
    	            mLiveCard.setViews(mLiveCardView);
    			}
    			previousBeat = beat;
    	}
    }
    private void displayData(String strBeat) {
    	int beat = 0;
    	try {
    		beat = Integer.parseInt(strBeat);
    	} catch(NumberFormatException nfe) {
    		beat = 0;
    	}
    	setDisplay(beat);
//		Runnable task = new Runnable() {
//			public void run() {
//			}
//		};							
//	    new Handler(Looper.getMainLooper()).post(task);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
                        
        if (mLiveCard == null) {
            mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
            mLiveCardView = new RemoteViews(getPackageName(), R.layout.hrmdemo_glass);

            mLiveCardView.setTextViewText(R.id.hrm_text, "bpm");
            mLiveCard.setViews(mLiveCardView);

            // Display the options menu when the live card is tapped.
            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));

            // Publish the live card
            mLiveCard.publish(LiveCard.PublishMode.REVEAL);
            Log.d(TAG, "mLiveCard.publish " + mLiveCard.isPublished());
            
        } else if (!mLiveCard.isPublished()) {
            mLiveCard.publish(LiveCard.PublishMode.REVEAL);
        } else {
            mLiveCard.navigate();
        }

        return START_STICKY;
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

}
