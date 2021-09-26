package com.example.boatcaptain;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
 
public class GPSTracker extends Service implements LocationListener {
 
    private final Context mContext;

    // flag for GPS status
    boolean isGPSEnabled = false;
 
    // flag for network status
    boolean isNetworkEnabled = false;
 
    // flag for GPS status
    boolean canGetLocation = false;
 
    Location location; // location
    double latitude; // latitude
    double longitude; // longitude
    
    //engine parameters (at time of GPS data arrival)
    public byte []m_batteryVoltageBytes = new byte[2];
	public byte []m_coolantTemperatureBytes = new byte[1];
	public byte []m_engineRPMBytes = new byte[2];
	public byte []m_roadSpeedBytes = new byte[1];
	public byte []m_pctFuelRemainingBytes = new byte[1];
	public byte []m_controlModuleVoltageBytes = new byte[2];
	public byte []m_numSecondsSinceEngineStartedBytes = new byte[2];
	public byte []m_protocolBytes = new byte[1];
	public byte []m_idleTimeBytes = new byte[2];
    
 
    // The minimum distance to change Updates in meters
    private long MIN_DISTANCE_CHANGE_FOR_UPDATES = 3; // 3 meters
 
    // The minimum time between updates in milliseconds
    private long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute
 
    // Declaring a Location Manager
    protected LocationManager locationManager;
 
    public GPSTracker(Context context) {
        this.mContext = context;
        getLocation();
    }

    public GPSTracker(Context context, long minDistance, long minUpdateTime) {
        this.mContext = context;
        MIN_DISTANCE_CHANGE_FOR_UPDATES = minDistance;
	    MIN_TIME_BW_UPDATES = minUpdateTime;
	    getLocation();
    }
 
    public Location getLocation() {
        try {
        	
        	 Criteria crta = new Criteria(); 
 	 		crta.setAccuracy(Criteria.ACCURACY_FINE); 
 	 		crta.setAltitudeRequired(true); 
 	 		crta.setBearingRequired(true); 
 	 		crta.setCostAllowed(true); 
 	 		crta.setPowerRequirement(Criteria.NO_REQUIREMENT); 
 	 		
            locationManager = (LocationManager) mContext
                    .getSystemService(LOCATION_SERVICE);
           String provider = locationManager.getBestProvider(crta, true); 
            // getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);
 
            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
 
            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {
                this.canGetLocation = true;
                // First get location from Network Provider
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d("Network", "Network");
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
               //     if (location == null) {
                        locationManager.requestLocationUpdates(
                                provider,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        Log.d("GPS Enabled", "GPS Enabled");
                        if (locationManager != null) {
                          	location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                         	
                            if (location != null) {
                            	latitude = location.getLatitude();
                            	longitude = location.getLongitude();
                            }
                          
                        }
                    }
               // }
            }
 
        } catch (Exception e) {
            e.printStackTrace();
        }
        //test
        if (location==null) {
        	Log.d("GPSTracker","location is null");
        }
        else {
        	String sTestOutput = String.format("latitude = %.6f, longitude = %.6f", latitude,longitude);
        	Log.d("GPSTracker",sTestOutput);
        }
        //end test	
        return location;
    }
     
    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     * */
    public void stopUsingGPS(){
        if(locationManager != null){
            locationManager.removeUpdates(GPSTracker.this);
        }       
    }
     
    /**
     * Function to get latitude
     * */
    public double getLatitude(){
        if(location != null){
            latitude = location.getLatitude();
        }
         
        // return latitude
        return latitude;
    }
     
    /**
     * Function to get longitude
     * */
    public double getLongitude(){
        if(location != null){
            longitude = location.getLongitude();
        }
         
        // return longitude
        return longitude;
    }
     
    /**
     * Function to check GPS/wifi enabled
     * @return boolean
     * */
    public boolean canGetLocation() {
        return this.canGetLocation;
    }
     


    @Override
    public void onLocationChanged(Location location) {
     	
    }
 
    @Override
    public void onProviderDisabled(String provider) {
    }
 
    @Override
    public void onProviderEnabled(String provider) {
    }
 
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
 
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}
