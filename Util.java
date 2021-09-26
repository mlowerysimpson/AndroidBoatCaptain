package com.example.boatcaptain;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.esri.arcgisruntime.geometry.CoordinateFormatter;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;

public class Util {
	Util() {
		
	}
	public static byte[] toByteArray(float value) {
		byte []bytes = new byte[4];
		byte []tempBytes = new byte[4];
		ByteBuffer.wrap(tempBytes).putFloat(value);
		//get reversed byte order
		bytes[0]=tempBytes[3];
		bytes[1]=tempBytes[2];
		bytes[2]=tempBytes[1];
		bytes[3]=tempBytes[0];
		return bytes;
	}
	
	public static byte[] toByteArray(double value) {
	    byte[] bytes = new byte[8];
	    ByteBuffer.wrap(bytes).putDouble(value);
	    return bytes;
	}
	
	public static byte[] toByteArray(int value) {
		byte[] bytes = new byte[4];
		ByteBuffer.wrap(bytes).putInt(value);
		return bytes;
	}

	public static double toDouble(byte[] bytes) {
		//need to reverse byte order 
		byte []tempBytes = new byte[8];
		for (int i=0;i<8;i++) {
			tempBytes[i]=bytes[7-i];
		}
	    return ByteBuffer.wrap(tempBytes).getDouble();
	}
	
	public static long toLong(byte[] bytes) {
		//need to reverse byte order 
		byte []tempBytes = new byte[8];
		for (int i=0;i<8;i++) {
			tempBytes[i]=bytes[7-i];
		}
		return ByteBuffer.wrap(tempBytes).getLong();
	}
	
	public static int toInt(byte[] bytes) {
		//need to reverse byte order
		byte []tempBytes = new byte[4];
		for (int i=0;i<4;i++) {
			tempBytes[i]=bytes[3-i];
		}
		return ByteBuffer.wrap(tempBytes).getInt();
	}
	
	public static void hideSoftKeyboard(Activity activity) {
	    InputMethodManager inputMethodManager = 
	        (InputMethodManager) activity.getSystemService(
	            Activity.INPUT_METHOD_SERVICE);
	    View currentView = activity.getCurrentFocus();
	    if (currentView!=null) {
			inputMethodManager.hideSoftInputFromWindow(
					currentView.getWindowToken(), 0);
		}
	}
	
	public static void FlushInputBuffer(InputStream inStream) {
		try {
			int nNumBytesAvailable = inStream.available();
			while (nNumBytesAvailable>0) {
				byte []inBuf = new byte[nNumBytesAvailable];
				inStream.read(inBuf);
				nNumBytesAvailable = inStream.available();
			}
		}			
		catch (Exception e) {
			//do nothing
		}
		
	}
	
	public static float toFloat(byte[] bytes) {
		//need to reverse byte order 
		byte []tempBytes = new byte[4];
		for (int i=0;i<4;i++) {
			tempBytes[i]=bytes[3-i];
		}
		return ByteBuffer.wrap(tempBytes).getFloat();
	}
	
	public static byte[] reverseByteOrder(byte []inputBytes, int nNumToReverse) {//reverses ordering of bytes, useful sometimes when sending data over network
		byte []outputBytes = new byte[nNumToReverse];
		for (int i=0;i<nNumToReverse;i++) {
			outputBytes[i] = inputBytes[nNumToReverse-i-1];
		}
		return outputBytes;
	}

	public static void PopupMsg(Context cText, int nStringID) {//show a little popup toast message
		Toast toast = Toast.makeText(cText, nStringID, Toast.LENGTH_LONG);
		toast.show();
	}

	public static void PopupMsg(Context cText, String sMsg) {//show a little popup toast message
		Toast toast = Toast.makeText(cText, sMsg,Toast.LENGTH_LONG);
		toast.show();
	}

	/**
	 * Append an array of bytes to an existing array of bytes.
	 * @param bytesToAppend An array of bytes that is to be added on to the end of the original array of bytes.
	 * @param originalBytes The original array of bytes. Can be null, in which case the returned value is just bytesToAppend.
	 * @return a concatenated array of bytes formed from combining originalBytes, followed by bytesToAppend.
	 */
	public static byte [] appendBytes(byte [] bytesToAppend, byte [] originalBytes) {
		if (originalBytes==null) {
			return bytesToAppend;
		}
		int nNumOriginal = originalBytes.length;
		int nNumBytesToAppend = 0;
		if (bytesToAppend!=null) {
			nNumBytesToAppend = bytesToAppend.length;
		}
		int nNewTotal = nNumBytesToAppend + nNumOriginal;
		byte []retval = new byte[nNewTotal];
		for (int i=0; i<nNumOriginal; i++) {
			retval[i] = originalBytes[i];
		}
		for (int i=0; i<nNumBytesToAppend; i++) {
			retval[i+nNumOriginal] = bytesToAppend[i];
		}
		return retval;
	}

	/**
	 * Convert latitude and longitude in degrees (WGS84 format) to web mercator format
	 * @param dLatitudeDeg the latitude measurement in degrees (-90 to +90)
	 * @param dLongitudeDeg the longitude measurement in degrees (-180 to +180)
	 * @return a Point of the location using web mercator coordinates
	 */
	public static Point WGS84ToWebMercator(double dLatitudeDeg, double dLongitudeDeg) {
		String sLatLong = ReformatLatLong(dLatitudeDeg, dLongitudeDeg);
		Point pt = CoordinateFormatter.fromLatitudeLongitude(sLatLong, SpatialReferences.getWebMercator());
		return pt;
	}

	/**
	 * Convert latitude and longitude in degrees (WGS84 format) to regular format
	 * @param dLatitudeDeg the latitude measurement in degrees (-90 to +90)
	 * @param dLongitudeDeg the longitude measurement in degrees (-180 to +180)
	 * @param sr the spatial reference to use for the conversion
	 * @return a Point of the location using regular default coordinates
	 */
	public static Point WGS84ToSR(double dLatitudeDeg, double dLongitudeDeg, SpatialReference sr) {
		String sLatLong = ReformatLatLong(dLatitudeDeg, dLongitudeDeg);
		Point pt = CoordinateFormatter.fromLatitudeLongitude(sLatLong, sr);
		return pt;
	}

	/**
	 * Takes latitude (-90 to +90) and longitude (-180 to 180) as inputs and formats them as a string in the expected ArcGIS format
	 * @param dLat the latitude measurement in degrees (-90 to +90)
	 * @param dLong the longitude measurement in degrees (-180 to +180)
	 * @return a string of the latitude, longitude measurement in the expected ArcGIS format
	 */
	public static String ReformatLatLong(double dLat, double dLong)
	{//
		boolean bEast = false, bNorth = false;
		if (dLong>=0.0)
		{
			bEast = true;
		}
		if (dLat>=0.0)
		{
			bNorth = true;
		}
		double dAbsLatitude = Math.abs(dLat);//absolute value of latitude in degrees
		double dAbsLongitude = Math.abs(dLong);//absolute value of longitude in degrees
		String sLatPart = "";//latitude part of output string
		if (bNorth)
		{
			sLatPart = String.format("%.6fN", dAbsLatitude);
		}
		else
		{
			sLatPart = String.format("%.6fS", dAbsLatitude);
		}
		String sLongPart = "";//longitude part of output string
		if (bEast)
		{
			sLongPart = String.format("%.6fE", dAbsLongitude);
		}
		else
		{
			sLongPart = String.format("%.6fW", dAbsLongitude);
		}
		String sOutputVal = String.format("%s %s", sLatPart, sLongPart);
		return sOutputVal;
	}

	/**
	 * Rotate a MapPoint point by the angle dAngleDeg about the vertical vector
	 * @param pt a point on the map that is to be rotated
	 * @param dAngleDeg the angle in degrees about which to rotate the point pt about the vertical vector passing through the origin (0,0)
	 * @return the rotated point
	 */
	public static Point RotatePt(Point pt, double dAngleDeg)
	{
		//rotate a MapPoint point by the angle dAngleDeg about the vertical vector
		//pt = a point on the map that is to be rotated
		//dAngleDeg = the angle in degrees about which to rotate the point pt about the vertical vector passing through the origin (0,0)
		double dAngleRad = dAngleDeg * Math.PI / 180;
		double dNewX = Math.cos(dAngleRad) * pt.getX() + Math.sin(dAngleRad) * pt.getY();
		double dNewY = -Math.sin(dAngleRad) * pt.getX() + Math.cos(dAngleRad) * pt.getY();
		Point newPt = new Point(dNewX, dNewY, pt.getSpatialReference());
		return newPt;
	}

	/**
	 * Split an input string into a List of strings
	 * @param sStringToSplit the string to be split up into a list of smaller strings
	 * @param sDelimiters the string delimiters to use for splitting up the string
	 * @return a List of strings
	 */
	public static List<String> SplitStrToList(String sStringToSplit, String sDelimiters) {
		String [] splitVals = sStringToSplit.split(sDelimiters);
		List <String> retval = Arrays.asList(splitVals);
		int nNumVals = retval.size();
		//remove empty entries (if any)
		for (int i=(nNumVals-1);i>=0;i--) {
			if (retval.get(i).length()<=0) {
				retval.remove(i);
			}
		}
		return retval;
	}

	/**
	 * Check tp see if a file path corresponds to a common image file type
	 * @param sFilename the file path to check
	 * @return true if sFilename corresponds to a common image file type, otherwise false
	 */
	public static boolean IsImageFile(String sFilename) {
		if (sFilename==null) {
			return false;
		}
		String sLowerCasePath = sFilename.toLowerCase();
		int nStrLength = sLowerCasePath.length();
		if (nStrLength<4) {
			return false;
		}
		String sExt = sLowerCasePath.substring(nStrLength-4);
		if (sExt.equals(".jpg")||sExt.equals(".png")||sExt.equals(".bmp")) {
			return true;
		}
		return false;
	}

}
