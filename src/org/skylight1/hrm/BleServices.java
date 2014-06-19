/************************************************************************************
 *
 *  Copyright (C) 2013 HTC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ************************************************************************************/
package org.skylight1.hrm;

import java.util.UUID;


/**
 * UUIDs of GATT services as per the GATT specification:
 * http://developer.bluetooth.org/gatt/services/Pages/ServicesHome.aspx
 */
public class BleServices {
	public static final String ALERT_NOTIFICATION = "00001811-0000-1000-8000-00805f9b34fb";
	public static final String BATTERY_SERVICE = "0000180f-0000-1000-8000-00805f9b34fb";
	public static final String BLOOD_PRESSURE = "00001810-0000-1000-8000-00805f9b34fb";
	public static final String CURRENT_TIME = "00001805-0000-1000-8000-00805f9b34fb";
	public static final String CYCLING_SPEED_AND_CADENCE = "00001816-0000-1000-8000-00805f9b34fb";
	public static final String DEVICE_INFORMATION = "0000180a-0000-1000-8000-00805f9b34fb";
	public static final String GENERIC_ACCESS = "00001800-0000-1000-8000-00805f9b34fb";
	public static final String GENERIC_ATTRIBUTE = "00001801-0000-1000-8000-00805f9b34fb";
	public static final String GLUCOSE = "00001808-0000-1000-8000-00805f9b34fb";
	public static final String HEALTH_THERMOMETER = "00001809-0000-1000-8000-00805f9b34fb";
	public static final String HEART_RATE = "0000180d-0000-1000-8000-00805f9b34fb";
	public static final String HUMAN_INTERFACE_DEVICE = "00001812-0000-1000-8000-00805f9b34fb";
	public static final String IMMEDIATE_ALERT = "00001802-0000-1000-8000-00805f9b34fb";
	public static final String LINK_LOSS = "00001803-0000-1000-8000-00805f9b34fb";
	public static final String NEXT_DST_CHANGE = "00001807-0000-1000-8000-00805f9b34fb";
	public static final String PHONE_ALERT_STATUS = "0000180e-0000-1000-8000-00805f9b34fb";
	public static final String REFERENCE_TIME_UPDATE = "00001806-0000-1000-8000-00805f9b34fb";
	public static final String RUNNING_SPEED_AND_CADENCE = "00001814-0000-1000-8000-00805f9b34fb";
	public static final String SCAN_PARAMETERS = "00001813-0000-1000-8000-00805f9b34fb";
	public static final String TX_POWER = "00001804-0000-1000-8000-00805f9b34fb";
	public static final String FIND_CENTRAL = "0daa5375-02d3-4b47-b6b7-53408ff159e5";
	public static final String SIMPLE_KEYS_SERV_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";

	public static final UUID SVC_BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
	public static final UUID SVC_GENERIC_ACCESS = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
	public static final UUID SVC_DEVICE_INFORMATION = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
	public static final UUID SVC_IMMEDIATE_ALERT = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
	public static final UUID SVC_LINK_LOSS = UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");
	public static final UUID SVC_TX_POWER = UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
	public static final UUID SVC_FIND_CENTRAL = UUID.fromString("0daa5375-02d3-4b47-b6b7-53408ff159e5");
	public static final UUID SVC_SIMPLE_KEYS_SERV_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
	public static final UUID SVC_HEART_RATE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");

}
