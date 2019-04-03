package com.example.boatcaptain;

import java.io.OutputStream;
import java.net.Socket;

public class BoatCommand {
	
	BoatCommand() {
		
	}
	
	public static BOAT_DATA CreateBoatData(int nDataType) {//create an empty BOAT_DATA structure for a given data type. 
		BOAT_DATA pAMOSData = new BOAT_DATA();
		if (nDataType==REMOTE_COMMAND.GPS_DATA_PACKET) {
			pAMOSData.nPacketType = REMOTE_COMMAND.GPS_DATA_PACKET;
			pAMOSData.nDataSize = GPS_DATA.dataSize();
			pAMOSData.dataBytes = new byte[pAMOSData.nDataSize];
			pAMOSData.checkSum = CalculateChecksum(pAMOSData);//checksum needs to be recalculated whenever pAMOSData changes
		}
		else if (nDataType==REMOTE_COMMAND.COMPASS_DATA_PACKET) {
			pAMOSData.nPacketType = REMOTE_COMMAND.COMPASS_DATA_PACKET;
			pAMOSData.nDataSize = IMU_DATASAMPLE.dataSize();
			pAMOSData.dataBytes = new byte[pAMOSData.nDataSize];
			pAMOSData.checkSum = CalculateChecksum(pAMOSData);//checksum needs to be recalculated whenever pAMOSData changes
		}
		else if (nDataType==REMOTE_COMMAND.BATTVOLTAGE_DATA_PACKET) {
			pAMOSData.nPacketType = REMOTE_COMMAND.BATTVOLTAGE_DATA_PACKET;
			pAMOSData.nDataSize = 4;
			pAMOSData.dataBytes = new byte[pAMOSData.nDataSize];
			pAMOSData.checkSum = CalculateChecksum(pAMOSData);//checksum needs to be recalculated whenever pAMOSData changes
		}
		else if (nDataType==REMOTE_COMMAND.SUPPORTED_SENSOR_DATA) {
			pAMOSData.nPacketType = REMOTE_COMMAND.SUPPORTED_SENSOR_DATA;
			pAMOSData.nDataSize = 4;//just getting the number of sensors first, will then get a second packet of info later that corresponds to the actual sensor types
			pAMOSData.dataBytes = new byte[pAMOSData.nDataSize];
			pAMOSData.checkSum = CalculateChecksum(pAMOSData);//checksum needs to be recalculated whenever pAMOSData changes
		}
		else if (nDataType==REMOTE_COMMAND.SENSOR_TYPES_INFO) {
			pAMOSData.nPacketType = REMOTE_COMMAND.SENSOR_TYPES_INFO;
			pAMOSData.nDataSize = 0;//needs to be assigned later
			pAMOSData.dataBytes = null;//needs to be assigned later
			pAMOSData.checkSum = CalculateChecksum(pAMOSData);//needs to be assigned later
		}
		else if (nDataType==REMOTE_COMMAND.WATER_TEMP_DATA_PACKET) {
			pAMOSData.nPacketType = REMOTE_COMMAND.WATER_TEMP_DATA_PACKET;
			pAMOSData.nDataSize = 4;
			pAMOSData.dataBytes = new byte[pAMOSData.nDataSize];
			pAMOSData.checkSum = CalculateChecksum(pAMOSData);//checksum needs to be recalculated whenever pAMOSData changes
		}
		else if (nDataType==REMOTE_COMMAND.WATER_PH_DATA_PACKET) {
			pAMOSData.nPacketType = REMOTE_COMMAND.WATER_PH_DATA_PACKET;
			pAMOSData.nDataSize = 4;
			pAMOSData.dataBytes = new byte[pAMOSData.nDataSize];
			pAMOSData.checkSum = CalculateChecksum(pAMOSData);//checksum needs to be recalculated whenever pAMOSData changes
		}
		else if (nDataType==REMOTE_COMMAND.WATER_TURBIDITY_DATA_PACKET) {
			pAMOSData.nPacketType = REMOTE_COMMAND.WATER_TURBIDITY_DATA_PACKET;
			pAMOSData.nDataSize = 4;
			pAMOSData.dataBytes = new byte[pAMOSData.nDataSize];
			pAMOSData.checkSum = CalculateChecksum(pAMOSData);//checksum needs to be recalculated whenever pAMOSData changes
		}
		else if (nDataType==REMOTE_COMMAND.VIDEO_DATA_PACKET) {
			pAMOSData.nPacketType = REMOTE_COMMAND.VIDEO_DATA_PACKET;
			pAMOSData.nDataSize = 4;
			pAMOSData.dataBytes = new byte[pAMOSData.nDataSize];
			pAMOSData.checkSum = CalculateChecksum(pAMOSData);//checksum needs to be recalculated whenever pAMOSData changes
		}
		else if (nDataType==REMOTE_COMMAND.LEAK_DATA_PACKET) {
			pAMOSData.nPacketType = REMOTE_COMMAND.LEAK_DATA_PACKET;
			pAMOSData.nDataSize = 4;
			pAMOSData.dataBytes = new byte[pAMOSData.nDataSize];
			pAMOSData.checkSum = CalculateChecksum(pAMOSData);//checksum needs to be recalculated
		}
		else if (nDataType==REMOTE_COMMAND.DIAGNOSTICS_DATA_PACKET) {
			pAMOSData.nPacketType = REMOTE_COMMAND.DIAGNOSTICS_DATA_PACKET;
			pAMOSData.nDataSize = 24;
			pAMOSData.dataBytes = new byte[pAMOSData.nDataSize];
			pAMOSData.checkSum = CalculateChecksum(pAMOSData);//checksum needs to be recalculated
		}
		return pAMOSData;		
	}
	
	//SendBoatData: send boat data out over socket connection
	//socket: connected socket where the data gets sent
	//boatData: object containing the data that is being sent
	public static boolean SendBoatData(Socket socket, BOAT_DATA boatData) {//sends boat data out over socket connection
		//first send the data packet type and data size so the remote server knows what type of data (and how much) to receive
		int nNumToSend = 8;
		byte []outputBuf = new byte[nNumToSend];
		byte []packetTypeBytes = Util.toByteArray(boatData.nPacketType);
		byte []dataSizeBytes = Util.toByteArray(boatData.nDataSize);
		for (int i=0;i<4;i++) {
			outputBuf[i] = packetTypeBytes[i];
			outputBuf[4+i] = dataSizeBytes[i];
		}
		OutputStream outStream = null;
		try {
			outStream = socket.getOutputStream();
		}
		catch (Exception e) {//unable to get output stream
			return false;
		}
		try {
			outStream.write(outputBuf);
		}
		catch (Exception e) {
			return false;
		}
		//now send the actual data and the checksum byte
		nNumToSend = boatData.nDataSize + 1;
		outputBuf = new byte[nNumToSend];
		for (int i=0;i<boatData.nDataSize;i++) {
			outputBuf[i] = boatData.dataBytes[i];
		}
		outputBuf[boatData.nDataSize-1] = boatData.checkSum;
		try {
			outStream.write(outputBuf);
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public static byte CalculateChecksum(BOAT_DATA pData) {//calculate simple 8-bit checksum for BOAT_DATA structure
		byte checksum = 0;
		byte []pBytes = pData.getBytes();
		int nNumToCheck = 8 + pData.nDataSize;
		for (int i=0;i<nNumToCheck;i++) {
			checksum+=pBytes[i];
		}
		return checksum;
	}

}
