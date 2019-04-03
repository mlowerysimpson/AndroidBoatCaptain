package com.example.boatcaptain;

import java.io.InputStream;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

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

}
