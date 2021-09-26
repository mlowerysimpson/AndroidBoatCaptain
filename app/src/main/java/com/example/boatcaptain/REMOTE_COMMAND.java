package com.example.boatcaptain;

import java.util.Calendar;

public class REMOTE_COMMAND {
	public static int THRUST_ON = 0;//turn one or both propellers on at defined speed and direction
	public static int THRUST_OFF = 1;//turn both propellers off
	public static int CPU_TEMP_PACKET = 2;//temperature of the Compute Module chip data packet
	public static int COMPASS_DATA_PACKET = 3;//compass / tilt data packet
	public static int GPS_DATA_PACKET = 4;//GPS data packet
	public static int BATTVOLTAGE_DATA_PACKET = 11;//battery voltage data packet
	public static int SUPPORTED_SENSOR_DATA = 12;//find out what types of sensor data the boat is capable of collecting
	public static int SENSOR_TYPES_INFO = 13;//code that indicates that sensor type info will follow (see SensorDataFile.h for definitions of the sensor types)
	public static int WATER_TEMP_DATA_PACKET = 14;//water temperature data packet
	public static int WATER_PH_DATA_PACKET = 15;//water pH data packet
	public static int VIDEO_DATA_PACKET = 17;//screen capture from video camera data packet
	public static int GPS_DESTINATION_PACKET = 18;//GPS destination packet (used to tell the boat where to go)
	public static int WATER_TURBIDITY_DATA_PACKET = 19;//water turbidity data packet
	public static int LEAK_DATA_PACKET = 20;//leak info data packet
	public static int DIAGNOSTICS_DATA_PACKET = 21;//diagnostics info data packet
	public static int CANCEL_OPERATION = 22;//cancel the operation currently in progress
	public static int QUIT_PROGRAM = 23;//quit the currently running Pi program (RemoteControlTest) on AMOS
	public static int ENTER_SLEEP = 24;//put AMOS into sleep mode for an optional length of time (or indefinitely). AMOS wakes up when time elapses or when a command to wakeup is received over the serial wireless link.
	public static int SCRIPT_STATUS_PACKET = 25;//used to request or send the current status of the file script currently running on AMOS
	public static int SCRIPT_STEP_CHANGE = 26;//used to change the step of the currently running file script
	public static int LIST_REMOTE_SCRIPTS = 27;//command sent to AMOS to return a list of all of the available remote scripts
	public static int USE_REMOTE_SCRIPT = 28;//command sent to AMOS to use a particular remote script
	public static int FILE_TRANSFER = 29;//transfer a particular file to AMOS
	public static int FILE_RECEIVE = 30;//receive / download a particular file from AMOS
	public static int REFRESH_SETTINGS = 31;//command to refresh settings from a partiulcar part of the prefs.txt file
	public static int LIST_REMOTE_DATA = 32;//command sent to AMOS to return a list of all of the available data files
	public static int LIST_REMOTE_LOG = 33;//command sent to AMOS to return a list of all of the available log files
	public static int LIST_REMOTE_IMAGE = 34;//command sent to AMOS to return a list of all of the available image files
	public static int RTK_CORRECTION = 35;//sending RTK correction bytes to AMOS from an RTK base station
	public static int DELETE_FILES = 36;//delete one or more AMOS files
	public static int LAST_COMMAND = 37;//the highest possible command #


	public static int TIMEOUT_TIME_MS = 5000;//timeout value in ms to use for communications

	public static float MAX_SPEED = 10;//maximum speed allowed to use for the thrusters / propellers
	public int nCommand;//command code sent from remote host
	public int nNumDataBytes;//number of data bytes included with command
	public long lCommandTimeMS;//time (in ms) when command was created
	public byte [] pDataBytes;//remotely received data bytes, may be NULL if no data bytes were received
	REMOTE_COMMAND() {
		nCommand=0;
		nNumDataBytes=0;
		Calendar calendar = Calendar.getInstance();
		lCommandTimeMS=calendar.getTimeInMillis();
		pDataBytes=null;
	}

	/**
	 *
	 * @return true if this command was created more than TIMEOUT_TIME_MS ago
	 */
	public boolean isCommandOld() {
		Calendar calendar = Calendar.getInstance();
		long lCurrentTimeMS = calendar.getTimeInMillis();
		if ((lCurrentTimeMS-lCommandTimeMS)>=TIMEOUT_TIME_MS) {
			return true;
		}
		return false;
	}

	/**
	 * Renew the time of the command to the current time in ms
	 */
	public void RenewTime() {
		Calendar calendar = Calendar.getInstance();
		lCommandTimeMS=calendar.getTimeInMillis();
	}
}
