package com.example.boatcaptain;

public class GPS_DATA {
	public double dLatitude;//latitude in degrees (-90 to +90)
	public double dLongitude;//longitude in degrees (-180 to +180)
	public long gps_time;//time of the GPS reading (ms since Jan 01, 1970, 00:00:00)
	GPS_DATA() {
		dLatitude=0.0;
		dLongitude=0.0;
		gps_time=0;
	}
	
	public static int dataSize() {//return the number of bytes that are used to send a GPS_DATA packet
		return 24;	
	}
	
	
}
