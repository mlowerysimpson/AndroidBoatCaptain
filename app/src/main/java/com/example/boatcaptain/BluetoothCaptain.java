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

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.example.boatcaptain.MainActivity.STATUS_MODE;


public class BluetoothCaptain extends Captain {
	public static int LARGEDATA_CHUNKSIZE = 512;//size of serial packets that are read in at a single time
	public BOAT_DATA m_pBoatData;//most recently collected boat data
	private int m_nErrorCount;//count of consecutive communications errors, gets reset to zero whenever a successful transmission occurs
	private boolean m_bConnected;//flag is true if we have established a successful Bluetooth connection with the boat
	private Vector <REMOTE_COMMAND> m_commands;
	private boolean m_bSendingCommand;//trm_characteristicue if we are currently sending a command and / or expecting some sort of response
	private Context m_context;
	private BluetoothGatt m_bluetoothGatt;
	private BluetoothGattCharacteristic m_characteristic=null;//the Bluetooth Gatt characteristic that is used for the actual reading and writing of data with the AMOS_REMOTE device
	private byte [] m_readBytes;//the bytes that are read in over the local Bluetooth connection
	private int m_nImageBytesOffset = 0;//offset into m_readBytes where a group of image bytes starts
	private Timer m_timeoutTimer;
	private ReentrantLock m_commandLock;//used for controlling access to m_commands vector
	BluetoothCaptain(Context context) {
		//initialize commands vector
		m_commands = new Vector<REMOTE_COMMAND>();
		m_bSendingCommand = false;
		m_context = context;
		m_pBoatData = null;
		m_readBytes = null;
		m_timeoutTimer=null;
		m_commandLock = new ReentrantLock();
		m_nImageBytesOffset = 0;
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
		m_timeoutTimer = new Timer();
		m_timeoutTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				TimeoutMethod();
			}
		}, REMOTE_COMMAND.ITMEOUT_TIME_MS);// launch singleshot timer function in REMOTE_COMMAND.TIMEOUT_TIME_MS ms
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
		if (m_characteristic==null) {
			if (!GetCharacteristic()) {
				//Util.PopupMsg(m_context,R.string.error_could_not_get_characteristic);
				Log.d("debug","error, could not get characteristic.\n");
				return false;
			}
			return false;//can't actually do write yet, need to wait for service and characteristic to be discovered
		}
		if (!m_bluetoothGatt.beginReliableWrite()) {
			//Util.PopupMsg(m_context, R.string.error_initializing_write);
			Log.d("debug","error initializing write.\n");
			return false;
		}
		//send command bytes
		byte[] commandBytes = null;
		if (rc.nNumDataBytes>0) {
			commandBytes = new byte[4+4+rc.nNumDataBytes];
		}
		else commandBytes = new byte[4];
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
		if (!m_characteristic.setValue(commandBytes)) {
			Log.d("debug","error setting characteristic value.\n");
			return false;
		}
		if (!m_bluetoothGatt.writeCharacteristic(m_characteristic)) {
			Log.d("debug", "error sending command bytes.\n");
			return false;
		}
		//now need to wait for confirmation that bytes were successfully sent...
		return true;
	}

	private boolean GetCharacteristic() {//gets the Bluetooth Gatt characteristic that is necessary for reading / writing to the AMOS_REMOTE BLE device. Returns true if successful
		if (m_bluetoothGatt==null) {
			return false;
		}
		//need to discover what services are available
		if (!m_bluetoothGatt.discoverServices()) {
			Util.PopupMsg(m_context,R.string.error_discovering_services);
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

	public boolean DataRead(byte []readBytes) {//check data from Bluetooth low energy (BLE) device (return true if successful (data is ok), false otherwise)
		if (readBytes==null) return false;
		m_commandLock.lock();
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
		m_readBytes = FilterOutAMOSCommands(m_readBytes);
		if (m_readBytes==null) {
			m_commandLock.unlock();
			return false;
		}
		int nResyncIndex = FindResyncBytes(m_readBytes);
		if (nResyncIndex>0) {
			m_readBytes = RemoveLastPartialChunk(m_readBytes, nResyncIndex);
		}

		int nNumBytesRead = m_readBytes.length;
		//test
		String sTest = String.format("nNumBytesRead = %d\n",nNumBytesRead);
		Log.d("debug",sTest);
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

		if (nNumBytesRead<8) {//not enough size bytes have arrived yet
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
		for (int i = 0; i < pBoatData.nDataSize; i++) {
			pBoatData.dataBytes[i] = m_readBytes[12+i];
		}
		//special case for video capture frame
		boolean bSkipProcessData=false;
		if (pBoatData.nPacketType==REMOTE_COMMAND.VIDEO_DATA_PACKET) {
			bSkipProcessData=true;
			if (m_nNumImageBytes==0) {
				m_nNumImageBytes = Util.toInt(pBoatData.dataBytes);
				m_nImageBytesOffset = 12 + pBoatData.nDataSize + 1;
				//make sure image is not super large
				if (m_nNumImageBytes >= 10000000) {
					String sErrorMsg = String.format("Number of image bytes is too large: %d\n", m_nNumImageBytes);
					Log.d("debug", sErrorMsg);
					m_bSendingCommand = false;
					m_readBytes = null;
				}
				m_commandLock.unlock();
				return false;//need to read in more bytes for image capture
			}
		}

		int nNumBytesToReceive = pBoatData.nDataSize + 1;
		int nNumBytesRemaining = nNumBytesRead - 12;
		if (m_nImageBytesOffset>0) {
			nNumBytesToReceive+=m_nNumImageBytes;
			nNumBytesRemaining = nNumBytesRead - m_nImageBytesOffset;
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
			m_commandLock.unlock();
			return false;
		}
		//ProcessBoatData(pBoatData);
		m_pBoatData = pBoatData;
		//everything must have worked out ok, so now remove this command from the list
		m_commands.removeElementAt(0);
		m_commandLock.unlock();
		m_bSendingCommand = false;
		if (m_pBoatData.nPacketType!=REMOTE_COMMAND.VIDEO_DATA_PACKET) {
			m_readBytes = null;
		}
		return true;
	}

	/**
	 * SentDone: call this function after bytes have been sent out, when it is time to start reading in the response from AMOS
	 */
	public void SentDone() {
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
	 * Check to see if video data is currently being read from AMOS.
	 *
	 * @return true if video data is currently being read in, otherwise return false.
	 */
	public boolean isReadingVideoData() {
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
		if (rc.nCommand!=REMOTE_COMMAND.VIDEO_DATA_PACKET) {//current command is not a video command
			m_commandLock.unlock();
			return false;
		}
		m_commandLock.unlock();
		return true;
	}

	/**
	 * Check to see if a download of video data has finished a complete "chunk", i.e. the total # of bytes downloaded is a multiple of LARGEDATA_CHUNKSIZE. This function assumes that a video capture
	 * download is in fact in progress; the calling functino should use the {@link #isReadingVideoData()} function first to make sure of this.
	 * @return true if the download of video data has finished a complete chunk, false otherwise.
	 */
	public boolean isFinishedVideoChunk() {
		int nNumDownloaded = GetNumVideoBytesDownloaded();
		if (nNumDownloaded<=0) {
			return false;//nothing downloaded yet
		}
		if (this.isFinishedDownloadingVideo()) {
			return true;//finished downloading video, so last video chunk must also be finished
		}
		int nRemainder = nNumDownloaded % LARGEDATA_CHUNKSIZE;
		//test
		String sMsg = String.format("chunk downloaded: %d\n",nRemainder);
		Log.d("debug",sMsg);
		//end test
		return (nRemainder==0);
	}

	/**
	 * Send confirmation byte (0x0D) to AMOS that a chunk of video data was successfully received
	 */
	public void SendVideoDoneChunk() {
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
		//send confirmation byte
		byte[] confirmationByte = new byte[1];

		confirmationByte[0] = 0x0d;
		if (!m_characteristic.setValue(confirmationByte)) {
			Log.d("debug","Error trying to set value of characteristic.");
		}
		if (!m_bluetoothGatt.writeCharacteristic(m_characteristic)) {
			Log.d("debug","Error trying to write characteristic.");
		}
		//now need to wait for confirmation that bytes were successfully sent...
	}

	/**
	 * Get the total number of video bytes that have been downloaded so far in the current request for an image capture. This function assumes that the download of image data is currently
	 * in progress. The calling function should call {@link #isReadingVideoData()} first to make sure of this.
	 * @return the total number of bytes downloaded so far in the current image capture request.
	 */
	public int GetNumVideoBytesDownloaded() {
		if (m_readBytes==null) return 0;
		int nNumDownloaded = this.m_readBytes.length - m_nImageBytesOffset;
		if (nNumDownloaded<0) nNumDownloaded = 0;
		return nNumDownloaded;
	}

	/**
	 * Get the bytes that correspond to the downloaded image.
	 * @return an array of bytes that corresponds to the downloaded image.
	 */
	byte [] GetImageBytes() {
		if (this.m_nNumImageBytes<=0) return null;
		byte [] retVal = new byte[m_nNumImageBytes];
		int nNumBytesAvail = m_readBytes.length;
		if (nNumBytesAvail<(m_nImageBytesOffset+m_nNumImageBytes)) {
			return null;//not enough bytes were read in
		}
		for (int i=0;i<m_nNumImageBytes;i++) {
			retVal[i] = m_readBytes[m_nImageBytesOffset+i];
		}
		return retVal;
	}

	/**
	 * Check to see if the download of a video image capture has completed.
	 * @return true if we are finished downloading all of the bytes from a video image capture command
	 */
	public boolean isFinishedDownloadingVideo() {
		if (!isReadingVideoData()) {
			return false;//not even reading video bytes
		}
		if (m_nNumImageBytes<=0) {
			return false;//do not know how big the image is yet
		}
		int nNumDownloaded = GetNumVideoBytesDownloaded();
		//test
		String sTest = String.format("downloaded %d of %d bytes\n",nNumDownloaded,m_nNumImageBytes);
		Log.d("debug",sTest);
		//end test
		return (nNumDownloaded>=m_nNumImageBytes);
	}

	/**
	 * Call this function after the video image capture bytes have been completely downloaded and the image has been saved somewhere or displayed.
	 */
	public void StopReadingVideoData() {
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
		m_nNumImageBytes = 0;
		m_commandLock.unlock();
	}

	private int FindResyncBytes(byte []buf) {
		//look at the last part of buf to see if it contains "sync" bytes sent by AMOS, i.e.
		//see if it contains the 10-byte text sequence: "AMOSRESYNC"
		//if this sequence is found, then return the index in buf where it starts, otherwise return -1 if the sequence
		//cannot be found
		int MAX_TO_CHECK = 30;//check no more than this many bytes at the end of buf
		int nNumToCheck = Math.min(MAX_TO_CHECK, buf.length);
		int nStartIndex = buf.length - nNumToCheck;
		int nEndIndex = nStartIndex + nNumToCheck - 9;
		for (int i=nStartIndex;i<nEndIndex;i++) {
			if (buf[i]=='A'&&buf[i+1]=='M'&&buf[i+2]=='O'&&buf[i+3]=='S'&&buf[i+4]=='R'&&buf[i+5]=='E'&&buf[i+6]=='S'&&buf[i+7]=='Y'&&buf[i+8]=='N'&&buf[i+9]=='C') {
				return i;
			}
		}
		return -1;
	}

	private byte [] RemoveLastPartialChunk(byte [] buf, int nIndex) {//remove the last partial
		//data chunk in buf that occurs before the index nIndex, the return value of the function
		//is the shortened version of the byte array buf
		if (buf==null) return null;
		if (nIndex<20||m_nImageBytesOffset==0) {//this function is currently only applicable for buffers of video image capture data
			return buf;//don't do anything
		}
		int nNumPartialChunkBytes = (nIndex - m_nImageBytesOffset) % LARGEDATA_CHUNKSIZE;
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
		//clear out any old commands that were sent more than REMOTE_COMMAND.ITMEOUT_TIME_MS ago
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
		if (nNumCommands==0) {
			m_bSendingCommand = false;
		}
		else if (bCommandsRemoved) {//send the command that is now first in the queue
			//renew times of commands
			for (int i=0;i<nNumCommands;i++) {
				m_commands.elementAt(i).RenewTime();
			}
			SendCommand(m_commands.firstElement());//send out the first command in the queue
		}
		m_commandLock.unlock();
	}


}

