package org.skylight1.hrm;

import java.util.UUID;

import android.bluetooth.BluetoothGattCharacteristic;

public class BleProfiles {

	public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString(BleCharacteristics.HEART_RATE_MEASUREMENT);
    public final static UUID UUID_MANUFACTURER_NAME = UUID.fromString(BleCharacteristics.MANUFACTURER_NAME_STRING);
    public final static UUID UUID_SOFTWARE_REVISION_STRING  = UUID.fromString(BleCharacteristics.SOFTWARE_REVISION_STRING);
    public final static UUID UUID_DEVICE_NAME_STRING  = UUID.fromString(BleCharacteristics.DEVICE_NAME);
    public final static UUID UUID_FIND_CENTRAL = UUID.fromString(BleCharacteristics.FIND_CENTRAL_CONFIG);
    public final static UUID UUID_ALERT_LEVEL = UUID.fromString(BleCharacteristics.ALERT_LEVEL);
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

	public static String processReadCharacteristic(BluetoothGattCharacteristic characteristic) {
		String result = "";
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
            }
            final int heartRate = characteristic.getIntValue(format, 1);        
            result = String.valueOf(heartRate);

        } else if(UUID_MANUFACTURER_NAME.equals(characteristic.getUuid()) 
        		|| UUID_SOFTWARE_REVISION_STRING.equals(characteristic.getUuid())
        		|| UUID_DEVICE_NAME_STRING.equals(characteristic.getUuid()) ) {
        	result = new String(characteristic.getValue());
        }
		return result;
	}
	
	public static BluetoothGattCharacteristic processWriteCharacteristic(BluetoothGattCharacteristic characteristic, byte value) {		
		BluetoothGattCharacteristic writeCharacteristic = characteristic;
		if(UUID_ALERT_LEVEL.equals(writeCharacteristic.getUuid()))  {
			byte[] values = { value };
			writeCharacteristic.setValue(values);
			writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
		} else {
			writeCharacteristic = null;
		}
		return writeCharacteristic;
	}
	
	public static UUID getdescriptor(BluetoothGattCharacteristic characteristic) {
		UUID descriptor = null;
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid()) || UUID_FIND_CENTRAL.equals(characteristic.getUuid()) ) {
        	descriptor = UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG);
        } 
        else if(UUID_ALERT_LEVEL.equals(characteristic.getUuid())) {
        	descriptor = UUID.fromString(BleCharacteristics.ALERT_NOTIFICATION_CONTROL_POINT);
        }
		return descriptor;
	}
}
