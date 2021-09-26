package com.example.boatcaptain;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.example.boatcaptain.MainActivity.STATUS_MODE;


public class BluetoothCaptain extends Captain {
	public static int LARGEDATA_CHUNKSIZE = 512;//size of serial packets that are read in at a single time
	private static int MAXBYTES_TO_WRITE = 18;//maximum number of bytes that are written to the BLE device at a single time
	public BOAT_DATA m_pBoatData;//most recently collected boat data
	private int m_nErrorCount;//count of consecutive communications errors, gets reset to zero whenever a successful transmission occurs
	private boolean m_bConnected;//flag is true if we have established a successful Bluetooth connection with the boat
	private Vector <REMOTE_COMMAND> m_commands;
	private boolean m_bSendingCommand;//trm_characteristicue if we are currently sending a command and / or expecting some sort of response
	private BluetoothGatt m_bluetoothGatt;
	private BluetoothGattCharacteristic m_characteristic=null;//the Bluetooth Gatt characteristic that is used for the actual reading and writing of data with the AMOS_REMOTE device
	private byte [] m_readBytes;//the bytes that are read in over the local Bluetooth connection
	private byte [] m_extraDataBytes;//extra data bytes that did not fit into the small BLE packet size limit and need to be sent later
	private int m_nLargeBytesOffset = 0;//offset into m_readBytes where a group of image bytes starts
	private Timer m_timeoutTimer;
	private ReentrantLock m_commandLock;//used for controlling access to m_commands vector
	private int m_nDownloadingChunkIndex = 0;//the index of the data chunk (in a large data packet) that is currently being downloaded (starts at 0, and goes up)
	private int m_nLargeDataIndex = 0;//index in m_readBytes from where to start looking for a valid "large data" packet
	private boolean m_bFinishedDownloadingLargeChunk = false;//flag is set to true after a large chunk of data is successfully downloaded
	public int m_nLargePacketTypeDownloaded = 0;//used to track the type of large packet that was most recently downloaded

	BluetoothCaptain() {
		//initialize commands vector
		m_extraDataBytes = null;
		m_commands = new Vector<REMOTE_COMMAND>();
		m_bSendingCommand = false;
		m_bFinishedDownloadingLargeChunk = false;
		m_pBoatData = null;
		m_readBytes = null;
		m_timeoutTimer=null;
		m_commandLock = new ReentrantLock();
		m_nLargeBytesOffset = 0;
		m_nDownloadingChunkIndex = 0;
		m_nLargeDataIndex = 0;

	}

	public void ConnectToBoat() {//establish a Bluetooth connection with the boat


	}

	/**
	 * SetConnected: sets whether or not we are connected to the remote Bluetooth server
	 * @param bConnected set to true if a connection has been made to the remote Bluetooth low energy (BLE) Gatt server, false otherwise
	 */
	public void SetConnected(boolean bConnected, BluetoothGatt bluetoothGatt) {
		m_bConnected = bConnected;
		m_bluetoothGatt = bluetoothGatt;
		if (m_bConnected) {
			//start reading in data (may need to get rid of some extra bytes from the HM-10 after a connection was established)
			m_bSendingCommand=false;//not sending anything yet
			if (m_characteristic==null) {
				GetCharacteristic();
			}
		}
	}

	public PROPELLER_STATE ForwardHo() {// accelerate forward
		PROPELLER_STATE propState = super.ForwardHo();
		if (propState == null) {
			return null;
		}
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.THRUST_ON;
		rc.nNumDataBytes = PROPELLER_STATE.dataSize();
		rc.pDataBytes = propState.getBytes();
		AddCommand(rc);
		return propState;
	}

	public PROPELLER_STATE Stop() {//stops both thrusters
		PROPELLER_STATE propState = super.Stop();
		if (propState == null) {
			return null;
		}
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.THRUST_ON;
		rc.nNumDataBytes = PROPELLER_STATE.dataSize();
		rc.pDataBytes = propState.getBytes();
		AddCommand(rc);
		return propState;
	}
	

	
	public PROPELLER_STATE StarboardHo() {//turn to right at default speed
		PROPELLER_STATE propState = super.StarboardHo();
		if (propState == null) {
			return null;
		}
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.THRUST_ON;
		rc.nNumDataBytes = PROPELLER_STATE.dataSize();
		rc.pDataBytes = propState.getBytes();
		AddCommand(rc);
		return propState;
	}
	

	public PROPELLER_STATE PortHo() {//turn to left
		PROPELLER_STATE propState = super.PortHo();
		if (propState == null) {
			return null;
		}
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.THRUST_ON;
		rc.nNumDataBytes = PROPELLER_STATE.dataSize();
		rc.pDataBytes = propState.getBytes();
		AddCommand(rc);
		return propState;
		
	}
	
	public PROPELLER_STATE BackHo() {//accelerate thrusters backward
		PROPELLER_STATE propState = super.BackHo();
		if (propState == null) {
			return null;
		}
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.THRUST_ON;
		rc.nNumDataBytes = PROPELLER_STATE.dataSize();
		rc.pDataBytes = propState.getBytes();
		AddCommand(rc);
		return propState;
	}
	
/*
	public String RequestGPSPosition() {//query the boat for its GPS position and return a string corresponding to that GPS data
		
		
	}
	
	public String RequestCompassData() {//query the boat for its compass heading, roll, and pitch, and also temperature, return results as a string
		
		
	}
	
	public boolean ReceiveBoatData() {//receive data from boat over Bluetooth connection
		
	}
		
	private void IncrementErrorCount() {
		
	}
	
	private void Cleanup() {//cleanup allocated resources that are no longer being used
		
	}
	
	private boolean SendCommand(REMOTE_COMMAND *pRC) {//sends a remote command out over the network
		
		
	}
	*/

	private boolean AddCommand(REMOTE_COMMAND rc) {//add a command to the list of commands to send, if it is the only command, then send it right away
		RemoveOldCommands();//remove old commands that were sent more than REMOTE_COMMAND.TIMEOUT_TIME_MS ago
		if (rc.nCommand==REMOTE_COMMAND.THRUST_ON)
		{
			RemovePreviousThrustCommands();//no need to send commands for previous thrust states, since the current one is the only one that matters
		}
		if (!BluetoothCaptain.IsLargePacketType(rc.nCommand)) {//for non-large data chunk packets, use a timeout limit for the incoming data
			m_timeoutTimer = new Timer();
			m_timeoutTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					TimeoutMethod();
				}
			}, REMOTE_COMMAND.TIMEOUT_TIME_MS);// launch singleshot timer function in REMOTE_COMMAND.TIMEOUT_TIME_MS ms
		}
		m_commandLock.lock();
		m_commands.addElement(rc);
		if (!m_bSendingCommand) {//send out a command now
			if (!SendCommand(m_commands.firstElement())) {
				m_commandLock.unlock();
				return false;
			}
		}
		m_commandLock.unlock();
		return true;
	}

	private boolean SendCommand(REMOTE_COMMAND rc) {//send out a command over Bluetooth BLE connection
		if (!m_bConnected) {
			//Util.PopupMsg(m_context, R.string.error_ble_notconnected);
			Log.d("debug","error, BLE not connected.\n");
			return false;
		}
		m_bSendingCommand=true;//set flag to indicate that a send is currently in progress
		m_bFinishedDownloadingLargeChunk = false;//always reset flag for downloaded image to false
		m_nDownloadingChunkIndex =0;//reset video chunk index to zero
		m_nNumLargeBlockBytes = 0;//make sure # of image byets is reset
		m_nLargeBytesOffset = 0;
		m_pBoatData = null;//reset boat data object
		if (m_characteristic==null) {
			if (!GetCharacteristic()) {
				//Util.PopupMsg(m_context,R.string.error_could_not_get_characteristic);
				Log.d("debug","error, could not get characteristic.\n");
				return false;
			}
			return false;//can't actually do write yet, need to wait for service and characteristic to be discovered
		}
		//delay for 100 ms (helps to make sure that any previous operation has been fully completed before attempting to send --> probably not the "right" way to do this)
		try {
			Thread.sleep(100);
		}
		catch (Exception e) {
			//do nothing
		}

		if (!m_bluetoothGatt.beginReliableWrite()) {
			//Util.PopupMsg(m_context, R.string.error_initializing_write);
			Log.d("debug","error initializing write.\n");
			return false;
		}
		//send command bytes
		byte[] commandBytes = null;
		int nNumToWrite = 4;
		if (rc.nNumDataBytes>0) {
			nNumToWrite = Math.min(MAXBYTES_TO_WRITE,4+4+rc.nNumDataBytes);
		}
		commandBytes = new byte[nNumToWrite];
		int nNumDataBytes = nNumToWrite - 8;//the number of bytes being sent that are data bytes
		commandBytes[0] = (byte) ((rc.nCommand & 0xff000000) >> 24);
		commandBytes[1] = (byte) ((rc.nCommand & 0x00ff0000) >> 16);
		commandBytes[2] = (byte) ((rc.nCommand & 0x0000ff00) >> 8);
		commandBytes[3] = (byte) (rc.nCommand & 0x000000ff);
		if (nNumDataBytes>0) {
			commandBytes[4] = (byte) ((rc.nNumDataBytes & 0xff000000) >> 24);
			commandBytes[5] = (byte) ((rc.nNumDataBytes & 0x00ff0000) >> 16);
			commandBytes[6] = (byte) ((rc.nNumDataBytes & 0x0000ff00) >> 8);
			commandBytes[7] = (byte) (rc.nNumDataBytes & 0x000000ff);
			for (int i = 0; i < nNumDataBytes; i++) {
				commandBytes[8 + i] = rc.pDataBytes[i];
			}
		}
		if (!m_characteristic.setValue(commandBytes)) {
			Log.d("debug","error setting characteristic value.\n");
			return false;
		}
		if (!m_bluetoothGatt.writeCharacteristic(m_characteristic)) {
			String sError = String.format("error sending %d command bytes, command bytes = %d, %d, %d, %d.\n", nNumToWrite, commandBytes[0], commandBytes[1], commandBytes[2], commandBytes[3]);
			Log.d("debug", sError);
			return false;
		}
		if (nNumDataBytes>0&&nNumDataBytes<rc.nNumDataBytes) {
			//there are other data bytes that need to be sent, save them and send them later
			Log.d("debug", "Save and send other bytes later.\n");
			int nNumExtraBytes = rc.nNumDataBytes - nNumDataBytes;
			m_extraDataBytes = new byte[nNumExtraBytes];
			for (int i=0;i<nNumExtraBytes;i++) {
				m_extraDataBytes[i] = rc.pDataBytes[nNumDataBytes+i];
			}
		}
		else m_extraDataBytes = null;
		String sOK = String.format("wrote %d bytes ok.\n", nNumToWrite);
		Log.d("debug", sOK);
		return true;
	}

	private boolean GetCharacteristic() {//gets the Bluetooth Gatt characteristic that is necessary for reading / writing to the AMOS_REMOTE BLE device. Returns true if successful
		if (m_bluetoothGatt==null) {
			return false;
		}
		//need to discover what services are available
		if (!m_bluetoothGatt.discoverServices()) {
			//Util.PopupMsg(m_context,R.string.error_discovering_services);
			return false;
		}
		return true;
	}

	/**
	 * ServicesDiscovered: call when the callback function for finding bluetooth low energy (BLE) services has been completed successfully
	 */
	public void ServicesDiscovered(BluetoothGatt gatt) {
		//get custom service for HM-10 or compatible device
		m_bluetoothGatt = gatt;
		BluetoothGattService btgService = gatt.getService(UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB"));
		//get the custom characteristic that corresponds to this service and is used for the actual reading and writing of data
		if (btgService!=null) {
			m_characteristic = btgService.getCharacteristic(UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB"));
			if (m_characteristic!=null) {//check to see if there are any commands that are waiting to be sent
				//request notification that characteristic has changed
				gatt.setCharacteristicNotification(m_characteristic,true);
				//also need to set the client characteristic configuration descriptor 0x2902
				UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
				BluetoothGattDescriptor descriptor = m_characteristic.getDescriptor(uuid);
				//test
				byte []testBytes = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
				int nNumTestBytes = testBytes.length;
				for (int i=0;i<nNumTestBytes;i++) {
				    String sTest = String.format("testByte[%d] = %d\n",i,(int)testBytes[i]);
				    Log.d("debug",sTest);
                }
				//end test
				descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
				gatt.writeDescriptor(descriptor);
				m_commandLock.lock();
				int nNumCommands = m_commands.size();
				if (nNumCommands>0) {
					SendCommand(m_commands.firstElement());
				}
				m_commandLock.unlock();
			}
		}
	}

	public void SentOK() {//confirmation that send operation (to the HM-10 and AMOS_REMOTE) was completed successfully
		//now receive response bytes from AMOS to confirm that command was successfully received by AMOS
		if (m_bluetoothGatt==null) return;
		m_bluetoothGatt.executeReliableWrite();
		//if (!m_bluetoothGatt.readCharacteristic(m_characteristic)) {
			//Util.PopupMsg(m_context,R.string.error_reading_characteristic);
		//}
	}

	public boolean DataRead
			(byte []readBytes) {//check data from Bluetooth low energy (BLE) device (return true if successful (data is ok), false otherwise)
		if (readBytes==null) return false;
		m_commandLock.lock();
		//test
		Log.d("debug", "received some BT data.\n");
		//end test
		if (m_commands.size()<=0) {
			Log.d("debug","received some unexpected data.\n");
			m_commandLock.unlock();
			return false;//no commands in queue, so not sure why data arrived???
		}
		REMOTE_COMMAND rc = m_commands.firstElement();
		if (isReadingTransmittedBytes(readBytes,rc)) {
			//do not have a response yet
			//Log.d("debug","Read in transmitted bytes.\n");
			m_commandLock.unlock();
			return false;
		}
		m_readBytes = Util.appendBytes(readBytes,m_readBytes);
		if (m_readBytes==null) {
			m_commandLock.unlock();
			return false;
		}
		/*m_readBytes = FilterOutAMOSCommands(m_readBytes);
		if (m_readBytes==null) {
			m_commandLock.unlock();
			return false;
		}*/
		LARGE_DATA_PACKET largeDataPacket = LARGE_DATA_PACKET.LookForValidLargeDataPacket(m_readBytes, m_nLargeDataIndex);
		if (largeDataPacket!=null) {
			if (largeDataPacket.isValid()) {
				//a valid large data packet has been detected in the incoming data
				//reformat last part of m_readBytes buffer to include the data bytes from this large data packet
				int nLargeDataChunkIndex = largeDataPacket.GetChunkIndex();
				if (nLargeDataChunkIndex>=m_nDownloadingChunkIndex) {
					m_readBytes = largeDataPacket.appendBytesTo(m_readBytes, m_nLargeDataIndex);
					m_nLargeDataIndex = m_readBytes.length;
					int nNumDownloaded = this.GetNumLargeChunkBytesDownloaded();
					//test
					String sDownloaded = String.format("downloaded %d of %d bytes\n",nNumDownloaded,m_nNumLargeBlockBytes);
					Log.d("debug",sDownloaded);
					//end test
					//test
					String sDataChunkIndex = String.format("m_nDownloadingChunkIndex = %d\n",m_nDownloadingChunkIndex);
					Log.d("debug",sDataChunkIndex);
					//end test
					m_nDownloadingChunkIndex++;
					if (nNumDownloaded>=m_nNumLargeBlockBytes) {
						this.m_bFinishedDownloadingLargeChunk = true;
						//send confirmation (2 crc bytes) back to AMOS to indicate that large data packet was received
						//test
						Log.d("debug","sending confirmation\n");
						//end test
						//save packet type that was downloaded
						m_nLargePacketTypeDownloaded = m_commands.firstElement().nCommand;
						this.SendLargePacketDoneChunk(largeDataPacket);
 						//everything must have worked out ok, so now remove this command from the list
						m_commands.removeElementAt(0);
						m_bSendingCommand = false;
						m_commandLock.unlock();
						return true;
					}
				}
				else {
					//must have re-downloaded a large data packet that has already been downloaded, so need to remove it
					//test
					Log.d("debug","re-downloaded large data packet\n");
					//end test
					this.SendLargePacketDoneChunk(largeDataPacket);
					m_readBytes = largeDataPacket.RemoveLastPartialChunk(m_readBytes, m_nLargeDataIndex);//remove all data from m_nLargeDataIndex on
					m_nLargeDataIndex = m_readBytes.length;
					m_commandLock.unlock();
					return false;
				}
				//send confirmation (2 crc bytes) back to AMOS to indicate that large data packet was received
				//test
				Log.d("debug","sending confirmation\n");
				//end test
				if (!this.SendLargePacketDoneChunk(largeDataPacket)) {
					//try again
					if (!this.SendLargePacketDoneChunk(largeDataPacket)) {
						//try one last time
						if (!this.SendLargePacketDoneChunk(largeDataPacket)) {
							//can't send out data for some reason, cancel downloading of image
							m_commands.removeElementAt(0);
							m_bSendingCommand = false;
							m_nNumLargeBlockBytes = 0;
							m_readBytes = null;
							m_commandLock.unlock();
							return false;
						}
					}
				}
			}
			else  {//a garbled large data packet has been detected, need to notify AMOS that the communication was corrupted
				//test
				Log.d("debug","garbled data packet\n");
				//end test
				m_readBytes = largeDataPacket.RemoveLastPartialChunk(m_readBytes, m_nLargeDataIndex);
				//send bytes to indicate that an error occurred
				this.SendLargePacketError();
				m_commandLock.unlock();
				return false;
			}

		}
		else {
			//test
			Log.d("debug","Large data packet is null.\n");
			//end test
		}

		int nNumBytesRead = m_readBytes.length;
		//test
		//String sTest = String.format("nNumBytesRead = %d\n",nNumBytesRead);
		//Log.d("debug",sTest);
		//end test
		if (rc == null) {
			m_bSendingCommand=false;
			Log.d("debug","rc is null.\n");
			m_readBytes=null;
			m_commandLock.unlock();
			return false;
		}

		if (nNumBytesRead<4) {
			//not enough bytes were read
			m_bSendingCommand=false;
			String sErrMsg = String.format("Error, only %d bytes read.\n",nNumBytesRead);
			Log.d("debug",sErrMsg);
			//Util.PopupMsg(m_context,sErrMsg);
			m_readBytes=null;
			m_commandLock.unlock();
			return false;
		}
		byte[] dataTypeBytes = new byte[4];
		//read in first 4 bytes and check to see if they match up with the command that was most recently sent
		for (int i=0;i<4;i++) {
			dataTypeBytes[i] = m_readBytes[i];
		}
		byte [] inverted_bytes = Util.reverseByteOrder(dataTypeBytes,4);
		int nDataType = Util.toInt(inverted_bytes);
		if (nDataType!=rc.nCommand) {//invalid response to command
			m_bSendingCommand=false;
			String sMsg = String.format("Invalid response to command, got %d, expected %d.\n",nDataType,rc.nCommand);
			Log.d("debug",sMsg);
			//Util.PopupMsg(m_context,R.string.invalid_response);
			m_readBytes=null;
			m_commandLock.unlock();
			return false;
		}

		// create structure for receiving boat data
		BOAT_DATA pBoatData = BoatCommand.CreateBoatData(nDataType);
		if (pBoatData == null) {
			m_bSendingCommand=false;
			Log.d("debug","Could not create boat data.\n");
			//Util.PopupMsg(m_context,R.string.could_not_create_data);
			m_readBytes = null;
			m_commandLock.unlock();
			return false;// unable to create this data type
		}

		if (pBoatData.nDataSize==0) {
			//no need to read in any more bytes, because this type of response does not include data
			m_commands.removeElementAt(0);
			m_bSendingCommand = false;
			m_pBoatData=null;//no actual data returned from boat
			m_readBytes=null;
			m_commandLock.unlock();
			return true;//confirmation was received successfully, but no data was included after that
		}

		if (nNumBytesRead<12) {//not enough size bytes have arrived yet
			//did not read in enough bytes, need to wait for more bytes to arrive later
			m_commandLock.unlock();
			return false;
		}

		byte[] dataSizeBytes = new byte[4];
		for (int i = 0; i < 4; i++) {
			dataSizeBytes[i] = m_readBytes[8 + i];
		}
		int nDataSize = Util.toInt(dataSizeBytes);


		if (pBoatData.nDataSize != nDataSize) {
			m_bSendingCommand=false;
			String sMsg = String.format("Unexpected data size, pBoatData.nDataSize = %d, nDataSize = %d.\n",pBoatData.nDataSize,nDataSize);
			Log.d("debug",sMsg);
			//Util.PopupMsg(m_context,R.string.unexpected_datasize);
			m_readBytes = null;
			m_commandLock.unlock();
			return false;// unexpected data size
		}

		if ((nNumBytesRead-12)<pBoatData.nDataSize) {
			//did not read in enough bytes, need to wait for more bytes to arrive later
			m_commandLock.unlock();
			return false;
		}
		try {
			for (int i = 0; i < pBoatData.nDataSize; i++) {
				pBoatData.dataBytes[i] = m_readBytes[12 + i];
			}
		}
		catch (Exception e) {
			Log.d("debug",e.toString());
			m_commandLock.unlock();
			return false;
		}
		//special case for video capture frame
		boolean bSkipProcessData=false;
		if (BluetoothCaptain.IsLargePacketType(pBoatData.nPacketType)) {//==REMOTE_COMMAND.VIDEO_DATA_PACKET) {
			bSkipProcessData=true;
			if (m_nNumLargeBlockBytes==0) {
				m_nNumLargeBlockBytes = Util.toInt(pBoatData.dataBytes);
				m_nLargeBytesOffset = 12 + pBoatData.nDataSize + 1;
				m_nLargeDataIndex = m_nLargeBytesOffset;
				//make sure image is not super large
				if (m_nNumLargeBlockBytes >= 10000000) {
					String sErrorMsg = String.format("Number of image bytes is too large: %d\n", m_nNumLargeBlockBytes);
					Log.d("debug", sErrorMsg);
					m_bSendingCommand = false;
					m_nLargeBytesOffset = 0;
					m_nLargeDataIndex = 0;
					m_nNumLargeBlockBytes = 0;
					m_readBytes = null;
				}
				m_commandLock.unlock();
				return false;//need to read in more bytes for image capture
			}
		}

		int nNumBytesToReceive = pBoatData.nDataSize + 1;
		int nNumBytesRemaining = nNumBytesRead - 12;
		if (m_nLargeBytesOffset>0) {
			nNumBytesToReceive+=m_nNumLargeBlockBytes;
			nNumBytesRemaining = nNumBytesRead - m_nLargeBytesOffset;
		}

		if (nNumBytesRemaining<nNumBytesToReceive) {
			//did not read in enough bytes, need to wait for more bytes to arrive later
			//String sMsg = String.format("bytesremaining = %d, numtoreceive = %d\n",nNumBytesRemaining,nNumBytesToReceive);
			//Log.d("debug",sMsg);
			//Util.PopupMsg(m_context,R.string.not_enough_bytes);
			m_commandLock.unlock();
			return false;
		}
		if (!bSkipProcessData&&!ProcessBoatData(pBoatData)) {
			//invalid data, could not process it
			m_bSendingCommand = false;
			m_pBoatData=null;//no vaoid data returned from boat
			m_readBytes=null;
			Log.d("debug","no valid data returned from boat\n");
			m_nNumLargeBlockBytes = 0;
			m_commandLock.unlock();
			return false;
		}
		if (BluetoothCaptain.IsLargePacketType(pBoatData.nPacketType)) {
			m_commandLock.unlock();
			return false;//should be near end of large data packet transmission if we get here, probably need just one additional BLE packet
		}
		//ProcessBoatData(pBoatData);
		m_pBoatData = pBoatData;
		//everything must have worked out ok, so now remove this command from the list
		m_commands.removeElementAt(0);
		m_commandLock.unlock();
		m_bSendingCommand = false;
		m_nNumLargeBlockBytes = 0;
		if (!BluetoothCaptain.IsLargePacketType(m_pBoatData.nPacketType)) {
			m_readBytes = null;
		}
		Log.d("debug","Parsed boat data ok.\n");
		return true;
	}

	/**
	 * SentDone: call this function after bytes have been sent out, when it is time to start reading in the response from AMOS
	 */
	public void SentDone() {
		//if (this.isReadingLargeChunkData()) {
		//	return;//reading in image data, don't need to keep reading characteristic
		//}
		if (this.m_extraDataBytes!=null) {
			SendExtraDataBytes();//send out extra data bytes
			return;
		}
		if (!m_bluetoothGatt.readCharacteristic(m_characteristic)) {
			//Util.PopupMsg(m_context,R.string.error_reading_characteristic);
		}
	}

	private boolean isReadingTransmittedBytes(byte []readBytes, REMOTE_COMMAND rc) {
		//return true if the bytes in readBytes correspond to the remote command bytes that correspond to rc
		//send command bytes
		if (readBytes==null) return false;
		byte[] commandBytes = null;
		int nNumBytesRead = readBytes.length;
		int numBytesTransmitted = rc.nNumDataBytes+8;
		if (nNumBytesRead!=numBytesTransmitted) {
			return false;//size of data is not same, so can't be reading bytes that were just transmitted
		}
		if (rc.nNumDataBytes>0) {
			commandBytes = new byte[4+4+rc.nNumDataBytes];
		}
		else commandBytes = new byte[8];
		commandBytes[0] = (byte) ((rc.nCommand & 0xff000000) >> 24);
		commandBytes[1] = (byte) ((rc.nCommand & 0x00ff0000) >> 16);
		commandBytes[2] = (byte) ((rc.nCommand & 0x0000ff00) >> 8);
		commandBytes[3] = (byte) (rc.nCommand & 0x000000ff);
		if (rc.nNumDataBytes>0) {
			commandBytes[4] = (byte) ((rc.nNumDataBytes & 0xff000000) >> 24);
			commandBytes[5] = (byte) ((rc.nNumDataBytes & 0x00ff0000) >> 16);
			commandBytes[6] = (byte) ((rc.nNumDataBytes & 0x0000ff00) >> 8);
			commandBytes[7] = (byte) (rc.nNumDataBytes & 0x000000ff);
			for (int i = 0; i < rc.nNumDataBytes; i++) {
				commandBytes[8 + i] = rc.pDataBytes[i];
			}
		}
		for (int i=0;i<nNumBytesRead;i++) {
			if (commandBytes[i]!=readBytes[i]) {
				return false;//bytes that were read in are not the same as those that were just transmitted out
			}
		}
		return true;//we are reading in the same bytes that were just transmitted out
	}

	/**
	 * query the boat for its GPS position
	 */
	public void RequestGPSPosition() {
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.GPS_DATA_PACKET;
		rc.nNumDataBytes = 0;
		rc.pDataBytes = null;
		AddCommand(rc);
	}

	/**
	 * query the boat for its compass data (roll, pitch, heading, and compass temperature)
	 */
	public void RequestCompassData() {
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.COMPASS_DATA_PACKET;
		rc.nNumDataBytes = 0;
		rc.pDataBytes = null;
		AddCommand(rc);
	}

	/**
	 * query the boat for its voltage data
	 */
	public void RequestVoltageData() {
		//test
		Log.d("debug","requesting voltage data.\n");
		//end test
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.BATTVOLTAGE_DATA_PACKET;
		rc.nNumDataBytes = 0;
		rc.pDataBytes = null;
		AddCommand(rc);
	}

	/**
	 * query the boat for its diagnostics data
	 */
	public void RequestDiagnosticsData() {
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.DIAGNOSTICS_DATA_PACKET;
		rc.nNumDataBytes = 0;
		rc.pDataBytes = null;
		AddCommand(rc);
	}

	/**
	 * query the boat for its leak sensor data
	 */
	public void RequestLeakData() {
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.LEAK_DATA_PACKET;
		rc.nNumDataBytes = 0;
		rc.pDataBytes = null;
		AddCommand(rc);
	}


	/**
	 * Request an image capture with feature markings in it from the boat
	 * @param nFeatureThreshold  value from 0 to 255 that is used to control how feature markings
	 * 	are determined in the image. nFeatureThreshold==0 disables or removes feature markers from the image.
	 */
	public boolean RequestVideoImage(int nFeatureThreshold) {
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.VIDEO_DATA_PACKET;
		rc.nNumDataBytes = 4;
		byte []featureBytes = Util.toByteArray(nFeatureThreshold);
		rc.pDataBytes = Util.reverseByteOrder(featureBytes, 4);
		return AddCommand(rc);
	}


	private byte [] FilterOutAMOSCommands(byte [] a) {//check to see if bytes contain one or more local AMOS commands
		//if so, such bytes need to be removed from the incoming byte stream, as they are just local AMOS commands, not data bytes that AMOS has sent out to this device
		//byte sequences that need to be filtered out include the following:
		//0x0D, "current", 0x0D
		//0x0D, "power", 0x0D
		//0x0D, "solar", 0x0D
		//0x0D, "down###", 0x0D --> ### is a variable number of numeric digits
		if (a==null) {
			return null;
		}
		int nNumToCheck = a.length;
		if (nNumToCheck<7) {
			return a;//too small to filter anything out
		}
		//look for 0x0D, "current", 0x0D sequence
		int nLimit = nNumToCheck - 8;
		for (int i=0;i<nLimit;i++) {
			if (a[i]==0x0d&&a[i+1]=='c'&&a[i+2]=='u'&&a[i+3]=='r'&&a[i+4]=='r'&&
			a[i+5]=='e'&&a[i+6]=='n'&&a[i+7]=='t'&&a[i+8]==0x0d) {
				int nNewSize = nNumToCheck - 9;
				if (nNewSize<=0) {
					return null;
				}
				byte [] b = new byte[nNewSize];
				for (int j=0;j<i;j++) {
					b[j] = a[j];
				}
				for (int j=i+9;j<nNumToCheck;j++) {
					b[j-9] = a[j];
				}
				return FilterOutAMOSCommands(b);
			}
		}
		//look for 0x0D, "power", 0x0D sequence
		nLimit = nNumToCheck - 6;
		for (int i=0;i<nLimit;i++) {
			if (a[i]==0x0d&&a[i+1]=='p'&&a[i+2]=='o'&&a[i+3]=='w'&&a[i+4]=='e'&&a[i+5]=='r'&&a[i+6]==0x0d) {
				int nNewSize = nNumToCheck - 7;
				if (nNewSize<=0) {
					return null;
				}
				byte [] b = new byte[nNewSize];
				for (int j=0;j<i;j++) {
					b[j] = a[j];
				}
				for (int j=i+7;j<nNumToCheck;j++) {
					b[j-7] = a[j];
				}
				return FilterOutAMOSCommands(b);
			}
		}
		//look for 0x0D, "solar", 0x0D sequence
		nLimit = nNumToCheck - 6;
		for (int i=0;i<nLimit;i++) {
			if (a[i]==0x0d&&a[i+1]=='s'&&a[i+2]=='o'&&a[i+3]=='l'&&a[i+4]=='a'&&a[i+5]=='r'&&a[i+6]==0x0d) {
				int nNewSize = nNumToCheck - 7;
				if (nNewSize<=0) {
					return null;
				}
				byte [] b = new byte[nNewSize];
				for (int j=0;j<i;j++) {
					b[j] = a[j];
				}
				for (int j=i+7;j<nNumToCheck;j++) {
					b[j-7] = a[j];
				}
				return FilterOutAMOSCommands(b);
			}
		}
		// look for 0x0D, "down" sequence
		nLimit = nNumToCheck - 4;
		for (int i=0;i<nLimit;i++) {
			if (a[i]==0x0d&&a[i+1]=='d'&&a[i+2]=='o'&&a[i+3]=='w'&&a[i+4]=='n') {
				int nNewSize = nNumToCheck - 5;
				int nFirstCRIndex = i;
				i=i+5;
				int nLastCRIndex = i;
				while (nNewSize>0&&i<nLimit) {
					nNewSize--;
					if (a[i]==0x0d) {
						nLastCRIndex = i;
						break;
					}
					i++;
				}
				if (nNewSize<=0) {
					return null;
				}
				byte [] b = new byte[nNewSize];
				for (int j=0;j<nFirstCRIndex;j++) {
					b[j] = a[j];
				}
				for (int j=nLastCRIndex+1;j<nNumToCheck;j++) {
					b[nFirstCRIndex+j-nLastCRIndex-1] = a[j];
				}
				return FilterOutAMOSCommands(b);
			}
		}
		//nothing filtered out, just return original byte array
		return a;
	}


	/**
	 * Check to see if a large chunk of data is currently being read from AMOS.
	 *
	 * @return true if a large chunk of data is currently being read in, otherwise return false.
	 */
	public boolean isReadingLargeChunkData() {
		if (m_commands==null) {
			return false;
		}
		m_commandLock.lock();
		int nNumCommands = m_commands.size();
		if (nNumCommands<=0) {
			m_commandLock.unlock();
			return false;
		}
		REMOTE_COMMAND rc = m_commands.firstElement();
		if (!BluetoothCaptain.IsLargePacketType(rc.nCommand)) {//current command is not a large chunk of data command
			m_commandLock.unlock();
			return false;
		}
		m_commandLock.unlock();
		return true;
	}


	/**
	 * SendLargePacketDoneChunk: send confirmation back to AMOS that a large chunk of data has been successfully downloaded
	 * @param large_data_packet an object of type LARGE_DATA_PACKET that corresponds to the large packet of data that was just successfully downloaded
	 *
	 * @return true if completed successfully, otherwise false
	 */
	public boolean SendLargePacketDoneChunk(LARGE_DATA_PACKET large_data_packet) {
		if (!m_bConnected) {
			Log.d("debug","Error, not connected to HM-10.");
			return false;
		}
		byte []bytes_to_send = new byte[4];
		byte []crc_bytes = large_data_packet.GetCRCBytes();
		if (crc_bytes==null) {
			Log.d("debug","crc_bytes null.");
			return false;
		}
		bytes_to_send[0] = 0;
		bytes_to_send[1] = 0;
		bytes_to_send[2] = crc_bytes[0];
		bytes_to_send[3] = crc_bytes[1];
		if (m_characteristic==null) {
			if (!GetCharacteristic()) {
				Log.d("debug","Error, could not get characteristic.");
				return false;
			}
		}
		if (!m_bluetoothGatt.beginReliableWrite()) {
			Log.d("debug","Error trying to begin reliable write.");
			return false;
		}
		if (!m_characteristic.setValue(bytes_to_send)) {
			Log.d("debug","Error trying to set value of characteristic.");
			return false;
		}

		if (!m_bluetoothGatt.writeCharacteristic(m_characteristic)) {
			Log.d("debug","Error trying to write characteristic.");
			//test
			//return false;//appears to be some sort of driver problem on some devices, e.g. samsung SM-T290 used for testing sometimes returns false for the writeCharacteristic, even though the write appears to always happen. For now, need to just ignore the return value of the writeCharacteristic function.
			//end test
		}
		//end test
		//test
		String sTest = String.format("crc0 = %d, crc1 = %d\n",(int)crc_bytes[0],(int)crc_bytes[1]);
		Log.d("debug",sTest);
		//end test
		return true;
	}

	private void SendLargePacketError() {//send four 0xFF bytes back to AMOS to indicate that the last data packet got garbled somehow
		if (!m_bConnected) {
			Log.d("debug","Error, not connected to HM-10.");
		}
		if (m_characteristic==null) {
			if (!GetCharacteristic()) {
				Log.d("debug","Error, could not get characteristic.");
			}
			return;
		}
		if (!m_bluetoothGatt.beginReliableWrite()) {
			Log.d("debug","Error trying to begin reliable write.");
		}
		byte [] error_bytes = new byte[4];
		error_bytes[0] = (byte)0xff;
		error_bytes[1] = (byte)0xff;
		error_bytes[2] = (byte)0xff;
		error_bytes[3] = (byte)0xff;
		if (!m_characteristic.setValue(error_bytes)) {
			Log.d("debug","Error trying to set value of characteristic.");
		}
		if (!m_bluetoothGatt.writeCharacteristic(m_characteristic)) {
			Log.d("debug","Error trying to write characteristic.");
		}
	}

	/**
	 * Get the total number of large chunk bytes that have been downloaded so far in the current request for a large chunk of data. This function assumes that the download of a large chunk of data is currently
	 * in progress. The calling function should call {@link #isReadingLargeChunkData()} first to make sure of this.
	 * @return the total number of bytes downloaded so far in the current large chunk download request.
	 */
	public int GetNumLargeChunkBytesDownloaded() {
		if (m_readBytes==null) return 0;
		int nNumDownloaded = m_nLargeDataIndex - m_nLargeBytesOffset;
		if (nNumDownloaded<0) nNumDownloaded = 0;
		return nNumDownloaded;
	}

	/**
	 * Get the bytes that correspond to the downloaded large packet of data
	 * @return an array of bytes that corresponds to the downloaded packet of data.
	 */
	byte [] GetLargeChunkBytes() {
		if (this.m_nNumLargeBlockBytes<=0) return null;
		byte [] retVal = new byte[m_nNumLargeBlockBytes];
		int nNumBytesAvail = m_readBytes.length;
		if (nNumBytesAvail<(m_nLargeBytesOffset+m_nNumLargeBlockBytes)) {
			return null;//not enough bytes were read in
		}
		for (int i=0;i<m_nNumLargeBlockBytes;i++) {
			retVal[i] = m_readBytes[m_nLargeBytesOffset+i];
		}
		return retVal;
	}

	/**
	 * Check to see if the download of a large data chunk has completed.
	 * @return true if we are finished downloading all of the bytes from a large data chunk
	 */
	public boolean isFinishedDownloadingLargeChunk() {
		return m_bFinishedDownloadingLargeChunk;
	}

	/**
	 * Call this function after the large chunk of data has been processed / used
	 */
	public void StopReadingLargeChunkData() {
		m_pBoatData = null;
		m_commandLock.lock();
		if (m_commands.size()>0) {
			REMOTE_COMMAND rc = m_commands.firstElement();
			if (rc.nCommand==REMOTE_COMMAND.VIDEO_DATA_PACKET) {
				m_commands.removeElementAt(0);
			}
		}
		m_bSendingCommand = false;
		m_readBytes = null;
		m_nNumLargeBlockBytes = 0;
		m_commandLock.unlock();
	}

	private byte [] RemoveLastPartialChunk(byte [] buf, int nIndex) {//remove the last partial
		//data chunk in buf that occurs before the index nIndex, the return value of the function
		//is the shortened version of the byte array buf
		if (buf==null) return null;
		if (nIndex<20||m_nLargeBytesOffset==0) {//this function is currently only applicable for buffers of video image capture data
			return buf;//don't do anything
		}
		int nNumPartialChunkBytes = (nIndex - m_nLargeBytesOffset) % LARGEDATA_CHUNKSIZE;
		if (nNumPartialChunkBytes==0) {
			nNumPartialChunkBytes = LARGEDATA_CHUNKSIZE;
		}
		int nNewNumBytes = nIndex - nNumPartialChunkBytes;
		byte []retVal = new byte[nNewNumBytes];
		for (int i=0;i<nNewNumBytes;i++) {
			retVal[i] = buf[i];
		}
		//test
		Log.d("debug","removed partial chunk\n");
		//end test
		return retVal;
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
		AddCommand(rc);
		return true;
	}

	public boolean CancelHome() {//function sends command to boat to cancel a previously issued homing command
		//send command to boat to return home
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.CANCEL_OPERATION;
		rc.nNumDataBytes = 0;
		rc.pDataBytes = null;
		AddCommand(rc);
		return true;
	}

	private void TimeoutMethod() {
		// This method is called directly by the timer
		// and runs in the same thread as the timer.
		//clear out any old commands that were sent more than REMOTE_COMMAND.TIMEOUT_TIME_MS ago
		boolean bCommandsRemoved = RemoveOldCommands();
		m_commandLock.lock();
		int nNumCommands = m_commands.size();
		if (bCommandsRemoved) {//send the command that is now first in the queue
			m_bSendingCommand = false;
			//renew times of commands
			for (int i=0;i<nNumCommands;i++) {
				m_commands.elementAt(i).RenewTime();
			}
			if (nNumCommands>0) {
				SendCommand(m_commands.firstElement());//send out the first command in the queue
			}
		}
		m_commandLock.unlock();
	}

	private boolean RemoveOldCommands() {//remove old commands that were sent more than REMOTE_COMMAND.TIMEOUT_TIME_MS ago
		m_commandLock.lock();
		int nNumCommands = m_commands.size();
		boolean bCommandsRemoved = false;
		for (int i=0;i<nNumCommands;i++) {
			if (m_commands.firstElement().isCommandOld()) {
				m_commands.removeElementAt(0);
				bCommandsRemoved = true;
			}
			else break;//commands are in order from oldest to newest, so no subsequent commands can be "old"
		}
		nNumCommands = m_commands.size();
		if (bCommandsRemoved) {
			m_bSendingCommand=false;
			this.m_readBytes=null;
			m_nNumLargeBlockBytes = 0;
		}
		m_commandLock.unlock();
		return bCommandsRemoved;
	}

	private void RemovePreviousThrustCommands() {//remove any REMOTE_COMMAND.THRUST_ON commands that are currently in the queue
		m_commandLock.lock();
		int nNumCommands = m_commands.size();
		boolean bCommandsRemoved = false;
		for (int i=0;i<nNumCommands;i++) {
			if (m_commands.firstElement().nCommand==REMOTE_COMMAND.THRUST_ON) {
				m_commands.removeElementAt(0);
				bCommandsRemoved = true;
			}
		}
		nNumCommands = m_commands.size();
		if (bCommandsRemoved) {
			m_bSendingCommand=false;
			this.m_readBytes=null;
			m_nNumLargeBlockBytes = 0;
		}
		m_commandLock.unlock();
	}

	private void SendExtraDataBytes() {//send out extra data bytes
		if (m_extraDataBytes==null) return;
		if (!m_bConnected) {
			m_extraDataBytes = null;
			return;
		}
		if (m_characteristic==null) {
			m_extraDataBytes = null;
			return;
		}

		//delay for 100 ms (helps to make sure that any previous operation has been fully completed before attempting to send --> probably not the "right" way to do this)
		try {
			Thread.sleep(1000);
		}
		catch (Exception e) {
			//do nothing
		}

		if (!m_bluetoothGatt.beginReliableWrite()) {
			//Util.PopupMsg(m_context, R.string.error_initializing_write);
			Log.d("debug","error initializing write.\n");
			m_extraDataBytes = null;
			return;
		}
		byte[] dataBytes = null;
		int nNumToWrite = Math.min(MAXBYTES_TO_WRITE,m_extraDataBytes.length);
		dataBytes = new byte[nNumToWrite];
		for (int i = 0; i < nNumToWrite; i++) {
			dataBytes[i] = m_extraDataBytes[i];
		}
		if (!m_characteristic.setValue(dataBytes)) {
			Log.d("debug","error setting characteristic value.\n");
			m_extraDataBytes = null;
			return;
		}
		if (!m_bluetoothGatt.writeCharacteristic(m_characteristic)) {
			String sError = String.format("error sending %d data bytes.\n",nNumToWrite);
			Log.d("debug", sError);
			m_extraDataBytes = null;
			return;
		}
		//test
		String sTest = String.format("Wrote %d extra bytes.\n",nNumToWrite);
		Log.d("debug",sTest);
		//end test
		if (nNumToWrite<m_extraDataBytes.length) {
			//there are other data bytes that need to be sent, save them and send them later
			int nNumExtraBytes = m_extraDataBytes.length - nNumToWrite;
			byte [] extraDataBytes = new byte[nNumExtraBytes];
			for (int i=0;i<nNumExtraBytes;i++) {
				extraDataBytes[i] = m_extraDataBytes[nNumToWrite+i];
			}
			m_extraDataBytes = extraDataBytes;
		}
		else m_extraDataBytes = null;
	}

	public void GetRemoteDataFiles() {
		//send command to request the names of the remote data files available on AMOS
		REMOTE_COMMAND rc = new REMOTE_COMMAND();
		rc.nCommand = REMOTE_COMMAND.LIST_REMOTE_DATA;
		rc.nNumDataBytes = 0;
		rc.pDataBytes = null;
		AddCommand(rc);
	}

	public static boolean IsLargePacketType(int nPacketType) {
		if (nPacketType==REMOTE_COMMAND.LIST_REMOTE_DATA||nPacketType==REMOTE_COMMAND.LIST_REMOTE_IMAGE||
		nPacketType==REMOTE_COMMAND.LIST_REMOTE_LOG||nPacketType==REMOTE_COMMAND.LIST_REMOTE_SCRIPTS||
		nPacketType==REMOTE_COMMAND.VIDEO_DATA_PACKET) {
			return true;
		}
		return false;
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
		//use m_extraDataBytes to hold the text of the remote filename
		AddCommand(rc);
		m_extraDataBytes = sRemoteFilename.getBytes();
		m_commandLock.lock();
		while (m_extraDataBytes!=null) {
			SendExtraDataBytes();
		}
		m_commandLock.unlock();
		return true;
	}
}

