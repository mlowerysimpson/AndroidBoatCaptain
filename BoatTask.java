package com.example.boatcaptain;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import android.os.AsyncTask;

public class BoatTask extends AsyncTask<NetCaptain, Integer, Integer> {
	public static int NETWORK_COMMAND_TASK = 2;
	public static int CONNECT_TASK = 1;
	public static int COMPLETED_OK = 0;
	public static int UNKNOWN_TASK = -1;
	public static int ERROR_CONNECTING = -2;
	public static int ERROR_SETTING_TIMEOUT =-3;
	public static int ERROR_COMMAND_FAILED = -4;
	public static int ERROR_SENDING_PASSCODE = -5;
	private int m_nTask;
	BoatTask(int nTask) {
		m_nTask = nTask;
	}
	
	private Integer Connect(NetCaptain netcaptain) {//try to connect to the boat over the network, return 0 if successful, < 0 otherwise
		netcaptain.m_connectedSock = null;
	    // Resolve the server address and port
		int nRemotePortNum = NetCaptain.REMOTE_PORTNUM;
		try {
			netcaptain.m_connectedSock = new Socket(netcaptain.m_sBoatIPAddr, nRemotePortNum);
		}
		catch (Exception e) {
			netcaptain.m_sLastError = String.format("Error trying to connect to boat at %s: %s",netcaptain.m_sBoatIPAddr,e.toString());
			return ERROR_CONNECTING;
		}
		//set receive timeout and no delay 
		try {
			netcaptain.m_connectedSock.setSoTimeout(NetCaptain.RECEIVE_TIMEOUT);
			netcaptain.m_connectedSock.setTcpNoDelay(true);
		}
		catch (Exception e) {
			netcaptain.m_sLastError = String.format("Error trying to set configuration of socket: %s", e.toString());
			return ERROR_SETTING_TIMEOUT;
		}
		//send passcode
		try {
			OutputStream outStream = netcaptain.m_connectedSock.getOutputStream();
			byte []passcodeBytes = NetCaptain.PASSCODE_TEXT.getBytes();
			outStream.write(passcodeBytes);
		}
		catch (Exception e) {
			netcaptain.m_sLastError = "Error trying to send passcode: "+e.toString();
			netcaptain.m_bConnected=false;
			return ERROR_SENDING_PASSCODE; 
		}
		netcaptain.m_bConnected=true;
		return COMPLETED_OK;	
	}
	
	protected Integer doInBackground(NetCaptain... netCaptain) {
		int iResult;
		if (m_nTask==CONNECT_TASK) {
			return Connect(netCaptain[0]);
		}
		else if (m_nTask==NETWORK_COMMAND_TASK) {
			if (netCaptain[0].SendNetworkCommand(netCaptain[0].m_rc)) {
				return COMPLETED_OK;
			}
			else return ERROR_COMMAND_FAILED;
		}
		return UNKNOWN_TASK;
    }

    protected void onProgressUpdate(Integer... progress) {

    }

    protected void onPostExecute(Integer result) {
  		((MainActivity)MainActivity.m_context).BoatTaskCompleted(m_nTask,result);
    }
}



