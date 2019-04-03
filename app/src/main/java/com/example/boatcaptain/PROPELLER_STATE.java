package com.example.boatcaptain;

public class PROPELLER_STATE {
	public static float MAX_ANGLE = 45;//maximum allowed rudder angle in degrees 
	public static float MIN_ANGLE = -45;//minimum allowed rudder angle in degrees
	public float fRudderAngle;//angle of the air rudder in degrees
	public float fPropSpeed;//rotational speed of the air propeller (arbitrary units)
	
	PROPELLER_STATE() {
		fRudderAngle=0;
		fPropSpeed=0;
	}
	
	public static int dataSize() {
		return 8;
	}
	
	public byte[] getBytes() {//return an array of bytes that corresponds to the 2 thruster speed values
		byte []retVal = new byte[8];
		byte []speed1Bytes = Util.toByteArray(fRudderAngle);
		byte []speed2Bytes = Util.toByteArray(fPropSpeed);
		for (int i=0;i<4;i++) {
			retVal[i] = speed1Bytes[i];
			retVal[4+i] = speed2Bytes[i];
		}
		return retVal;
	}
}
