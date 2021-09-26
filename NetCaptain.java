package com.example.boatcaptain;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class NetCaptain extends Captain {
	public static String PASSCODE_TEXT = "AMOS2018";
	public static int REMOTE_PORTNUM = 81;// the remote port number of the
											// boat that is used for
											// communications
	public static int CHUNKSIZE = 10000;//size of chunk to try receiving over network (eg. when downloading image)
	public static int RECEIVE_TIMEOUT = 10000;// timeout for receiving network
												// data in milliseconds
	public Socket m_connectedSock;// socket connection to the remote boat server
	public String m_sBoatIPAddr;// the IP address of the boat that we are trying
								// to connect to
	private boolean m_bCleanedUp;// flag is true after socket connections have
									// been properly closed and cleaned up
	public boolean m_bConnected;// flag is true if we are connected to the
								// server on the boat
	private String m_sBoatImageFilename;//file path of image received from boat
	public REMOTE_COMMAND m_rc;// the current remote command to execute

	NetCaptain() {
		m_connectedSock = null;
		m_bCleanedUp = false;
		m_sBoatIPAddr = "";
		m_sLastError = "";
		m_bConnected = false;
	}

	public void ConnectToBoat(String sIPAddr) {
		this.m_sBoatIPAddr = sIPAddr;
		new BoatTask(BoatTask.CONNECT_TASK).execute(this);
	}

	public PROPELLER_STATE ForwardHo() {// accelerate forward
		PROPELLER_STATE propState = super.ForwardHo();
		if (propState == null) {
			return null;
		}
		m_rc = new REMOTE_COMMAND();
		m_rc.nCommand = REMOTE_COMMAND.THRUST_ON;
		m_rc.nNumDataBytes = PROPELLER_STATE.dataSize();
		m_rc.pDataBytes = propState.getBytes();
		new BoatTask(BoatTask.NETWORK_COMMAND_TASK).execute(this);
		return propState;
	}

	public PROPELLER_STATE StarboardHo() {// turn to right at default speed
		PROPELLER_STATE propState = super.StarboardHo();
		if (propState == null) {
			return null;
		}
		m_rc = new REMOTE_COMMAND();
		m_rc.nCommand = REMOTE_COMMAND.THRUST_ON;
		m_rc.nNumDataBytes = PROPELLER_STATE.dataSize();
		m_rc.pDataBytes = propState.getBytes();
		new BoatTask(BoatTask.NETWORK_COMMAND_TASK).execute(this);
		return propState;
	}

	public PROPELLER_STATE PortHo() {// turn ot left at default speed
		PROPELLER_STATE propState = super.PortHo();
		if (propState == null) {
			return null;
		}
		m_rc = new REMOTE_COMMAND();
		m_rc.nCommand = REMOTE_COMMAND.THRUST_ON;
		m_rc.nNumDataBytes = PROPELLER_STATE.dataSize();
		m_rc.pDataBytes = propState.getBytes();
		new BoatTask(BoatTask.NETWORK_COMMAND_TASK).execute(this);
		return propState;
	}

	public PROPELLER_STATE BackHo() {// reverse thrusters (move backwards) at
										// default speed
		PROPELLER_STATE propState = super.BackHo();
		if (propState == null) {
			return null;
		}
		m_rc = new REMOTE_COMMAND();
		m_rc.nCommand = REMOTE_COMMAND.THRUST_ON;
		m_rc.nNumDataBytes = PROPELLER_STATE.dataSize();
		m_rc.pDataBytes = propState.getBytes();
		new BoatTask(BoatTask.NETWORK_COMMAND_TASK).execute(this);
		return propState;
	}

	public PROPELLER_STATE Stop() {// stop thrusters
		PROPELLER_STATE propState = super.Stop();
		if (propState == null) {
			return null;
		}
		m_rc = new REMOTE_COMMAND();
		m_rc.nCommand = REMOTE_COMMAND.THRUST_ON;
		m_rc.nNumDataBytes = PROPELLER_STATE.dataSize();
		m_rc.pDataBytes = propState.getBytes();
		new BoatTask(BoatTask.NETWORK_COMMAND_TASK).execute(this);
		return propState;
	}

	public String GetIPAddr() {// return the IP address of the remote boat that
								// we are connected to
		return m_sBoatIPAddr;
	}

	public String RequestGPSPosition() {// query the boat for its GPS position
										// and return a string corresponding to
										// that GPS data
		String sGPSPosition = "";
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.GPS_DATA_PACKET;
		rc.nNumDataBytes = 0;
		rc.pDataBytes = null;
		if (SendNetworkCommand(rc)) {
			if (!ReceiveBoatData()) {
				return null;
			}
		} else {
			return null;
		}
		if (m_gpsTime == 0) {// no GPS readings obtained yet
			return null;
		}
		sGPSPosition = FormatGPSData();
		return sGPSPosition;
	}

	public String RequestCompassData() {// query the boat for its compass data
										// (heading, roll, and pitch angles, as
										// well as temperature). Returns true if
										// successful
		String sCompassData = "";
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.COMPASS_DATA_PACKET;
		rc.nNumDataBytes = 0;
		rc.pDataBytes = null;
		if (SendNetworkCommand(rc)) {
			if (!ReceiveBoatData()) {
				return null;
			}
		}
		else {
			return null;
		}
		sCompassData = FormatCompassData();
		return sCompassData;
	}

	/**
	 * Receive data from the boat over the network connection
	 * @return true if data was received successfully from the boat, otherwise returns false. When requesting video or image data, note that this function just returns acknowledgement that the request
	 * for an image was received, it does not return the actual image data. The m_nNumLargeBlockBytes variable contains the size of the image that is available for download from AMOS. The image must be downloaded
	 * using the ReceiveDataChunk function.
	 */
	public boolean ReceiveBoatData() {// receive data from boat over network
		if (m_connectedSock == null) {
			return false;// not connected yet
		}
		int nNumBytesToReceive = 8;
		// first receive the data type from the boat and the number of data
		// bytes
		int nDataType = 0;
		int nDataSize = 0;
		byte[] inBuf = new byte[nNumBytesToReceive];
		InputStream inputStream = null;
		try {
			inputStream = m_connectedSock.getInputStream();
		} catch (Exception e) {
			m_sLastError = "Error, unable to get input stream for receving boat data: "
					+ e.toString();
			return false;
		}
		int nNumReceived = 0;
		try {
			nNumReceived = inputStream.read(inBuf, 0, nNumBytesToReceive);
		} catch (Exception e) {
			m_sLastError = "Error trying to receive boat data: "
					+ e.toString();
			return false;
		}
		byte[] dataTypeBytes = new byte[4];
		byte[] dataSizeBytes = new byte[4];
		for (int i = 0; i < 4; i++) {
			dataTypeBytes[i] = inBuf[i];
			dataSizeBytes[i] = inBuf[4 + i];
		}
		nDataType = Util.toInt(dataTypeBytes);
		nDataSize = Util.toInt(dataSizeBytes);
		// create structure for receiving boat data
		BOAT_DATA pBoatData = BoatCommand.CreateBoatData(nDataType);
		if (pBoatData == null) {
			return false;// unable to create this data type
		}
		if (pBoatData.nDataSize != nDataSize) {
			return false;// unexpected data size
		}
		nNumBytesToReceive = pBoatData.nDataSize + 1;
		inBuf = new byte[nNumBytesToReceive];
		try {
			nNumReceived = inputStream.read(inBuf, 0, nNumBytesToReceive);
		} catch (Exception e) {
			m_sLastError = "Error trying to receive data bytes: "
					+ e.toString();
			return false;
		}
		for (int i = 0; i < pBoatData.nDataSize; i++) {
			pBoatData.dataBytes[i] = inBuf[i];
		}
		pBoatData.checkSum = inBuf[nNumBytesToReceive - 1];
		//special case for video capture frame
		boolean bSkipProcessData=false;
		if (pBoatData.nPacketType==REMOTE_COMMAND.VIDEO_DATA_PACKET) {
			bSkipProcessData=true;
			m_nNumLargeBlockBytes = 0;
			m_nNumLargeBlockBytes = Util.toInt(pBoatData.dataBytes);
			//make sure image is not super large
			if (m_nNumLargeBlockBytes>=10000000) {
				return false;//something wrong, image file should not be this big!
			}
		}
		else if (pBoatData.nPacketType == REMOTE_COMMAND.LIST_REMOTE_DATA) {
			bSkipProcessData = true;
			m_nNumLargeBlockBytes = 0;
			m_nNumLargeBlockBytes = Util.toInt(pBoatData.dataBytes);
			//make sure number of bytes is not too large
			if (m_nNumLargeBlockBytes >= 100000) {
				return false;//something wrong, list of remote data files should not be this long!
			}
		}
		else if (pBoatData.nPacketType == REMOTE_COMMAND.LIST_REMOTE_LOG) {
			bSkipProcessData = true;
			m_nNumLargeBlockBytes = 0;
			m_nNumLargeBlockBytes = Util.toInt(pBoatData.dataBytes);
			//make sure number of bytes is not too large
			if (m_nNumLargeBlockBytes >= 100000) {
				return false;//something wrong, list of remote log files should not be this long!
			}
		}
		else if (pBoatData.nPacketType == REMOTE_COMMAND.LIST_REMOTE_IMAGE) {
			bSkipProcessData = true;
			m_nNumLargeBlockBytes = 0;
			m_nNumLargeBlockBytes = Util.toInt(pBoatData.dataBytes);
			//make sure number of bytes is not too large
			if (m_nNumLargeBlockBytes >= 100000) {
				return false;//something wrong, list of remote image files should not be this long!
			}
		}
		else if (pBoatData.nPacketType == REMOTE_COMMAND.FILE_RECEIVE) {
			bSkipProcessData = true;
			m_nNumLargeBlockBytes = 0;
			m_nNumLargeBlockBytes = Util.toInt(pBoatData.dataBytes);
			//make sure number of bytes is not too large
			if (m_nNumLargeBlockBytes >= 10000000) {
				return false;//something wrong, remote file larger than this would take forever over BLE
			}
		}
		else if (pBoatData.nPacketType == REMOTE_COMMAND.DELETE_FILES) {
			bSkipProcessData = true;
			m_nNumLargeBlockBytes = 0;
			m_nNumLargeBlockBytes = Util.toInt(pBoatData.dataBytes);
			//make sure number of bytes is not too large
			if (m_nNumLargeBlockBytes >= 2000000) {
				return false;
			}
		}
		if (!bSkipProcessData&&!ProcessBoatData(pBoatData)) {
			return false;
		}
		return true;// command was sent successfully
	}

	private boolean cleanup() {// close socket
		if (m_connectedSock != null) {
			try {
				m_connectedSock.close();
			} catch (Exception e) {
				m_sLastError = "Error trying to close socket: " + e.toString();
				return false;
			}
			m_connectedSock = null;
			return true;
		}
		m_bCleanedUp = true;
		return true;
	}

	// SendNetworkCommand: sends a remote command out over the network
	// pRC = REMOTE_COMMAND object describing the command to be sent to the boat
	// returns true if command was successfully sent, false otherwise
	public boolean SendNetworkCommand(REMOTE_COMMAND pRC) {// sends a remote
															// command out over
															// the network
		if (m_connectedSock == null) {
			return false;// not connected yet
		}
		// first send command bytes
		byte[] commandBytes = new byte[4];
		commandBytes[0] = (byte) ((pRC.nCommand & 0xff000000) >> 24);
		commandBytes[1] = (byte) ((pRC.nCommand & 0x00ff0000) >> 16);
		commandBytes[2] = (byte) ((pRC.nCommand & 0x0000ff00) >> 8);
		commandBytes[3] = (byte) (pRC.nCommand & 0x000000ff);

		OutputStream outStream = null;
		InputStream inStream = null;
		try {
			outStream = m_connectedSock.getOutputStream();
		}
		catch (IOException e) {
			m_sLastError = e.toString() + ", unable to get output stream.";
			return false;
		}
		try {
			inStream = m_connectedSock.getInputStream();
		}
		catch (IOException e) {
			m_sLastError = "Error, unable to get input stream: " + e.toString();
			return false;
		}
		//flush input buffer
		Util.FlushInputBuffer(inStream);
		try {
			outStream.write(commandBytes);
		} catch (IOException e) {
			m_sLastError = "Error writing command bytes to boat: "
					+ e.toString();
			return false;
		}

		if (pRC.pDataBytes != null) {// need to send data along with command
			byte[] dataSizeBytes = new byte[4];
			dataSizeBytes[0] = (byte) ((pRC.nNumDataBytes & 0xff000000) >> 24);
			dataSizeBytes[1] = (byte) ((pRC.nNumDataBytes & 0x00ff0000) >> 16);
			dataSizeBytes[2] = (byte) ((pRC.nNumDataBytes & 0x0000ff00) >> 8);
			dataSizeBytes[3] = (byte) (pRC.nNumDataBytes & 0x000000ff);
			try {
				outStream.write(dataSizeBytes);
			}
			catch (IOException e) {
				m_sLastError = "Error writing data size bytes to boat: "
						+ e.toString();
				return false;
			}
			//now write the actual data bytes
			try {
				outStream.write(pRC.pDataBytes);
			}
			catch (IOException e) {
				m_sLastError = "Error writing data bytes to boat: "
						+ e.toString();
				return false;
			}
		}
		// get confirmation from remote boat
		byte[] inBuf = new byte[4];
	
		int nNumRead = 0;
		try {
			nNumRead = inStream.read(inBuf, 0, 4);
		} catch (IOException e) {
			String sError = "Error trying to read data: " + e.toString();
			return false;
		}
		int nByte1 = (int) inBuf[0];
		int nByte2 = (int) inBuf[1];
		int nByte3 = (int) inBuf[2];
		int nByte4 = (int) inBuf[3];
		int nResponse = (nByte1 << 24) + (nByte2 << 16) + (nByte3 << 8)
				+ nByte4;
		if (nResponse != pRC.nCommand) {
			m_sLastError = "Error, invalid confirmation response from boat.";
			return false;
		}
		return true;// command was sent successfully
	}

	public boolean Disconnect() {
		return cleanup();
	}
	
	public String RequestVoltageData() {// query the boat for its voltage data
		//Returns text corresponding to the boat's power supply voltage (typ. +12 V) if  successful
		String sVoltageData = "";
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.BATTVOLTAGE_DATA_PACKET;
		rc.nNumDataBytes = 0;
		rc.pDataBytes = null;
		if (SendNetworkCommand(rc)) {
			if (!ReceiveBoatData()) {
				return null;
			}
		}
		else {
			return null;
		}
		sVoltageData = FormatVoltageData();
		return sVoltageData;
	}
	
	public boolean ReturnHome(double dLatitude, double dLongitude) {
		//send command to boat to return home
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.GPS_DESTINATION_PACKET;
		rc.nNumDataBytes = 16;
		rc.pDataBytes = new byte[rc.nNumDataBytes];
		byte []latitude_bytes = Util.toByteArray(dLatitude);
		latitude_bytes = Util.reverseByteOrder(latitude_bytes, 8);//AMOS expects bytes in reverse order
		byte []longitude_bytes = Util.toByteArray(dLongitude);
		longitude_bytes = Util.reverseByteOrder(longitude_bytes, 8);//AMOS expects bytes in reverse order
		for (int i=0;i<8;i++) {
			rc.pDataBytes[i]=latitude_bytes[i];
			rc.pDataBytes[8+i]=longitude_bytes[i];
		}
		if (!SendNetworkCommand(rc)) {
			return false;
		}
		return true;
	}
	
	public boolean CancelHome() {//function sends command to boat to cancel a previously issued homing command
		//send command to boat to return home
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.CANCEL_OPERATION;
		rc.nNumDataBytes = 0;
		rc.pDataBytes = null;
		if (!SendNetworkCommand(rc)) {
			return false;
		}
		return true;
	}
	
	/**
	 * Request an image capture with feature markings in it from the boat
	 * @param nFeatureThreshold  value from 0 to 255 that is used to control how feature markings
	 * 	are determined in the image. nFeatureThreshold==0 disables or removes feature markers from the image.
	 * @return true if the request for an image was sent successfully, otherwise returns false.
	 */
	public boolean RequestVideoImage(int nFeatureThreshold) {
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.VIDEO_DATA_PACKET;
		rc.nNumDataBytes = 4;
		byte []featureBytes = Util.toByteArray(nFeatureThreshold); 
		rc.pDataBytes = Util.reverseByteOrder(featureBytes, 4);
		if (!SendNetworkCommand(rc)) {
			return false;
		}
		return true;
	}
	

	/**
	 * tries to receive a large chunk of data over network socket connection
	 * @param nNumToReceive the number of bytes to receive over the socket connection
	 * @return returns a byte array of size nNumToReceive, or null if there was an error receiving the data
	 */
	public byte [] ReceiveLargeDataChunk(int nNumToReceive) {
		int MAX_ALLOWED_TIMEOUTS = 5;
		int nNumRemaining = nNumToReceive;
		int nNumReceived=0;
		int nTimeoutCount=0;
		byte []rxBytes = new byte[nNumToReceive];
		InputStream inputStream = null;
		try {
			inputStream = m_connectedSock.getInputStream();
		} catch (Exception e) {
			m_sLastError = "Error, unable to get input stream for receiving boat data: "
					+ e.toString();
			return null;
		}
		do {
			int nRX = 0;
			byte []inBuf = new byte[nNumRemaining];
			try {
				nRX = inputStream.read(inBuf, 0, nNumRemaining);
			}
			catch (Exception e) {
				m_sLastError = "Error trying to receive boat data: "
						+ e.toString();
				nTimeoutCount++;
				continue;
			}
			if (nRX>0) {
				for (int i=nNumReceived;i<(nNumReceived+nRX);i++) {
					rxBytes[i] = inBuf[i-nNumReceived];
				}
				nNumReceived+=nRX;
				nNumRemaining-=nRX;
			}
			else {//timeout or error occurred
				nTimeoutCount++;
			}
		} while (nNumRemaining>0&&nTimeoutCount<MAX_ALLOWED_TIMEOUTS);
		if (nNumRemaining==0) {//success
			return rxBytes;
		}
		return null;//failed to get all of the bytes	
	}
	
	public String RequestDiagnosticsData() {// query the boat for its diagnostics data
		//Returns text corresponding to the leak sensor status, and the amount of current drawn by the boat's +12V power supply
		//Request leak sensor data
		String sLeakData = "";
		String sDiagnosticsData = "";
		String sResponse = "";
		boolean bGotLeakData=false;
		boolean bGotDiagnosticsData=false;
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.LEAK_DATA_PACKET;
		rc.nNumDataBytes = 0;
		rc.pDataBytes = null;
		if (SendNetworkCommand(rc)) {
			if (ReceiveBoatData()) {
				bGotLeakData=true;
			}
		}
		if (bGotLeakData) {
			sLeakData = FormatLeakData();
		}
		rc.nCommand = REMOTE_COMMAND.DIAGNOSTICS_DATA_PACKET;
		rc.nNumDataBytes = 0;
		rc.pDataBytes = null;
		if (SendNetworkCommand(rc)) {
			if (ReceiveBoatData()) {
				bGotDiagnosticsData = true;
			}
		}
		if (bGotDiagnosticsData) {
			sDiagnosticsData = FormatDiagnosticsData();
		}
		if (bGotLeakData&&bGotDiagnosticsData) {
			sResponse = String.format("%s, %s",sLeakData,sDiagnosticsData);
		}
		else if (bGotLeakData) {
			sResponse = sLeakData;
		}
		else if (bGotDiagnosticsData) {
			sResponse = sDiagnosticsData;
		}
		return sResponse;
	}

	public List<String> GetRemoteDataFiles() {
		//list all of the remote data files available on AMOS
		String sRemoteDataFiles = "";
		int nGotRemoteDataFilesStatus = -1;
		List <String> retval = null;

		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.LIST_REMOTE_DATA;
		rc.nNumDataBytes = 0;
		rc.pDataBytes = null;
		if (SendNetworkCommand(rc)) {
			if (ReceiveBoatData()) {
				retval = Util.SplitStrToList(m_sRemoteDataAvailable,"\r|\n");
			}
		}
		return retval;
	}

	/**
	 * @brief receive a copy of a remote file on AMOS
	 *
	 * @param sRemoteFilename the path to the remote filename on AMOS that we want to download.
	 * @param sDestPath the local path of the copied file that was downloaded (should get downloaded to the current folder)
	 * @param downloadParams download parameters; downloadParams[0] is the size of the file being downloaded in bytes, downloadParams[1] is updated by this function and corresponds to the number of bytes that have been downloaded
	 * @return 0 if the command was executed successfully, or negative if an error occurred.
	 */
	public int ReceiveFile(String sRemoteFilename, String sDestPath, int []downloadParams) {//receive a remote file on AMOS
		if (sRemoteFilename==null || sDestPath==null) {//invalid parameter
			return -2;
		}
		int nRemoteFilenameLength = sRemoteFilename.length();
		if (nRemoteFilenameLength==0||sDestPath.length()==0) {//invalid parameter
			return -3;
		}

		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.FILE_RECEIVE;
		rc.nNumDataBytes = 4;
		rc.pDataBytes = new byte[4];
		rc.pDataBytes[0] = (byte) ((nRemoteFilenameLength & 0xff000000) >> 24);
		rc.pDataBytes[1] = (byte) ((nRemoteFilenameLength & 0x00ff0000) >> 16);
		rc.pDataBytes[2] = (byte) ((nRemoteFilenameLength & 0x0000ff00) >> 8);
		rc.pDataBytes[3] = (byte) (nRemoteFilenameLength & 0x000000ff);

		int nRetval = 0;
		if (!SendNetworkCommand(rc)) {
			nRetval = -4;
		}
		else {
			//now send a block of data that corresponds to the path of the remote file
			OutputStream outStream = null;
			InputStream inStream = null;
			try {
				outStream = m_connectedSock.getOutputStream();
			}
			catch (IOException e) {
				m_sLastError = e.toString() + ", unable to get output stream.";
				return -5;
			}
			try {
				inStream = m_connectedSock.getInputStream();
			}
			catch (IOException e) {
				m_sLastError = "Error, unable to get input stream: " + e.toString();
				return -6;
			}
			//flush input buffer
			Util.FlushInputBuffer(inStream);
			try {
				outStream.write(sRemoteFilename.getBytes());
			}
			catch (IOException e) {
				m_sLastError = "Error writing command bytes to boat: "
						+ e.toString();
				return -7;
			}
			//try to receive data from boat
			if (!ReceiveFileData(sDestPath, downloadParams)) {
				nRetval = -8;
			}
		}
		return nRetval;
	}

	/**
	 * @brief receive a copy of a remote file on AMOS (assumes that ReceiveFile function has already been called to request the file)
	 *
	 * @param sDestPath the local path of the copied file that was downloaded (should get downloaded to the current folder)
	 * @param downloadParams download parameters; downloadParams[0] is the size of the file being downloaded in bytes, downloadParams[1] is the number of bytes that have been downloaded. Both are updated by this function.
	 * @return true if the file was received successfully
	 */
	private boolean ReceiveFileData(String sDestPath, int [] downloadParams) {
		int nNumBytesToReceive=8;
		InputStream inStream = null;
		try {
			inStream = m_connectedSock.getInputStream();
		}
		catch (IOException e) {
			m_sLastError = "Error, unable to get input stream: " + e.toString();
			return false;
		}
		//first receive the data type from the boat and the number of data bytes
		int nDataType=0;
		int nDataSize=0;

		byte [] inBuf = new byte[nNumBytesToReceive];

		try {
			inStream.read(inBuf);
		}
		catch (IOException e) {
			Util.FlushInputBuffer(inStream);
			m_sLastError = e.toString();
			return false;
		}
		nDataType = Util.toInt(inBuf);
		if (nDataType!=REMOTE_COMMAND.FILE_RECEIVE) {
			m_sLastError = "Wrong data type received from AMOS.";
			return false;
		}
		byte [] dataSizeBytes = new byte[4];
		dataSizeBytes[0] = inBuf[4];
		dataSizeBytes[1] = inBuf[5];
		dataSizeBytes[2] = inBuf[6];
		dataSizeBytes[3] = inBuf[7];
		nDataSize = Util.toInt(dataSizeBytes);

		//create structure for receiving boat data
		BOAT_DATA pBoatData = BoatCommand.CreateBoatData(nDataType);
		if (pBoatData==null) {
			m_sLastError = "Unable to create boat data.";
			return false;//unable to create this data type
		}
		if (pBoatData.nDataSize!=nDataSize) {
			m_sLastError = "Unexpected data size.";
			return false;//unexpected data size
		}
		nNumBytesToReceive = pBoatData.nDataSize+1;
		inBuf = new byte[nNumBytesToReceive];
		try {
			inStream.read(inBuf);
		}
		catch (IOException e) {
			m_sLastError = e.toString();
			Util.FlushInputBuffer(inStream);
			return false;
		}
		//copy data bytes
		for (int i=0;i<pBoatData.nDataSize;i++) {
			pBoatData.dataBytes[i] = inBuf[i];
		}
		pBoatData.checkSum = inBuf[nNumBytesToReceive-1];

		int nNumBytes = Util.toInt(pBoatData.dataBytes);
		//make sure number of bytes is not too large
		if (nNumBytes < 100000000) {
			boolean bRetval = DownloadBytes(inStream, nNumBytes, REMOTE_COMMAND.FILE_RECEIVE, sDestPath, downloadParams);
			return bRetval;
		}
		return false;
	}

	/**
	 * @brief download a largeish number of bytes at once
	 * @param inStream the network stream used for receiving the bytes
	 * @param nNumBytes the expected number of bytes to receive
	 * @param nDataType the type of data that is expected to be received
	 * @param sDestPath the local path where the bytes will be saved (set to null or empty if not saving bytes to file)
	 * @param downloadParams download parameters; downloadParams[0] is the size of the file being downloaded in bytes, downloadParams[1] is the number of bytes that have been downloaded. Both are updated by this function.
	 * @return true if the file was received successfully
	 */
	private boolean DownloadBytes(InputStream inStream, int nNumBytes, int nDataType, String sDestPath, int []downloadParams) {
		boolean bRetval = false;
		downloadParams[0] = nNumBytes;
		if (nDataType == REMOTE_COMMAND.LIST_REMOTE_SCRIPTS) {
			byte [] fileNameBytes = ReceiveLargeDataChunk(nNumBytes);
			if (fileNameBytes.length == nNumBytes) {//bytes received successfully, save to m_sRemoteScriptsAvailable
				byte []nullTermFilenameBytes = new byte[fileNameBytes.length+1];
				nullTermFilenameBytes[fileNameBytes.length] = 0;//null terminate
				//copy bytes
				for (int i=0;i<fileNameBytes.length;i++) {
					nullTermFilenameBytes[i] = fileNameBytes[i];
				}
				m_sRemoteScriptsAvailable = new String(nullTermFilenameBytes);
				bRetval = true;
			}
			else {
				m_sRemoteScriptsAvailable = "";
			}
		}
		else if (nDataType == REMOTE_COMMAND.LIST_REMOTE_DATA) {
			byte [] fileNameBytes = ReceiveLargeDataChunk(nNumBytes);
			if (fileNameBytes.length == nNumBytes) {//bytes received successfully, save to m_sRemoteDataAvailable
				byte []nullTermFilenameBytes = new byte[fileNameBytes.length+1];
				nullTermFilenameBytes[fileNameBytes.length] = 0;//null terminate
				//copy bytes
				for (int i=0;i<fileNameBytes.length;i++) {
					nullTermFilenameBytes[i] = fileNameBytes[i];
				}
				m_sRemoteDataAvailable = new String(nullTermFilenameBytes);
				bRetval = true;
			}
			else {
				m_sRemoteDataAvailable = "";
			}
		}
		else if (nDataType == REMOTE_COMMAND.LIST_REMOTE_LOG) {
			byte [] fileNameBytes = ReceiveLargeDataChunk(nNumBytes);
			if (fileNameBytes.length == nNumBytes) {//bytes received successfully, save to m_sRemoteLogsAvailable
				byte []nullTermFilenameBytes = new byte[fileNameBytes.length+1];
				nullTermFilenameBytes[fileNameBytes.length] = 0;//null terminate
				//copy bytes
				for (int i=0;i<fileNameBytes.length;i++) {
					nullTermFilenameBytes[i] = fileNameBytes[i];
				}
				m_sRemoteLogsAvailable = new String(nullTermFilenameBytes);
				bRetval = true;
			}
			else {
				m_sRemoteLogsAvailable = "";
			}
		}
		else if (nDataType == REMOTE_COMMAND.LIST_REMOTE_IMAGE) {
			byte [] fileNameBytes = ReceiveLargeDataChunk(nNumBytes);
			if (fileNameBytes.length == nNumBytes) {//bytes received successfully, save to m_sRemoteImageFilesAvailable
				byte []nullTermFilenameBytes = new byte[fileNameBytes.length+1];
				nullTermFilenameBytes[fileNameBytes.length] = 0;//null terminate
				//copy bytes
				for (int i=0;i<fileNameBytes.length;i++) {
					nullTermFilenameBytes[i] = fileNameBytes[i];
				}
				m_sRemoteRemoteImageFilesAvailable = new String(nullTermFilenameBytes);
				bRetval = true;
			}
			else {
				m_sRemoteRemoteImageFilesAvailable = "";
			}
		}
		else if (nDataType == REMOTE_COMMAND.FILE_RECEIVE) {
			if (sDestPath==null||sDestPath.length()==0) {
				return false;
			}
			byte [] fileBytes =  ReceiveLargeDataChunk(nNumBytes, downloadParams);
			if (fileBytes.length == nNumBytes) {//bytes received successfully, save to sDestPath
				try {
					FileOutputStream f_out = new FileOutputStream(sDestPath);
					f_out.write(fileBytes);
					f_out.close();
				}
				catch (Exception e) {
					m_sLastError = String.format("Error %s trying to save data to file: %s",e.toString(),sDestPath);
					return false;
				}
			}
		}
		else if (nDataType == REMOTE_COMMAND.DELETE_FILES)
		{
			m_sUnDeletedFiles = "";
			byte [] fileBytes = ReceiveLargeDataChunk(nNumBytes);
			if (fileBytes.length == nNumBytes) {//bytes received successfully
				byte []nullTermFileBytes = new byte[fileBytes.length+1];
				nullTermFileBytes[fileBytes.length] = 0;//null terminate
				//copy bytes
				for (int i=0;i<fileBytes.length;i++) {
					nullTermFileBytes[i] = fileBytes[i];
				}
				m_sUnDeletedFiles = new String(nullTermFileBytes);
			}
		}
		return bRetval;
	}


	/**
	 * tries to receive a large chunk of data over network socket connection
	 * @param nNumToReceive the number of bytes to receive over the socket connection
	 * @param downloadParams download parameters; downloadParams[0] is the size of the file being downloaded in bytes, downloadParams[1] is the number of bytes that have been downloaded. Both are updated by this function.
	 * @return returns a byte array of size nNumToReceive, or null if there was an error receiving the data
	 */
	public byte [] ReceiveLargeDataChunk(int nNumToReceive, int []downloadParams) {
		int MAX_ALLOWED_TIMEOUTS = 5;
		int nNumRemaining = nNumToReceive;
		int nNumReceived=0;
		int nTimeoutCount=0;
		downloadParams[0] = nNumToReceive;
		downloadParams[1] = 0;
		byte []rxBytes = new byte[nNumToReceive];
		InputStream inputStream = null;
		try {
			inputStream = m_connectedSock.getInputStream();
		} catch (Exception e) {
			m_sLastError = "Error, unable to get input stream for receiving boat data: "
					+ e.toString();
			return null;
		}
		do {
			int nRX = 0;
			byte []inBuf = new byte[nNumRemaining];
			try {
				nRX = inputStream.read(inBuf, 0, nNumRemaining);
			}
			catch (Exception e) {
				m_sLastError = "Error trying to receive boat data: "
						+ e.toString();
				nTimeoutCount++;
				continue;
			}
			if (nRX>0) {
				for (int i=nNumReceived;i<(nNumReceived+nRX);i++) {
					rxBytes[i] = inBuf[i-nNumReceived];
				}
				nNumReceived+=nRX;
				downloadParams[1]=nNumReceived;
				nNumRemaining-=nRX;
			}
			else {//timeout or error occurred
				nTimeoutCount++;
			}
		} while (nNumRemaining>0&&nTimeoutCount<MAX_ALLOWED_TIMEOUTS);
		if (nNumRemaining==0) {//success
			return rxBytes;
		}
		return null;//failed to get all of the bytes
	}

	/**
	 * makes a request to download a remote filename from AMOS
	 * @param sRemoteFilename the path of the remote filename to download
	 * @return true if the request for the remote file was successfully made, otherwise false
	 */
	public boolean RequestRemoteFile(String sRemoteFilename) {
		REMOTE_COMMAND rc = super.CreateRemoteFileCommand(sRemoteFilename);
		if (rc==null) {
			return false;
		}
		if (!SendNetworkCommand(rc)) {
			return false;
		}
		else {
			//now send a block of data that corresponds to the path of the remote file
			OutputStream outStream = null;
			InputStream inStream = null;
			try {
				outStream = m_connectedSock.getOutputStream();
			}
			catch (IOException e) {
				m_sLastError = e.toString() + ", unable to get output stream.";
				return false;
			}
			try {
				inStream = m_connectedSock.getInputStream();
			}
			catch (IOException e) {
				m_sLastError = "Error, unable to get input stream: " + e.toString();
				return false;
			}
			//flush input buffer
			Util.FlushInputBuffer(inStream);
			try {
				outStream.write(sRemoteFilename.getBytes());
			}
			catch (IOException e) {
				m_sLastError = "Error writing command bytes to boat: "
						+ e.toString();
				return false;
			}
		}
		return true;
	}
}
