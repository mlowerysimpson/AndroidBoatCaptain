package com.example.boatcaptain;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;

public class Captain {
	//data
	protected double m_dLatitude;//the last known latitude of the boat
	protected double m_dLongitude;//the last known longtitude of the boat
	protected long m_gpsTime;//the last known time of a GPS reading from the boat (in ms since Jan 01, 1970, 00:00:00)
	protected float m_fBatteryVoltage;//the voltage of the boat's battery
	protected boolean m_bLeakDetected;//whether or not a leak was detected in the boat
	protected float m_fCurrentDraw;//the amount of current drawn by AMOS on its +12V power supply
	protected float m_fWirelessRXPower;//the power level measured by the remote wireless receiver
	protected float m_fWaterTemp;//the last known water temperature measured by the boat
	protected float m_fPH;//the last known water pH value measured by the boat
	protected float m_fWaterTurbidity;//the last known water turbidity measured by the boat
	protected float m_fHumidity;//the humidity inside the main enclosure of AMOS
	protected float m_fHumidityTemp;//the temperature as measured by the humidity sensor in AMOS
	protected boolean m_bSolarAvailable;//flag is true if solar power is available for charging the boat's battery, otherwise false
	protected IMU_DATASAMPLE m_compassData;//the last known compass data from the boat
	protected int m_nNumSensorsAvailable;//the total number of sensors that the boat has available
	protected int []m_sensorTypes;//the types of sensors that the boat has available, see SensorDataFile.h for a list of the supported sensor types
	public String m_sLastError;//string describing the last network error that occurred
	public int m_nNumImageBytes;//the number of image bytes saved by the boat that are available for download
	private long m_lastIncrDecrTime;//the time (in ms) when a quantity was last incremented or decremented
	
	private PROPELLER_STATE m_currentPropState;//the current state of the propellers
	private SimpleDateFormat m_dateFormat;
	
	Captain() {
		m_lastIncrDecrTime = 0;
		m_nNumImageBytes = 0;
		m_bLeakDetected = false;
		m_bSolarAvailable = false;
		m_fCurrentDraw=0;
		m_fWirelessRXPower=0;
		m_fWaterTemp=0;
		m_fPH=0;
		m_fWaterTurbidity=0;
		m_fHumidity=0;
		m_fHumidityTemp=0;
		m_nNumSensorsAvailable=0;
		m_sensorTypes = null;
		m_currentPropState = new PROPELLER_STATE();
		m_dLatitude=0.0;
		m_dLongitude=0.0;
		m_fBatteryVoltage=0;
		m_gpsTime=0;
		m_compassData = new IMU_DATASAMPLE();
		m_dateFormat = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss");
	}
	
	public PROPELLER_STATE GetCurrentPropState() {// PROPELLER_STATE//returns the current state of the propellers as a pointer to a PROPELLER_STATE structure
		return m_currentPropState;		
	}
	
	public PROPELLER_STATE Stop() {//stop thrusters, set speed of both to zero
		PROPELLER_STATE pPropState = new PROPELLER_STATE();
		pPropState.fRudderAngle=0;
		pPropState.fPropSpeed=0;
		m_currentPropState.fRudderAngle=0;
		m_currentPropState.fPropSpeed=0;
		return pPropState;
	}
	
	public PROPELLER_STATE ForwardHo() {//move forward, increase forward speed (up to limit)
		PROPELLER_STATE pPropState = IncrementForwardSpeed();
		return pPropState;
	}
	
	public PROPELLER_STATE StarboardHo() {//move to right, increase right turning speed (up to limit)
		PROPELLER_STATE pPropState = IncrementRightSpeed();
		return pPropState;
	}
	
	public PROPELLER_STATE PortHo() {//move to left, increase left turning speed (up to limit)
		PROPELLER_STATE pPropState = IncrementLeftSpeed();
		return pPropState;	
	}
	
	public PROPELLER_STATE BackHo() {//move backward, increase backward speed (up to limit)
		PROPELLER_STATE pPropState = IncrementBackSpeed();
		return pPropState;
	}
	
	public void DisplayLastSockError(String sError, Context cText) {//display the most recent Sockets error text
		SimpleMessage.msbox("Error", sError, cText);
	}
	
	public String FormatGPSData() {//format the current GPS data as a string, ex: 45.334523° N, 62.562533° W, 2018-06-18, 17:23:00
		String sRetval="";
		String sSN = "N";
		if (m_dLatitude<0) sSN = "S";
		String sEW = "E";
		if (m_dLongitude<0) sEW = "W";
		
		if (m_gpsTime!=0) {
			String sTime = m_dateFormat.format(new Date(m_gpsTime*1000));//m_gpsTime is in seconds, so need to convert it to ms to work with the Date constructor
			sRetval = String.format("%.6f° %s, %.6f° %s, %s",Math.abs(m_dLatitude),sSN,Math.abs(m_dLongitude),sEW,sTime);
		}
		return sRetval;
		
	}
	
	public String FormatCompassData() {//format the current compass data as a string, ex: Heading = 175.2°, Roll = 1.4°, Pitch = 1.8°, Temp = 19.2 °C
		String sRetval="";
		//get average temperature from mag & acc/gyro temp sensors
		double dAvgTemp = (m_compassData.mag_temperature + m_compassData.acc_gyro_temperature)/2;
		sRetval = String.format("Heading = %.1f°, Roll = %.1f°, Pitch = %.1f°, Temp = %.1f °C",m_compassData.heading,m_compassData.roll,m_compassData.pitch,dAvgTemp);
		return sRetval;
	}
	
	public boolean ProcessBoatData(BOAT_DATA pBoatData) {//process data from boat, return true if boat data could be successfully processed
		if (pBoatData.nPacketType==REMOTE_COMMAND.GPS_DATA_PACKET) {
			if (pBoatData.nDataSize!=GPS_DATA.dataSize()) {
				return false;
			}
			byte[] latitude_bytes = new byte[8];
			byte[] longitude_bytes = new byte[8];
			byte[] time_bytes = new byte[8];
			for (int i=0;i<8;i++) {
				latitude_bytes[i] = pBoatData.dataBytes[i];
				longitude_bytes[i] = pBoatData.dataBytes[8+i];
				time_bytes[i] = pBoatData.dataBytes[16+i];
			}
			m_dLatitude = Util.toDouble(latitude_bytes);
			m_dLongitude = Util.toDouble(longitude_bytes);
			m_gpsTime = Util.toLong(time_bytes);
			return true;
		}
		else if (pBoatData.nPacketType==REMOTE_COMMAND.COMPASS_DATA_PACKET) {
			if (pBoatData.nDataSize!=IMU_DATASAMPLE.dataSize()) {
				return false;
			}
			m_compassData.setData(pBoatData.dataBytes);
			return true;
		}
		else if (pBoatData.nPacketType==REMOTE_COMMAND.BATTVOLTAGE_DATA_PACKET) {
			if (pBoatData.nDataSize!=4) {
				return false;
			}
			byte []voltage_bytes = new byte[4];
			for (int i=0;i<4;i++) {
				voltage_bytes[i] = pBoatData.dataBytes[i];
			}
			m_fBatteryVoltage = Util.toFloat(voltage_bytes);
			return true;
		}
		else if (pBoatData.nPacketType==REMOTE_COMMAND.LEAK_DATA_PACKET) {
			if (pBoatData.nDataSize!=4) {
				return false;
			}
			int nLeakDetected = Util.toInt(pBoatData.dataBytes);
			if (nLeakDetected>0) {
				this.m_bLeakDetected =true;
			}
			else m_bLeakDetected = false;
			return true;
		}
		else if (pBoatData.nPacketType==REMOTE_COMMAND.DIAGNOSTICS_DATA_PACKET) {
			if (pBoatData.nDataSize!=24) {
				return false;
			}
			//battery voltage
			m_fBatteryVoltage = Util.toFloat(pBoatData.dataBytes);

			//current draw 
			byte [] currentBytes = new byte[4];
			for (int i=0;i<4;i++) {
				currentBytes[i] = pBoatData.dataBytes[4+i];
			}
			m_fCurrentDraw = Util.toFloat(currentBytes);
			
			//humidity
			byte [] humidityBytes = new byte[4];
			for (int i=0;i<4;i++) {
				humidityBytes[i] = pBoatData.dataBytes[8+i];
			}
			m_fHumidity = Util.toFloat(humidityBytes);
			
			//humidity temperature (i.e. temperature as measured by the humidity sensor)
			byte [] humidityTempBytes = new byte[4];
			for (int i=0;i<4;i++) {
				humidityTempBytes[i] = pBoatData.dataBytes[12+i];
			}
			m_fHumidityTemp = Util.toFloat(humidityTempBytes);
			
			//wireless RX power
			byte [] rxPowerBytes = new byte[4];
			for (int i=0;i<4;i++) {
				rxPowerBytes[i] = pBoatData.dataBytes[16+i];
			}
			m_fWirelessRXPower = Util.toFloat(rxPowerBytes);
					
			//solar power availability
			byte [] solarBytes = new byte[4];
			for (int i=0;i<4;i++) {
				solarBytes[i] = pBoatData.dataBytes[20+i];
			}
			int nSolarAvail = Util.toInt(solarBytes);
			if (nSolarAvail>0) m_bSolarAvailable = true;
			else m_bSolarAvailable = false;
			return true;
		}
		return false;
		
	}
	
	PROPELLER_STATE IncrementForwardSpeed() {//increase speed in forward direction for both props
		//if one or more props are negative, then just increase the speed of the negative props
		float fRudderAngle = m_currentPropState.fRudderAngle;
		float fPropSpeed = m_currentPropState.fPropSpeed;
		fPropSpeed++;
		if (fPropSpeed> REMOTE_COMMAND.MAX_SPEED) {
			fPropSpeed= REMOTE_COMMAND.MAX_SPEED;
		
		} 
		m_currentPropState.fRudderAngle = fRudderAngle;
		m_currentPropState.fPropSpeed = fPropSpeed;
		return m_currentPropState;
	}
	
	PROPELLER_STATE IncrementRightSpeed() {//increase speed in right direction only, by incrementing the left prop speed, and decreasing the right prop speed (if going forward)
		float fRudderAngle = m_currentPropState.fRudderAngle;
		float fPropSpeed = m_currentPropState.fPropSpeed;
		
		fRudderAngle = Increment(fRudderAngle);
		if (fRudderAngle>PROPELLER_STATE.MAX_ANGLE) {
			fRudderAngle = PROPELLER_STATE.MAX_ANGLE;
		}
		m_currentPropState.fRudderAngle = fRudderAngle;
		m_currentPropState.fPropSpeed = fPropSpeed;
		return m_currentPropState;
	}

	PROPELLER_STATE IncrementLeftSpeed() {//increase speed in left direction only, by incrementing the right prop speed, and decreasing the left prop speed (if going forward)
		float fRudderAngle = m_currentPropState.fRudderAngle;
		float fPropSpeed = m_currentPropState.fPropSpeed;
	
		fRudderAngle = Decrement(fRudderAngle);
		if (fRudderAngle<PROPELLER_STATE.MIN_ANGLE) {
			fRudderAngle=PROPELLER_STATE.MIN_ANGLE;	
		}
		
		
		m_currentPropState.fRudderAngle = fRudderAngle;
		m_currentPropState.fPropSpeed = fPropSpeed;
		return m_currentPropState;
	}

	PROPELLER_STATE IncrementBackSpeed() {//increase speed in back direction for both props
		//if one or more props are positive, then just decrease the speed of the positive props
		float fRudderAngle = m_currentPropState.fRudderAngle;
		float fPropellerSpeed = m_currentPropState.fPropSpeed;
		fPropellerSpeed--;
		if (fPropellerSpeed<0) {
			fPropellerSpeed=0;
		}
		m_currentPropState.fRudderAngle = fRudderAngle;
		m_currentPropState.fPropSpeed = fPropellerSpeed;
		return m_currentPropState;
	}
	
	public String GetLastError() {
		return m_sLastError;
	}
	
	public String FormatLeakData() {//format the leak sensor data
		String sLeakData="Leak Detected: NO";
		if (this.m_bLeakDetected) {
			sLeakData="Leak Detected: YES";
		}
		return sLeakData;
		
	}
	
	public String FormatCurrentData() {//format the AMOS's current data
		String sCurrentData = String.format("Current (@12V): %.2f A",this.m_fCurrentDraw);
		return sCurrentData;
	}
	
	public String FormatSolarPower() {//output string concerning status of whether or not solar power is available
		String sSolarAvailable = "Solar: YES";
		if (!m_bSolarAvailable) {
			sSolarAvailable = "Solar: NO";
		}
		return sSolarAvailable;
	}
	
	public String FormatDiagnosticsData() {//format the received diagnostics data
		String sRXPower="";
		if (m_fWirelessRXPower!=0) {
			sRXPower = String.format("RX Power: %.0f dBm",m_fWirelessRXPower);
		}
		else sRXPower = "RX Power: N.A.";
		String sDiagData="";
		if (m_bSolarAvailable) {
			sDiagData = String.format("Voltage: %.3f V, Current (@12 V): %.2f A, RH = %.1f %%, Temp = %.1f °C, %s, Solar: YES",
				m_fBatteryVoltage, m_fCurrentDraw, m_fHumidity, m_fHumidityTemp, sRXPower);
		}
		else sDiagData = String.format("Voltage: %.3f V, Current (@12 V): %.2f A, RH = %.1f %%, Temp = %.1f °C, %s, Solar: NO",
			m_fBatteryVoltage, m_fCurrentDraw, m_fHumidity, m_fHumidityTemp, sRXPower);
		return sDiagData;
	}

	public String FormatVoltageData() {
		String sVoltage = String.format("Voltage: %.2f V", this.m_fBatteryVoltage);
		return sVoltage;
	}

	private float Increment(float fVal) {//increments a value, but does so in an accelerated fashion if the same function has been called recently, i.e. within the last 2 seconds
		boolean bRecentlyChanged = false;
		long currentTime = System.currentTimeMillis();
		if (m_lastIncrDecrTime>0) {
			long lTimeElapsed = currentTime - m_lastIncrDecrTime;
			if (lTimeElapsed<2000) {
				bRecentlyChanged = true;
			}
		}
		float fIncrementVal = 1;
		if (bRecentlyChanged) {//increment value instead by 25% of its current value (if that is bigger than 1)
			if (Math.abs(fVal/4)>fIncrementVal) {
				fIncrementVal = Math.abs(fVal/4);
			}
		}
		m_lastIncrDecrTime = currentTime;
		return fVal + fIncrementVal;
	}

	private float Decrement(float fVal) {//decrements a value, but does so in an accelerated fashion if the same function has been called recently, i.e. within the last 2 seconds
		boolean bRecentlyChanged = false;
		long currentTime = System.currentTimeMillis();
		if (m_lastIncrDecrTime>0) {
			long lTimeElapsed = currentTime - m_lastIncrDecrTime;
			if (lTimeElapsed<2000) {
				bRecentlyChanged = true;
			}
		}
		float fDecrementVal = 1;
		if (bRecentlyChanged) {//decrement value instead by 25% of its current value (if that is bigger than 1)
			if (Math.abs(fVal/4)>fDecrementVal) {
				fDecrementVal = Math.abs(fVal/4);
			}
		}
		m_lastIncrDecrTime = currentTime;
		return fVal - fDecrementVal;
	}
}


