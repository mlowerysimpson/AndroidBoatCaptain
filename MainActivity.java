/**
 * 
 */
package com.example.boatcaptain;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Murray Lowery-Simpson Simpson's Helpful Software 2018
 */
public class MainActivity extends Activity {
	public static Context m_context;

	private int REQUEST_ENABLE_BT = 1;//code corresponding to a request to enable Bluetooth on this device

	// possible modes for timer function
	public static int STATUS_MODE = 0;// mode for getting status info: voltage,
										// temperature, etc.
	public static int HOMING_MODE = 1;// mode for calling AMOS back to the
										// location of the captain (based on
										// phone's GPS coordinates)
	public static int CANCEL_HOMING_MODE = 2;// mode for canceling a request to
												// call AMOS back home
	public static int IMAGE_MODE = 3;// mode for requesting an image capture
										// from AMOS
	private int m_nTimerMode;// before launching the timer, the program puts
								// itself in one of the above states, depending
								// on what it wants to do
	private String m_sHomingMsgStatus;// text that describes whether or not the
										// homing request was sent sucessfully
	private double m_dHomingLatitude;// latitude for homing
	private double m_dHomingLongitude;// longitude for homing

	private Button m_cStatusButton;
	private Button m_cConnectButton;
	private TextView m_cRudderAngleText;
	private TextView m_cPropSpeedText;
	private TextView m_cStatusText;
	private TextView m_cGPSLocation;
	private TextView m_cCompassData;
	private TextView m_cDiagnosticsData;
	private TextView m_cIPAddrLabel;
	private ProgressBar m_cRudderAngleProgress;
	private ProgressBar m_cPropSpeedProgress;
	private Switch m_cUseBluetooth;// checkbox for using Bluetooth connection
										// to remote control box
	private Button m_cStopButton;
	private Button m_cHomeButton;
	private Button m_cCameraButton;
	private Button m_cMapButton;
	private Button m_cForwardButton;
	private Button m_cBackButton;
	private Button m_cLeftButton;
	private Button m_cRightButton;
	private EditText m_cIPAddr;
	public static  NetCaptain m_pNetCaptain;
	public static  BluetoothCaptain m_pBluetoothCaptain;
	public static GPSTracker m_gpsTracker;
	public static String m_sDiagnosticsText="";// general diagnostic data to display

	private String m_sIPAddr;// the IP address specified in the m_cIPAddr
								// control
	private boolean m_bTimerStarted;// true once the 10-sec timer for GPS
									// position, heading, etc. has been started
	private boolean m_bShowingHomeButton = true;// flag indicates whether or not
												// the "home" button is being
												// displayed
	private Timer m_updateTimer;// timer that is used to update GPS position,
								// heading, etc.
	private String m_sGPSText="";// GPS data to display onscreen
	private String m_sCompassText="";// compass, roll, pitch data to display
									// onscreen

	private String m_sLeakText="";//leak sensor text to display
										// onscreen
	private boolean m_bUsingBluetooth = false;//set to true if we are using a Bluetooth connection (to the remote control box) as opposed to a network connection to a server for communications with AMOS.

	//Bluetooth adaptor
	private BluetoothAdapter m_bluetoothAdapter = null;
	//Bluetooth device
	private BluetoothDevice m_bluetoothDevice = null;
	//Bluetooth scanner
	private BluetoothLeScanner m_leScanner = null;
	//scan callback function
	private ScanCallback m_scanCallback = null;
	//Bluetooth Generic Attribute (Gatt) (used to handle connection to Bluetooth low energy (BLE) device connected to AMOS_REMOTE)
	private BluetoothGatt m_connectGatt = null;
    //Bluetooth Gatt Callback
    private BluetoothGattCallback m_gattCallback = null;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);
		View main_view = findViewById(R.id.activity_main_layout);
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

		m_gpsTracker = null;
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, -1);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, -1);
		layoutParams.topMargin = 50;
		m_cStatusButton = (Button) findViewById(R.id.status_button);
		m_cConnectButton = (Button) findViewById(R.id.network_connect_button);
		m_cUseBluetooth = (Switch) findViewById(R.id.use_bluetooth);
		m_cIPAddrLabel = (TextView)findViewById(R.id.boat_ipaddress_label);
		m_cRudderAngleText = (TextView) findViewById(R.id.rudder_angle_text);
		m_cPropSpeedText = (TextView) findViewById(R.id.prop_speed_text);
		m_cGPSLocation = (TextView) findViewById(R.id.gps_location);
		m_cCompassData = (TextView) findViewById(R.id.compass_data);
		m_cDiagnosticsData = (TextView) findViewById(R.id.diagnostics_data);
		m_cRudderAngleProgress = (ProgressBar) findViewById(R.id.rudder_angle_progress);
		m_cPropSpeedProgress = (ProgressBar) findViewById(R.id.prop_speed_progress);
		m_cStopButton = (Button) findViewById(R.id.stop_button);
		m_cHomeButton = (Button) findViewById(R.id.home_button);
		m_cCameraButton = (Button) findViewById(R.id.camera_button);
		m_cMapButton = (Button) findViewById(R.id.map_button);
		m_cForwardButton = (Button) findViewById(R.id.forwards_button);
		m_cLeftButton = (Button) findViewById(R.id.left_button);
		m_cRightButton = (Button) findViewById(R.id.right_button);
		m_cBackButton = (Button) findViewById(R.id.back_button);
		m_cIPAddr = (EditText) findViewById(R.id.et_ipaddr);
		m_cStatusText = (TextView) findViewById(R.id.status_text);
		m_context = this;

		addConnectNetworkListener();
		// addBluetoothConnectListener();
		addStatusButtonListener();
		addStopButtonListener();
		addCameraButtonListener();
		addForwardButtonListener();
		addLeftButtonListener();
		addRightButtonListener();
		addBackButtonListener();
		addHomeButtonListener();
		addMapButtonListener();
		addBluetoothCheckboxListener();
		GetPrefs();
		if (!isBLESupported()) {//if Bluetooth Low Energy (BLE) is not supported, we need to de-select and hide the controls for using a Bluetooth connection and make sure that the Network-related controls are shown instead
			HideBluetoothControls();
		}
		if (m_bUsingBluetooth) {
			StartBluetoothConnection();
		}

		main_view.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				Util.hideSoftKeyboard(MainActivity.this);
				return true;
			}
		});

	}

	private void addStatusButtonListener() {
		m_cStatusButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Get updated GPS, compass, etc. data in this function
				// use one-shot timer to get it in order to avoid problems with
				// running network functions on the main thread
				m_nTimerMode = STATUS_MODE;
				StartTimer();
			}
		});
	}

	private void addBluetoothCheckboxListener() {
		m_cUseBluetooth.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (m_cUseBluetooth.isChecked()) {// use Bluetooth connection to
													// remote control box
					HideNetworkControls();
					StartBluetoothConnection();
				} else {
					CloseBluetoothConnection();
					ShowNetworkControls();
				}
			}
		});
	}

	private void addConnectNetworkListener() {
		m_cConnectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String sButtonText = m_cConnectButton.getText().toString();
				sButtonText = sButtonText.toLowerCase();
				if (sButtonText.indexOf("disconnect") >= 0) {// execute
																// disconnect
																// from boat
																// function
					// disconnected from boat, i.e. destroy m_pNetCaptain object
					if (m_pNetCaptain != null) {
						if (!m_pNetCaptain.Disconnect()) {
							String sError = String.format(
									"Error disconnecting: %s.",
									m_pNetCaptain.m_sLastError);
							UpdateStatus(sError, true, false);
						}
						m_pNetCaptain = null;
						UpdateStatus("Disconnected from boat.", true, false);
					}
					m_cConnectButton.setText("Connect");
				} else {
					// change connect button to disconnect button
					m_cConnectButton.setText("Disconnect");
					SaveSettings();
					if (m_pNetCaptain == null) {
						m_pNetCaptain = new NetCaptain();
					}
					m_pNetCaptain.ConnectToBoat(m_sIPAddr);
				}
			}
		});
	}

	private void addForwardButtonListener() {
		m_cForwardButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (m_pNetCaptain != null) {
					// accelerate forwards
					m_pNetCaptain.ForwardHo();
				} else if (m_pBluetoothCaptain != null) {
					// accelerate forwards
					m_pBluetoothCaptain.ForwardHo();
				} else {
					NoConnectionErrMsg();// display message about not being
											// connected to the boat yet
				}
			}
		});
	}

	private void addLeftButtonListener() {
		m_cLeftButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (m_pNetCaptain != null) {
					// turn to left
					m_pNetCaptain.PortHo();
				} else if (m_pBluetoothCaptain != null) {
					// turn to left
					m_pBluetoothCaptain.PortHo();
				} else {
					NoConnectionErrMsg();// display message about not being
											// connected to the boat yet
				}
			}
		});
	}

	private void addRightButtonListener() {
		m_cRightButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (m_pNetCaptain != null) {
					// turn to right
					m_pNetCaptain.StarboardHo();
				} else if (m_pBluetoothCaptain != null) {
					// turn to right
					m_pBluetoothCaptain.StarboardHo();
				} else {
					NoConnectionErrMsg();// display message about not being
											// connected to the boat yet
				}
			}
		});
	}

	private void addBackButtonListener() {
		m_cBackButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (m_pNetCaptain != null) {
					// accelerate backwards
					m_pNetCaptain.BackHo();
				} else if (m_pBluetoothCaptain != null) {
					// stop thrusters
					m_pBluetoothCaptain.BackHo();
				} else {
					NoConnectionErrMsg();// display message about not being
											// connected to the boat yet
				}
			}
		});
	}

	private void addStopButtonListener() {
		m_cStopButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (m_pNetCaptain != null) {
					// stop thrusters
					m_pNetCaptain.Stop();
				} else if (m_pBluetoothCaptain != null) {
					// stop thrusters
					m_pBluetoothCaptain.Stop();
				} else {
					NoConnectionErrMsg();// display message about not being
											// connected to the boat yet
				}
			}
		});
	}

	/*
	 * private void addBluetoothConnectListener() {
	 * m_cBluetoothConnectButton.setOnClickListener(new OnClickListener() {
	 *
	 * @Override public void onClick(View v) {
	 * m_cBluetoothConnectButton.setVisibility(View.INVISIBLE);
	 * UpdateStatus("Connecting to boat over Bluetooth, please wait..."
	 * ,false,false); String sButtonText =
	 * m_cBluetoothConnectButton.getText().toString(); sButtonText =
	 * sButtonText.toLowerCase(); if (sButtonText.indexOf("disconnect")>=0)
	 * {//execute disconnect disconnect from boat function //disconnect from
	 * boat, i.e. destroy m_pBluetoothCaptain object if (this->m_timerID) {
	 * this->KillTimer(m_timerID); m_timerID=0; } if (m_pBluetoothCaptain!=null)
	 * { m_pBluetoothCaptain = null;
	 * UpdateStatus("Disconnected from boat.",true,false); }
	 * m_cBluetoothConnectButton.setText("Bluetooth Connect"); } else { //change
	 * connect button to disconnect button
	 * m_cBluetoothConnectButton.setText("Disconnect"); SaveSettings(); if
	 * (m_pBluetoothCaptain==null) { m_pBluetoothCaptain = new
	 * BluetoothCaptain(); } if (!m_pBluetoothCaptain.ConnectToBoat()) {
	 * UpdateStatus("Error, could not connect to boat.",true,true);
	 * BluetoothCommandFailed();
	 * m_cBluetoothConnectButton.setText("Bluetooth Connect");
	 * m_cBluetoothConnectButton.setVisibility(View.VISIBLE); return; }
	 * //connected to boat CString sStatusText =
	 * "Connected to boat using Bluetooth link.";
	 * UpdateStatus(sStatusText,true,false); //test if (this->m_timerID==0) {
	 * UINT_PTR timerID = 1; m_timerID = SetTimer(timerID,
	 * BLUETOOTH_TIMER_INTERVAL_MS, NULL); } //end test } }); }
	 */

	// UpdateStatus: update the status message shown in this activity and
	// display an optional message or error popup
	// sMsg = the text message to show in the main activity
	// bShowPopupMsg = true if a popup version of sMsg should also be shown
	// bError = true if the message should be presented as an error
	private void UpdateStatus(String sMsg, boolean bShowPopupMsg, boolean bError) {
		m_cStatusText.setText(sMsg);
		if (bShowPopupMsg) {
			if (bError) {// show error popup message
				SimpleMessage.msbox("Error", sMsg, this.m_context);
			} else {// show information popup message
				SimpleMessage.msbox("Info", sMsg, this.m_context);
			}
		}
	}

	private void SaveSettings() {// save configuration settings to file
		this.m_sIPAddr = this.m_cIPAddr.getText().toString();
		String sPrefsFoldername = Environment.getExternalStorageDirectory()
				+ "/AMOS";
		File fPrefsFolder = new File(sPrefsFoldername);
		if (!fPrefsFolder.exists()) {
			// try to create data folder
			try {
				boolean bMadeDir = fPrefsFolder.mkdirs();
				if (!bMadeDir) {
					String sErrMsg = String.format(
							"Error: trying to create data folder: %s",
							fPrefsFolder.getName());
					SimpleMessage.msbox("Error", sErrMsg, this);
					return;
				}
			} catch (Exception e) {
				String sError = String.format(
						"Error %s trying to create data folder: %s",
						e.toString(), sPrefsFoldername);
				SimpleMessage.msbox("Error", sError, this);
			}
		}
		String sPrefsFilename = fPrefsFolder.getPath() + "/prefs.cfg";
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(sPrefsFilename);// creates (or overwrites)
													// prefs.cfg file
		} catch (FileNotFoundException e) {
			String sError = "Error: %s trying to create prefs.cfg file."
					+ e.toString();
			this.UpdateStatus(sError, true, true);
			return;
		}

		pw.print("[prefs]\r\n");
		pw.printf("remote_ip_addr %s\r\n", m_sIPAddr);
		pw.print("\n");
		pw.close();
	}

	private void NetCommandFailed() {// a network command has failed... adjust
										// interface accordingly
		// delete current network captain
		if (m_pNetCaptain != null) {
			m_pNetCaptain = null;
		}
		m_cConnectButton.setText("Connect");
	}

	private void StartTimer() {
		m_bTimerStarted = true;
		m_updateTimer = new Timer();
		m_updateTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				TimerMethod();
			}
		}, 0);// just schedule for one-time operation, immediately
	}

	private void TimerMethod() {
		// This method is called directly by the timer
		// and runs in the same thread as the timer.

		// We call the method that will work with the UI
		// through the runOnUiThread method.

		if (m_nTimerMode == STATUS_MODE) {
			RequestStatus();
		} else if (m_nTimerMode == HOMING_MODE) {
			RequestHoming();
		} else if (m_nTimerMode == CANCEL_HOMING_MODE) {
			RequestCancelHoming();
		}
		if (this.m_pNetCaptain!=null) {
			this.runOnUiThread(Timer_Tick);
		}
	}

	private Runnable Timer_Tick = new Runnable() {
		public void run() {
			if (m_nTimerMode == STATUS_MODE) {
				m_cGPSLocation.setText(m_sGPSText);
				m_cCompassData.setText(m_sCompassText);
				m_cDiagnosticsData.setText(m_sDiagnosticsText);
				String sStatusText = m_sGPSText + "\n" + m_sCompassText + "\n"
						 + m_sDiagnosticsText;
				SimpleMessage.msbox("Status", sStatusText, m_context);
			} else if (m_nTimerMode == HOMING_MODE
					|| m_nTimerMode == CANCEL_HOMING_MODE) {
				if (m_sHomingMsgStatus.length() > 0) {
					if (m_sHomingMsgStatus.indexOf("Error") >= 0) {
						UpdateStatus(m_sHomingMsgStatus, true, true);
					} else {
						UpdateStatus(m_sHomingMsgStatus, true, false);
					}
				}
			}
		}
	};

	// UpdateSpeedAndAngle: updates the text and progress bar controls for the
	// propeller speed and rudder angle
	private void UpdateSpeedAndAngle() {
		PROPELLER_STATE pPropState = null;
		Captain pCaptain = GetCaptain();
		if (pCaptain != null) {
			pPropState = pCaptain.GetCurrentPropState();
		}
		float fRudderAngle = 0;// angle of air rudder in degrees
		float fPropSpeed = 0;// speed of air propeller (arbitrary units)
		if (pPropState != null) {
			fRudderAngle = pPropState.fRudderAngle;
			fPropSpeed = pPropState.fPropSpeed;
		}

		// set progress bar controls (assume default of 100 divisions)
		int nRudderAngleVal = (int) (50 + 50 * fRudderAngle
				/ PROPELLER_STATE.MAX_ANGLE);
		int nSpeedVal = (int) (100 * fPropSpeed / REMOTE_COMMAND.MAX_SPEED);
		m_cRudderAngleProgress.setProgress(nRudderAngleVal);
		m_cPropSpeedProgress.setProgress(nSpeedVal);
		String sRudderAngleText = "", sPropSpeedText = "";
		sRudderAngleText = String.format("R. Angle: %.1f", fRudderAngle);
		sPropSpeedText = String.format("Prop Speed: %.1f", fPropSpeed);
		m_cRudderAngleText.setText(sRudderAngleText);
		m_cPropSpeedText.setText(sPropSpeedText);
	}

	private Captain GetCaptain() {
		if (m_pNetCaptain != null) {
			return (Captain) m_pNetCaptain;
		} else if (m_pBluetoothCaptain != null) {
			return (Captain) m_pBluetoothCaptain;
		}
		return null;
	}

	private void BluetoothCommandFailed() {// a Bluetooth command has failed...
											// adjust interface accordingly
		// don't do anything
	}

	private void NoConnectionErrMsg() {// display message about not being
										// connected to the boat yet
		UpdateStatus("Error, not connected to boat yet.", true, true);
	}

	private void GetPrefs() {
		// get program preferences
		// m_sLastUsedDeviceName
		String sPrefsFilename = Environment.getExternalStorageDirectory()
				+ "/AMOS/prefs.cfg";
		File fPrefsFile = new File(sPrefsFilename);
		if (!fPrefsFile.exists()) {
			return;
		}
		filedata prefsFile = new filedata(sPrefsFilename);
		m_sIPAddr = prefsFile.getString("[prefs]", "remote_ip_addr");
		m_cIPAddr.setText(m_sIPAddr);
	}

	// BoatTaskCompleted: display message corresponding to an asynchronous task
	// (typically involving communications with boat) that has just completed
	// nTask = the type of task that has just completed (either successfully or
	// with an error
	// result = integer code corresponding to whether or not the task was
	// successful, = 0 if successful, < 0 if an error occurred
	public void BoatTaskCompleted(int nTask, int result) {
		if (nTask == BoatTask.CONNECT_TASK) {
			if (result == BoatTask.COMPLETED_OK) {
				if (this.m_pNetCaptain == null) {
					return;
				}
				String sBoatConnectMsg = String.format(
						"Connected to boat on %s", m_pNetCaptain.m_sBoatIPAddr);
				this.UpdateStatus(sBoatConnectMsg, true, false);
				// test
				// if (!m_bTimerStarted) {
				// StartTimer();
				// }
				// end test
			} else {// some sort of error occured
				String sErrorMsg = String.format(
						"Error trying to connect to boat: %s",
						m_pNetCaptain.m_sLastError);
				this.UpdateStatus(sErrorMsg, true, true);
				// change text of connect button back to "Connect"
				this.m_cConnectButton.setText("Connect");
			}
		} else if (nTask == BoatTask.NETWORK_COMMAND_TASK) {
			if (this.m_pNetCaptain == null) {
				return;
			}
			if (result == BoatTask.ERROR_COMMAND_FAILED) {// an error occurred
															// trying to send a
															// network command
				String sErrorMsg = String.format(
						"Error occurred trying to send network command: %s",
						m_pNetCaptain.m_sLastError);
				this.UpdateStatus(sErrorMsg, true, true);
			} else {// command was successful, so update screen with new state
				this.UpdateSpeedAndAngle();
			}
		}
	}

	private void addHomeButtonListener() {
		m_cHomeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (m_pNetCaptain != null||m_pBluetoothCaptain != null) {
					if (m_bShowingHomeButton) {// home button was pressed
						// get current GPS coordinates from phone
						int MAX_NUM_ATTEMPTS = 60;// approx. 1 second delay
													// between attempts
						m_sHomingMsgStatus = "";
						if (m_gpsTracker == null) {
							m_gpsTracker = new GPSTracker(m_context, 3, 10000);
						}
						int nGPSAttemptCount = 0;
						Location captainLocation = m_gpsTracker.getLocation();
						while (captainLocation == null
								&& nGPSAttemptCount < MAX_NUM_ATTEMPTS) {
							try {
								Thread.sleep(1000);
							} catch (Exception e) {
								// do nothing
							}
							nGPSAttemptCount++;
							captainLocation = m_gpsTracker.getLocation();
						}
						if (captainLocation == null)
							return;
						m_dHomingLatitude = captainLocation.getLatitude();
						m_dHomingLongitude = captainLocation.getLongitude();

						// changed to "cancel home" button
						m_bShowingHomeButton = false;
						m_cHomeButton.setBackgroundResource(R.drawable.xmark);
						m_nTimerMode = HOMING_MODE;
						StartTimer();
					} else {// cancel home function button was pressed
						m_bShowingHomeButton = true;
						m_cHomeButton.setBackgroundResource(R.drawable.house);
						// send command to boat to cancel the current GPS
						// destination (i.e. instruction to go home)
						m_nTimerMode = CANCEL_HOMING_MODE;
						StartTimer();
					}
				} else {
					NoConnectionErrMsg();// display message about not being
											// connected to the boat yet
				}
			}
		});
	}

	private void RequestStatus() {// request basic status information from AMOS
		String sGPSPosition = null;
		String sCompassData = null;
		String sVoltageData = null;
		String sDiagnosticData = null;
		if (m_pNetCaptain != null) {
			sGPSPosition = m_pNetCaptain.RequestGPSPosition();
			sCompassData = m_pNetCaptain.RequestCompassData();
			sVoltageData = m_pNetCaptain.RequestVoltageData();
			sDiagnosticData = m_pNetCaptain.RequestDiagnosticsData();
		}
		else if (m_pBluetoothCaptain!=null) {
			m_pBluetoothCaptain.RequestGPSPosition();
			return;//need to request each status item one at a time, waiting for a response before getting the next status item
		}
		if (sGPSPosition != null) {
			this.m_sGPSText = "GPS Position: " + sGPSPosition;
		} else {
			m_sGPSText = "GPS Position: N.A.";
		}
		if (sCompassData != null) {
			this.m_sCompassText = sCompassData;
		} else {
			m_sCompassText = "Heading = N.A., Roll = N.A., Pitch = N.A., Temp = N.A.";
		}

		if (sDiagnosticData != null) {
			this.m_sDiagnosticsText = sDiagnosticData;
		} else {
			m_sDiagnosticsText = "Diagnostics = N.A.";
		}
	}

	private void RequestHoming() {
		// send command to boat to return to the captain's location
		if (m_pNetCaptain == null&&m_pBluetoothCaptain==null) {
			NoConnectionErrMsg();// display message about not being connected to
									// the boat yet
			return;
		}

		if (m_pNetCaptain!=null) {
			if (m_pNetCaptain.ReturnHome(m_dHomingLatitude, m_dHomingLongitude)) {
				m_sHomingMsgStatus = String
						.format("Command sent succesfully to bring boat home to this location: %.6f, %.6f",
								m_dHomingLatitude, m_dHomingLongitude);
				// UpdateStatus(sSuccessfulCommand,true,false);
			} else {
				m_sHomingMsgStatus = String.format(
						"Error sending command to boat: %s",
						m_pNetCaptain.m_sLastError);
				// UpdateStatus(sErrorSendingCommand,true,true);
			}
		}
		else if (m_pBluetoothCaptain!=null) {
			m_pBluetoothCaptain.ReturnHome(m_dHomingLatitude, m_dHomingLongitude);
		}

	}

	void RequestCancelHoming() {
		// send command to boat to cancel a previously sent command to return
		// "home"
		if (m_pNetCaptain == null&&m_pBluetoothCaptain==null)
			return;
		if (m_pNetCaptain!=null) {
			if (m_pNetCaptain.CancelHome()) {
				m_sHomingMsgStatus = "Command sent succesfully to cancel homing.";
			} else {
				m_sHomingMsgStatus = String.format(
						"Error sending command to boat: %s",
						m_pNetCaptain.m_sLastError);
			}
		}
		else if (m_pBluetoothCaptain!=null) {
			m_pBluetoothCaptain.CancelHome();
		}
	}

	private void addCameraButtonListener() {
		m_cCameraButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (m_pNetCaptain == null&&m_pBluetoothCaptain==null) {
					NoConnectionErrMsg();// display message about not being
											// connected to the boat yet
					return;
				}
				//m_nTimerMode = IMAGE_MODE;
				//StartTimer();
				RequestImage();
			}
		});
	}

	private void addMapButtonListener() {
		m_cMapButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				ShowMap();
			}
		});

	}

	private void RequestImage() {
		if (m_pNetCaptain == null&&m_pBluetoothCaptain==null) {
			return;
		}
		// request image capture from AMOS
		Intent intent = new Intent(MainActivity.this,
				ImageCapActivity.class);
		//intent.putExtra("image_filename", m_sImageFilename);
		startActivity(intent);
		//m_sImageFilename = m_pNetCaptain.RequestVideoImage(65536);// 65536 is
																	// code for
																	// getting
																	// image
																	// without
																	// any
																	// feature
																	// detection
																	// stuff
	}

	private void HideNetworkControls() {// hide the network-related controls
										// (when a Bluetooth connection to the
										// remote control box is used instead)
		m_cIPAddr.setVisibility(View.INVISIBLE);
		m_cIPAddrLabel.setVisibility(View.INVISIBLE);
		m_cConnectButton.setVisibility(View.INVISIBLE);
	}

	private void ShowNetworkControls() {// show the network-related controls, i.e. when a Bluetooth connection[ to the remote control box is NOT being used.
		m_cIPAddr.setVisibility(View.VISIBLE);
		m_cIPAddrLabel.setVisibility(View.VISIBLE);
		m_cConnectButton.setVisibility(View.VISIBLE);
	}

	private void HideBluetoothControls() {//hide the Bluetooth-related controls, ex: when Bluetooth is turnd off on the device or not supported
		//also uncheck the Bluetooth checkbox
		m_cUseBluetooth.setChecked(false);
		m_cUseBluetooth.setVisibility(View.INVISIBLE);
	}

	private void ShowBluetoothControls() {
		m_cUseBluetooth.setVisibility(View.VISIBLE);
	}

	private boolean isBLESupported() {//checks to see if BLE is supported tries to get a Bluetooth Adapter and checks to see if Bluetooth is currently enabled, if not shows a message warning user that it is not available
		//see: https://developer.android.com/guide/topics/connectivity/bluetooth-le#java
		// Use this check to determine whether BLE is supported on the device. Then
		// you can selectively disable BLE-related features.
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
		    Toast toast = Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG);
		    toast.show();
		    return false;
		}
		return true;//BLE is supported on this device
	}

	void StartBluetoothConnection() {//does initialization stuff for getting a Bluetooth connection to the remote control box that talks to AMOS over a long-distance wireless link.
		//create Bluetooth captain object
		if (m_pBluetoothCaptain==null) {
			m_pBluetoothCaptain = new BluetoothCaptain();
			//get rid of network captain (if present)
			m_pNetCaptain = null;
		}
		//initialize the Bluetooth adapter
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		m_bluetoothAdapter = bluetoothManager.getAdapter();
		//check to see whether or not Bluetooth is enabled
		// Ensures Bluetooth is available on the device and it is enabled. If not,
		// displays a dialog requesting user permission to enable Bluetooth.
		if (m_bluetoothAdapter == null || !m_bluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		ShowBluetoothControls();
		if (m_cUseBluetooth.isChecked()) {//using Bluetooth connection to remote box,
			StartBLEScan();//start scan for the BLE device used for communication with the remote box
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode==REQUEST_ENABLE_BT) {
			//Bluetooth might have been enabled by user
			if (resultCode==RESULT_OK) {
				StartBluetoothConnection();
			}
			else {
				HideBluetoothControls();
				ShowNetworkControls();
			}

		}
	}




	private void StartBLEScan() {//start scan for the BLE device used for communication with the remote box

		List <ScanFilter> ble_filter = Arrays.asList(new ScanFilter[]{
				new ScanFilter.Builder().setDeviceName("AMOS_REMOTE").build()});
		m_leScanner = m_bluetoothAdapter.getBluetoothLeScanner();
		if (m_leScanner!=null) {
			ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
			m_scanCallback = new ScanCallback() {
				@Override
				public void onScanResult(int callbackType, ScanResult result) {
					super.onScanResult(callbackType, result);
					//test
					int nRSSI = result.getRssi();
					m_bluetoothDevice = result.getDevice();
					if (m_bluetoothDevice!=null) {
						if (m_connectGatt==null) {
							try {
                                if (m_gattCallback==null) {
                                    CreateGattCallback();
                                }
                                Toast toast = Toast.makeText(MainActivity.this, R.string.ble_connecting, Toast.LENGTH_LONG);
                                toast.show();
								m_connectGatt = m_bluetoothDevice.connectGatt(MainActivity.this,false,m_gattCallback);
								m_leScanner.stopScan(m_scanCallback);
								toast = Toast.makeText(MainActivity.this, R.string.ble_connected, Toast.LENGTH_LONG);
								toast.show();
							} catch (Exception e) {
								//error trying to get bluetooth socket
								Toast toast = Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG);
								toast.show();
							}
						}
					}
					//end test
				}
			};
			m_leScanner.startScan(ble_filter, settings, m_scanCallback);
		}

	}

	private void CloseBluetoothConnection() {//stop using Bluetooth
        if (m_connectGatt!=null) {
            m_connectGatt.close();
            m_connectGatt = null;
        }
        m_pBluetoothCaptain=null;
        if (m_pNetCaptain==null) {
        	m_pNetCaptain = new NetCaptain();
		}
	}

    private void CreateGattCallback() {
	    if (m_gattCallback!=null) return;
	    m_gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (status==BluetoothGatt.GATT_SUCCESS) {
                    if (newState== BluetoothProfile.STATE_CONNECTED) {//connected to BLE Gatt Server on AMOS remote control box
                    	m_pBluetoothCaptain.SetConnected(true,m_connectGatt);
                        //Toast toast = Toast.makeText(MainActivity.this, R.string.ble_connected, Toast.LENGTH_LONG);
                        //toast.show();
                    }
                    else if (newState==BluetoothProfile.STATE_DISCONNECTED) {
                    	m_pBluetoothCaptain.SetConnected(false,m_connectGatt);
                        //Toast toast = Toast.makeText(MainActivity.this, R.string.ble_disconnected, Toast.LENGTH_LONG);
                        //toast.show();
                     }

                }
            }

            @Override
			public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            	if (status==BluetoothGatt.GATT_SUCCESS) {
            		m_pBluetoothCaptain.ServicesDiscovered(gatt);
				}
			}

			@Override
			public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            	if (status==BluetoothGatt.GATT_SUCCESS) {
            		m_pBluetoothCaptain.SentOK();//confirmation that send operation completed successfully
				}
			}

			@Override
			public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            	if (status==BluetoothGatt.GATT_SUCCESS) {
            		m_pBluetoothCaptain.SentDone();//bytes have been sent out, time to start reading in the response from AMOS
				}
			}

			@Override
			public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
				//characteristic has changed, read it to get data / find out what happened
				byte[] testBytes = characteristic.getValue();
				if (m_pBluetoothCaptain.DataRead(testBytes)) {
					//data was read in successfully, so update speed and angle state (on main thread)
					MainActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							MainActivity.this.UpdateBoatStatus(m_pBluetoothCaptain.m_pBoatData);
						}
					});
				}
				else if (m_pBluetoothCaptain.isReadingLargeChunkData()) {
					if (m_pBluetoothCaptain.m_pBoatData!=null) {
						if (m_pBluetoothCaptain.m_pBoatData.nPacketType==REMOTE_COMMAND.VIDEO_DATA_PACKET) {
							MainActivity.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									ImageCapActivity.UpdateDownloadStatus(m_pBluetoothCaptain.GetNumLargeChunkBytesDownloaded(), m_pBluetoothCaptain.m_nNumLargeBlockBytes);
								}
							});
						}
						else if (m_pBluetoothCaptain.m_pBoatData.nPacketType==REMOTE_COMMAND.FILE_RECEIVE) {
							MainActivity.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									DownloadFilesActivity.UpdateDownloadStatus(m_pBluetoothCaptain.GetNumLargeChunkBytesDownloaded(), m_pBluetoothCaptain.m_nNumLargeBlockBytes);
								}
							});
						}
					}
				}
				if (m_pBluetoothCaptain.isFinishedDownloadingLargeChunk()) {
					//test
					Log.d("debug","finished downloading large data chunk.\n");
					//end test
					MainActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (m_pBluetoothCaptain.m_nLargePacketTypeDownloaded==REMOTE_COMMAND.VIDEO_DATA_PACKET) {
								ImageCapActivity.DisplayDownloadedImageBytes(m_pBluetoothCaptain.GetLargeChunkBytes());
								m_pBluetoothCaptain.StopReadingLargeChunkData();
							}
							else if (m_pBluetoothCaptain.m_nLargePacketTypeDownloaded==REMOTE_COMMAND.LIST_REMOTE_DATA) {
								byte []largeChunkBytes = m_pBluetoothCaptain.GetLargeChunkBytes();
								if (largeChunkBytes==null) {
									UpdateStatus("Error, no data files available on AMOS.", true, true);
									return;
								}
								m_pBluetoothCaptain.m_sRemoteDataAvailable = new String(largeChunkBytes);
								List<String> remoteDataFiles = Util.SplitStrToList(m_pBluetoothCaptain.m_sRemoteDataAvailable, "\r|\n");
								m_pBluetoothCaptain.StopReadingLargeChunkData();
								if (remoteDataFiles==null||remoteDataFiles.size() == 0)
								{
									UpdateStatus("No data files are available on AMOS.", true, true);
									return;
								}
								else
								{
									//show activity for downloading files
									Intent intent = new Intent(MainActivity.this,
											DownloadFilesActivity.class);
									intent.putExtra("download_files", remoteDataFiles.toArray());
									intent.putExtra("download_type", REMOTE_COMMAND.LIST_REMOTE_DATA);
									startActivity(intent);
								}
							}
							else if (m_pBluetoothCaptain.m_nLargePacketTypeDownloaded==REMOTE_COMMAND.FILE_RECEIVE) {
								DownloadFilesActivity.SaveDownloadedFileBytes(m_pBluetoothCaptain.GetLargeChunkBytes());
								m_pBluetoothCaptain.StopReadingLargeChunkData();
							}
						}
					});
				}
			}
        };
    }

	/**
	 * Update the UI with info about the boat's current status, i.e. rudder angle and thrust power, gps, temperatures, etc.
	 * @param boatData the data from the boat, gps data, humidity, power status, etc. If boatData is null, then the rudder angle and direction info will be updated on the screen.
	 *
	 */
	public void UpdateBoatStatus(BOAT_DATA boatData) {//
		String sDiagnosticsText="";
		if (boatData==null) {
			this.UpdateSpeedAndAngle();
			return;
		}
		if (boatData.nPacketType==REMOTE_COMMAND.GPS_DATA_PACKET) {//gps data
			m_sGPSText = m_pBluetoothCaptain.FormatGPSData();
			m_pBluetoothCaptain.RequestCompassData();
		}
		else if (boatData.nPacketType==REMOTE_COMMAND.COMPASS_DATA_PACKET) {//compass data
			this.m_sCompassText = m_pBluetoothCaptain.FormatCompassData();
			m_pBluetoothCaptain.RequestLeakData();
		}
		else if (boatData.nPacketType==REMOTE_COMMAND.LEAK_DATA_PACKET) {//leak sensor data
			//test
			Log.d("debug","got leak data.\n");
			//end test
			this.m_sLeakText = m_pBluetoothCaptain.FormatLeakData();
			m_pBluetoothCaptain.RequestDiagnosticsData();
		}
		else if (boatData.nPacketType==REMOTE_COMMAND.DIAGNOSTICS_DATA_PACKET) {//diagnostics data
			//test
			Log.d("debug","got diagnostics data.\n");
			//end test
			sDiagnosticsText = m_pBluetoothCaptain.FormatDiagnosticsData();
		}
		else if (BluetoothCaptain.IsLargePacketType(boatData.nPacketType)) {//just received complete large data packet
			return;//do nothing here
		}
		m_cGPSLocation.setText(m_sGPSText);
		m_cCompassData.setText(m_sCompassText);

		if (m_sLeakText.length()>0&&sDiagnosticsText.length()>0) {
			m_sDiagnosticsText = String.format("%s, %s",m_sLeakText,sDiagnosticsText);
		}
		else if (m_sLeakText.length()>0) {
			m_sDiagnosticsText = m_sLeakText;
		}
		else if (sDiagnosticsText.length()>0) {
			m_sDiagnosticsText = sDiagnosticsText;
		}
		m_cDiagnosticsData.setText(m_sDiagnosticsText);
	}

	private void ShowMap() {
		// show graphical map view
		Intent intent = new Intent(MainActivity.this,
				MapActivity.class);
		//intent.putExtra("image_filename", m_sImageFilename);
		startActivity(intent);

	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.boatcaptain_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle menu item selection
		switch (item.getItemId()) {
			case R.id.open_datafile:
				DownloadDataFile();
				return true;
			case R.id.open_depthfile:

				return true;

			case R.id.download_datafile:
				DownloadDataFile();
				return true;

			case R.id.download_logfiles:

				return true;

			case R.id.download_imagefiles:

				return true;

			case R.id.remote_amos_programinfo:

				return true;

			case R.id.submenu_alarms:

				return true;

			case R.id.submenu_camera:

				return true;

			case R.id.submenu_lidar:

				return true;

			case R.id.submenu_sensors:

				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void DownloadDataFile() {
		if (m_pNetCaptain == null&&m_pBluetoothCaptain==null) {
			NoConnectionErrMsg();// display message about not being
			// connected to the boat yet
			return;
		}

		List<String> remoteDataFiles = null;
		if (m_pNetCaptain!=null) {
			remoteDataFiles = m_pNetCaptain.GetRemoteDataFiles();
			if (remoteDataFiles==null||remoteDataFiles.size() == 0)
			{
				UpdateStatus("No data files are available on AMOS.", true, true);
				return;
			}
			else
			{
				//show activity for downloading files
				Intent intent = new Intent(MainActivity.this,
						DownloadFilesActivity.class);
				intent.putExtra("download_files", remoteDataFiles.toArray());
				intent.putExtra("download_files", REMOTE_COMMAND.LIST_REMOTE_DATA);
				startActivity(intent);
			}
		}
		else {
			m_pBluetoothCaptain.GetRemoteDataFiles();
		}
	}
}