package com.example.boatcaptain;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import android.os.Environment;

public class BOAT_DATA {
	public static String TMP_IMAGE_FILENAME = "/tmp_img_output.jpg"; // name of
																	// temporary
																	// image
																	// file for
																	// storing
																	// video
																	// captures
																	// from
																	// remote
																	// boat
	public static String LAST_DATA_ERROR = "";// used for keeping track of any
												// errors

	public int nPacketType;// packet code describing what type of data this is
	public int nDataSize;// number of dataBytes
	public byte[] dataBytes;
	public byte checkSum;// simple 8-bit checksum of everything in this
							// structure, except this checkSum byte

	BOAT_DATA() {
		nPacketType = 0;
		nDataSize = 0;
		dataBytes = null;
		checkSum = 0;
	}

	public byte[] getBytes() {// return everything in this object as a byte
								// array
		int nObjectSize = 2 * 4 + nDataSize + 1;
		byte[] outputBytes = new byte[nObjectSize];
		byte[] packetTypeBytes = Util.toByteArray(nPacketType);
		byte[] dataSizeBytes = Util.toByteArray(nDataSize);
		for (int i = 0; i < 4; i++) {
			outputBytes[i] = packetTypeBytes[i];
			outputBytes[4 + i] = dataSizeBytes[i];
		}
		for (int i = 0; i < nDataSize; i++) {
			outputBytes[8 + i] = dataBytes[i];
		}
		// checksum byte
		outputBytes[8 + nDataSize] = checkSum;
		return outputBytes;
	}

	// save frame capture bytes to temporary image file, return pathname of the temporary image file
	public static String SaveTmpImageFile(byte[] imgBytes) {
		String sTmpImgFoldername = Environment.getExternalStorageDirectory()
				+ "/AMOS";// folder to use for storing the temporary image file
		File fImgFolder = new File(sTmpImgFoldername);
		if (!fImgFolder.exists()) {
			// try to create folder
			try {
				boolean bMadeDir = fImgFolder.mkdirs();
				if (!bMadeDir) {
					// error trying to create folder
					LAST_DATA_ERROR = "Error trying to create folder: "
							+ sTmpImgFoldername;
					return null;
				}
			} catch (Exception e) {
				LAST_DATA_ERROR = String.format(
						"Error %s trying to create folder: %s", e.toString(),
						sTmpImgFoldername);
				return null;
			}
		}
		String sTmpImgFilename = fImgFolder.getPath() + TMP_IMAGE_FILENAME;
		try {
			FileOutputStream f_out = new FileOutputStream(sTmpImgFilename);
			f_out.write(imgBytes);
			f_out.close();
		}
		catch (Exception e) {
			LAST_DATA_ERROR = String.format("Error %s trying to save data to file: %s",e.toString(),sTmpImgFoldername);
			return null;
		}
		return sTmpImgFilename;
	}

	/**
	 * saves bytes from an image file to a local path on this device
	 * @param receivedBytes the bytes downloaded from an image file stored locally on AMOS
	 * @param sImageName the name of the image file (without any directory / path info included)
	 * @return the path of the locally saved image file, or null if a problem occurred
	 */
	public static String SaveImageFile(byte [] receivedBytes, String sImageName) {
		String sAMOSFoldername = Environment.getExternalStorageDirectory()
				+ "/AMOS";// folder to use for storing AMOS files
		File fAMOSFolder = new File(sAMOSFoldername);
		if (!fAMOSFolder.exists()) {
			// try to create folder
			try {
				boolean bMadeDir = fAMOSFolder.mkdirs();
				if (!bMadeDir) {
					// error trying to create folder
					LAST_DATA_ERROR = "Error trying to create folder: "
							+ sAMOSFoldername;
					return null;
				}
			} catch (Exception e) {
				LAST_DATA_ERROR = String.format(
						"Error %s trying to create folder: %s", e.toString(),
						sAMOSFoldername);
				return null;
			}
		}
		String sImgFolderName = sAMOSFoldername + "/Images";//folder used for storing AMOS images
		File fImageFolder = new File(sImgFolderName);
		if (!fImageFolder.exists()) {
			// try to create folder
			try {
				boolean bMadeDir = fImageFolder.mkdirs();
				if (!bMadeDir) {
					// error trying to create folder
					LAST_DATA_ERROR = "Error trying to create folder: "
							+ sImgFolderName;
					return null;
				}
			} catch (Exception e) {
				LAST_DATA_ERROR = String.format(
						"Error %s trying to create folder: %s", e.toString(),
						sImgFolderName);
				return null;
			}
		}

		String sImgFilePath = fImageFolder.getPath() + sImageName;
		try {
			FileOutputStream f_out = new FileOutputStream(sImgFilePath);
			f_out.write(receivedBytes);
			f_out.close();
		}
		catch (Exception e) {
			LAST_DATA_ERROR = String.format("Error %s trying to save data to file: %s",e.toString(),sImgFilePath);
			return null;
		}
		return sImgFilePath;
	}

	/**
	 * saves bytes from a data file to a local path on this device
	 * @param receivedBytes the bytes downloaded from a data file stored locally on AMOS
	 * @param sFileName the name of the data file (without any directory / path info included)
	 * @return the path of the locally saved data file, or null if a problem occurred
	 */
	public static String SaveDataFile(byte [] receivedBytes, String sFileName) {
		String sAMOSFoldername = Environment.getExternalStorageDirectory()
				+ "/AMOS";// folder to use for storing AMOS files
		File fAMOSFolder = new File(sAMOSFoldername);
		if (!fAMOSFolder.exists()) {
			// try to create folder
			try {
				boolean bMadeDir = fAMOSFolder.mkdirs();
				if (!bMadeDir) {
					// error trying to create folder
					LAST_DATA_ERROR = "Error trying to create folder: "
							+ sAMOSFoldername;
					return null;
				}
			} catch (Exception e) {
				LAST_DATA_ERROR = String.format(
						"Error %s trying to create folder: %s", e.toString(),
						sAMOSFoldername);
				return null;
			}
		}
		String sDataFilePath = fAMOSFolder.getPath() + sFileName;
		try {
			FileOutputStream f_out = new FileOutputStream(sDataFilePath);
			f_out.write(receivedBytes);
			f_out.close();
		}
		catch (Exception e) {
			LAST_DATA_ERROR = String.format("Error %s trying to save data to file: %s",e.toString(),sDataFilePath);
			return null;
		}
		return sDataFilePath;
	}
}
