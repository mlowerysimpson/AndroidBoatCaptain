package com.example.boatcaptain;

import android.app.DownloadManager;
import android.content.Context;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.internal.jni.CoreRGBColor;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MapActivity extends AppCompatActivity {
    public static Context m_context;
    private static long GPS_TIMER_PERIOD_MS = 5000;//the update rate of the timer in ms
    private static int MAX_COMMAND_COUNT = 3;

    private TextView m_cDiagnosticsData;
    private TextView m_cStatus;
    private TextView m_cRudderAngle;
    private TextView m_cPropSpeed;
    private Button m_cForwardButton;
    private Button m_cBackButton;
    private Button m_cLeftButton;
    private Button m_cRightButton;
    private Button m_cStopButton;

    private MapView m_mapView;
    private ArcGISMap m_map;
    private Graphic m_legsGraphic;
    private Graphic m_armsGraphic;
    private Graphic m_torsoGraphic;
    private Graphic m_shipGraphic;
    private double m_dPhoneLatitude;
    private double m_dPhoneLongitude;
    private double m_dBoatLatitude;
    private double m_dBoatLongitude;
    private double m_dBoatHeading;

    private int m_nCommandCount;//keeps track of number of commands issued, to avoid conflicts with timed requests for data

    //GPS timer variables
    boolean m_bGPSTimerStarted;
    Timer m_updateGPSTimer;

    private GraphicsOverlay m_graphicsOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_view);
        getActionBar().setIcon(R.drawable.robot_only);

        m_cForwardButton = (Button) findViewById(R.id.map_up_button);
        m_cBackButton = (Button) findViewById(R.id.map_back_button);
        m_cLeftButton = (Button) findViewById(R.id.map_left_button);
        m_cRightButton = (Button) findViewById(R.id.map_right_button);
        m_cStopButton = (Button) findViewById(R.id.map_stop_button);
        m_cStatus = (TextView) findViewById(R.id.map_status_text);
        m_cDiagnosticsData = (TextView) findViewById(R.id.map_diagnostics_data);
        m_cPropSpeed = (TextView) findViewById(R.id.map_prop_speed_text);
        m_cRudderAngle = (TextView) findViewById(R.id.map_rudder_angle_text);

        m_context = this;

        addForwardButtonListener();
        addLeftButtonListener();
        addRightButtonListener();
        addBackButtonListener();
        addStopButtonListener();


        // authentication with an API key or named user is required to access basemaps and other
        // location services
        ArcGISRuntimeEnvironment.setApiKey(BuildConfig.API_KEY);
        // inflate MapView from layout
        m_mapView = findViewById(R.id.mapView);
        //get current location from phone / tablet
        // get current GPS coordinates from phone
        GetPhonePosition();

        // create a map with Topographic Basemap
        m_map = new ArcGISMap(Basemap.Type.IMAGERY_WITH_LABELS, m_dPhoneLatitude, m_dPhoneLongitude, 16);
        // set the map to be displayed in this view
        m_mapView.setMap(m_map);

        m_graphicsOverlay = new GraphicsOverlay();

        // add the graphic overlay to the map view
        m_mapView.getGraphicsOverlays().add(m_graphicsOverlay);

        //add listener to know when map has finished loading
        m_map.addDoneLoadingListener(() -> {
            if (m_map.getLoadStatus() == LoadStatus.LOADED) {
                DrawStickFigure();
                StartGPSTimer();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        m_mapView.resume();
        StartGPSTimer();
    }

    @Override
    protected void onPause() {
        StopGPSTimer();
        super.onPause();
        m_mapView.pause();
    }

    @Override
    protected void onDestroy() {
        StopGPSTimer();
        super.onDestroy();
        m_mapView.dispose();
    }


    private void DrawStickFigure() {
        //draw simple stick figure that corresponds to the location of the person holding viewing their phone and viewing the map data
        double LENGTH_SCALE_FACTOR = 0.06;
        double WIDTH_SCALE_FACTOR = 0.04;

        SpatialReference srMap = m_mapView.getSpatialReference();

        Polygon polyVisArea = m_mapView.getVisibleArea();
        if (polyVisArea == null)
        {
            return;
        }
        Envelope mapEnvelope = polyVisArea.getExtent();
        double dPosWidth = mapEnvelope.getXMax() - mapEnvelope.getXMin();
        double PERSON_HEIGHT = dPosWidth * LENGTH_SCALE_FACTOR;
        double ARM_WIDTH = dPosWidth * WIDTH_SCALE_FACTOR;
        double ARM_HEIGHT = PERSON_HEIGHT*.75;
        double HIP_WIDTH = ARM_WIDTH/2;
        double HIP_HEIGHT = PERSON_HEIGHT*.4;

        //scale stick figure length and width according to current map dimensions
        Point bp = Util.WGS84ToSR(m_dPhoneLatitude, m_dPhoneLongitude, srMap);//base point
        double dBaseX = bp.getX();
        double dBaseY = bp.getY();
        //define coordinatess of stick figure vertices, in [East, North] order
        Point pt1 = new Point(HIP_WIDTH/2 + dBaseX, dBaseY, SpatialReferences.getWebMercator());//right foot
        Point pt2 = new Point(-HIP_WIDTH/2 + dBaseX, dBaseY, SpatialReferences.getWebMercator());//left foot
        Point pt3 = new Point(HIP_WIDTH/2 + dBaseX, HIP_HEIGHT + dBaseY, SpatialReferences.getWebMercator());//right hip
        Point pt4 = new Point(-HIP_WIDTH/2 + dBaseX, HIP_HEIGHT + dBaseY, SpatialReferences.getWebMercator());//left hip
        Point pt5 = new Point(dBaseX, HIP_HEIGHT + dBaseY, SpatialReferences.getWebMercator());//pelvis
        Point pt6 = new Point(dBaseX, PERSON_HEIGHT + dBaseY, SpatialReferences.getWebMercator());//head
        Point pt7 = new Point(ARM_WIDTH/2 + dBaseX, ARM_HEIGHT + dBaseY, SpatialReferences.getWebMercator());//right hand
        Point pt8 = new Point(-ARM_WIDTH/2 + dBaseX, ARM_HEIGHT + dBaseY, SpatialReferences.getWebMercator());//left hand

        //legs
        PointCollection legs = new PointCollection(SpatialReferences.getWebMercator());
        legs.add(pt1);
        legs.add(pt3);
        legs.add(pt4);
        legs.add(pt2);
        Polyline legs_p = new Polyline(legs);

        //arms
        PointCollection arms = new PointCollection(SpatialReferences.getWebMercator());
        arms.add(pt8);
        arms.add(pt7);
        Polyline arms_p = new Polyline(arms);

        //torso and head
        PointCollection torso = new PointCollection(SpatialReferences.getWebMercator());
        torso.add(pt5);
        torso.add(pt6);
        Polyline torso_p = new Polyline(torso);

        //remove existing polyline stick figure graphics (if any)
        if (m_legsGraphic != null)
        {
            m_graphicsOverlay.getGraphics().remove(m_legsGraphic);
        }
        if (m_armsGraphic!=null)
        {
            m_graphicsOverlay.getGraphics().remove(m_armsGraphic);
        }
        if (m_torsoGraphic!=null)
        {
            m_graphicsOverlay.getGraphics().remove(m_torsoGraphic);
        }

        //Create symbol for polyline
        SimpleLineSymbol polylineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xffffffff, 3);//white stick figure

        //Create polyline graphics
        m_legsGraphic = new Graphic(legs_p, polylineSymbol);
        m_armsGraphic = new Graphic(arms_p, polylineSymbol);
        m_torsoGraphic = new Graphic(torso_p, polylineSymbol);

        //Add polylines to graphics overlay
        m_graphicsOverlay.getGraphics().add(m_legsGraphic);
        m_graphicsOverlay.getGraphics().add(m_armsGraphic);
        m_graphicsOverlay.getGraphics().add(m_torsoGraphic);
    }

    private void DrawBoatShape() {
        //draw simple stick figure that corresponds to the location of the person holding viewing their phone and viewing the map data
        double LENGTH_SCALE_FACTOR = 0.06;
        double WIDTH_SCALE_FACTOR = 0.04;

        SpatialReference srMap = m_mapView.getSpatialReference();

        Polygon polyVisArea = m_mapView.getVisibleArea();
        if (polyVisArea == null)
        {
            return;
        }
        Envelope mapEnvelope = polyVisArea.getExtent();
        double dPosWidth = mapEnvelope.getXMax() - mapEnvelope.getXMin();

        //scale boat length and width according to current map dimensions
        double BOAT_LENGTH = dPosWidth * LENGTH_SCALE_FACTOR;
        double BOAT_WIDTH = dPosWidth * WIDTH_SCALE_FACTOR;

        Point pt = Util.WGS84ToSR(m_dBoatLatitude, m_dBoatLongitude, srMap);//center point of the boat
        double dBaseX = pt.getX();
        double dBaseY = pt.getY();
        //define local coordinates of boat vertices, in [East, North] order
        Point pt1 = new Point(BOAT_WIDTH / 2, -BOAT_LENGTH / 2);
        Point pt2 = new Point(BOAT_WIDTH / 2, -BOAT_LENGTH / 2 + BOAT_WIDTH);
        Point pt3 = new Point(0.0, BOAT_LENGTH / 2);
        Point pt4 = new Point(-BOAT_WIDTH / 2, -BOAT_LENGTH / 2 + BOAT_WIDTH);
        Point pt5 = new Point(-BOAT_WIDTH / 2, -BOAT_LENGTH / 2);

        //rotate boat points by the heading of the boat
        pt1 = Util.RotatePt(pt1, m_dBoatHeading);
        pt2 = Util.RotatePt(pt2, m_dBoatHeading);
        pt3 = Util.RotatePt(pt3, m_dBoatHeading);
        pt4 = Util.RotatePt(pt4, m_dBoatHeading);
        pt5 = Util.RotatePt(pt5, m_dBoatHeading);

        //add on center point to points
        pt1 = new Point(pt1.getX() + dBaseX, pt1.getY() + dBaseY);
        pt2 = new Point(pt2.getX() + dBaseX, pt2.getY() + dBaseY);
        pt3 = new Point(pt3.getX() + dBaseX, pt3.getY() + dBaseY);
        pt4 = new Point(pt4.getX() + dBaseX, pt4.getY() + dBaseY);
        pt5 = new Point(pt5.getX() + dBaseX, pt5.getY() + dBaseY);

        //boat point collection
        PointCollection boat_pc = new PointCollection(SpatialReferences.getWebMercator());
        boat_pc.add(pt1);
        boat_pc.add(pt2);
        boat_pc.add(pt3);
        boat_pc.add(pt4);
        boat_pc.add(pt5);
        boat_pc.add(pt1);//close loop back at pt1
        Polyline boat_p = new Polyline(boat_pc);

        //remove existing polyline ship graphics (if any)
        if (m_shipGraphic!=null) {
            m_graphicsOverlay.getGraphics().remove(m_shipGraphic);
        }

        //Create symbol for polyline
        SimpleLineSymbol polylineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xffffff00, 3);//yellow boat outline

        //Create polyline graphics
        m_shipGraphic = new Graphic(boat_p, polylineSymbol);

        //Add polylines to graphics overlay
        m_graphicsOverlay.getGraphics().add(m_shipGraphic);
    }

    private void StartGPSTimer() {
        m_bGPSTimerStarted = true;
        if (m_updateGPSTimer==null) {
            m_updateGPSTimer = new Timer();
            m_updateGPSTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    GPSTimerMethod();
                }
            }, 0, GPS_TIMER_PERIOD_MS);//schedule for updates every GPS_TIMER_PERIOD_MS milliseconds
        }
    }

    private void GPSTimerMethod() {
        // This method is called directly by the timer
        // and runs in the same thread as the timer.

        // We call the method that will work with the UI
        // through the runOnUiThread method.
        //get AMOS gps position and heading direction
        GetBoatPositionAndHeading();

        //get GPS position for this phone
        GetPhonePosition();

        this.runOnUiThread(Timer_Tick);
    }

    private  void GetPhonePosition() {
        int MAX_NUM_ATTEMPTS = 30;// approx. 1 second delay between attempts
        if (MainActivity.m_gpsTracker == null) {
            MainActivity.m_gpsTracker = new GPSTracker(this, 3, 10000);
        }
        int nGPSAttemptCount = 0;
        Location captainLocation = MainActivity.m_gpsTracker.getLocation();
        while (captainLocation == null
                && nGPSAttemptCount < MAX_NUM_ATTEMPTS) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                // do nothing
            }
            nGPSAttemptCount++;
            captainLocation = MainActivity.m_gpsTracker.getLocation();
        }

        if (captainLocation != null) {
            m_dPhoneLatitude = captainLocation.getLatitude();
            m_dPhoneLongitude = captainLocation.getLongitude();
        }
    }

    private void GetBoatPositionAndHeading() {
        if (MainActivity.m_pNetCaptain!=null)
        {
            m_dBoatLatitude = MainActivity.m_pNetCaptain.m_dLatitude;
            m_dBoatLongitude = MainActivity.m_pNetCaptain.m_dLongitude;
            m_dBoatHeading = MainActivity.m_pNetCaptain.m_compassData.heading;
        }
        else if (MainActivity.m_pBluetoothCaptain!=null)
        {
            m_dBoatLatitude = MainActivity.m_pBluetoothCaptain.m_dLatitude;
            m_dBoatLongitude = MainActivity.m_pBluetoothCaptain.m_dLongitude;
            m_dBoatHeading = MainActivity.m_pBluetoothCaptain.m_compassData.heading;
        }
    }

    private void addForwardButtonListener() {
        m_cForwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IncreaseCommandCount();
                if (MainActivity.m_pNetCaptain != null) {
                    // accelerate forwards
                    MainActivity.m_pNetCaptain.ForwardHo();
                } else if (MainActivity.m_pBluetoothCaptain != null) {
                    // accelerate forwards
                    MainActivity.m_pBluetoothCaptain.ForwardHo();
                } else {
                    NoConnectionErrMsg();// display message about not being
                    // connected to the boat yet
                }
                UpdateSpeedAndAngle();
            }
        });
    }

    private void addLeftButtonListener() {
        m_cLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IncreaseCommandCount();
                if (MainActivity.m_pNetCaptain != null) {
                    // turn to left
                    MainActivity.m_pNetCaptain.PortHo();
                } else if (MainActivity.m_pBluetoothCaptain != null) {
                    // turn to left
                    MainActivity.m_pBluetoothCaptain.PortHo();
                } else {
                    NoConnectionErrMsg();// display message about not being
                    // connected to the boat yet
                }
                UpdateSpeedAndAngle();
            }
        });
    }

    private void addRightButtonListener() {
        m_cRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IncreaseCommandCount();
                if (MainActivity.m_pNetCaptain != null) {
                    // turn to right
                    MainActivity.m_pNetCaptain.StarboardHo();
                } else if (MainActivity.m_pBluetoothCaptain != null) {
                    // turn to right
                    MainActivity.m_pBluetoothCaptain.StarboardHo();
                } else {
                    NoConnectionErrMsg();// display message about not being
                    // connected to the boat yet
                }
                UpdateSpeedAndAngle();
            }
        });
    }

    private void addBackButtonListener() {
        m_cBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IncreaseCommandCount();
                if (MainActivity.m_pNetCaptain != null) {
                    // accelerate backwards
                    MainActivity.m_pNetCaptain.BackHo();
                } else if (MainActivity.m_pBluetoothCaptain != null) {
                    // stop thrusters
                    MainActivity.m_pBluetoothCaptain.BackHo();
                } else {
                    NoConnectionErrMsg();// display message about not being
                    // connected to the boat yet
                }
                UpdateSpeedAndAngle();
            }
        });
    }

    private void addStopButtonListener() {
        m_cStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IncreaseCommandCount();
                if (MainActivity.m_pNetCaptain != null) {
                    // stop thrusters
                    MainActivity.m_pNetCaptain.Stop();
                } else if (MainActivity.m_pBluetoothCaptain != null) {
                    // stop thrusters
                    MainActivity.m_pBluetoothCaptain.Stop();
                } else {
                    NoConnectionErrMsg();// display message about not being
                    // connected to the boat yet
                }
                UpdateSpeedAndAngle();
            }
        });
    }

    private void NoConnectionErrMsg() {// display message about not being
        // connected to the boat yet
        UpdateStatus("Error, not connected to boat yet.", true, true);
    }

    // UpdateStatus: update the status message shown in this activity and
    // display an optional message or error popup
    // sMsg = the text message to show in the main activity
    // bShowPopupMsg = true if a popup version of sMsg should also be shown
    // bError = true if the message should be presented as an error
    private void UpdateStatus(String sMsg, boolean bShowPopupMsg, boolean bError) {
        this.m_cStatus.setText(sMsg);
        if (bShowPopupMsg) {
            if (bError) {// show error popup message
                SimpleMessage.msbox("Error", sMsg, this.m_context);
            } else {// show information popup message
                SimpleMessage.msbox("Info", sMsg, this.m_context);
            }
        }
    }

    private void StopGPSTimer() {
        if (m_updateGPSTimer!=null) {
            m_updateGPSTimer.cancel();
            m_updateGPSTimer = null;
        }
    }

    private void RequestStatus() {// request basic status information from AMOS
        String sGPSPosition = null;
        String sCompassData = null;
        String sDiagnosticData = null;
        if (MainActivity.m_pNetCaptain != null) {
            sGPSPosition = MainActivity.m_pNetCaptain.RequestGPSPosition();
            sCompassData = MainActivity.m_pNetCaptain.RequestCompassData();
            sDiagnosticData = MainActivity.m_pNetCaptain.RequestDiagnosticsData();
        }
        else if (MainActivity.m_pBluetoothCaptain!=null) {
            MainActivity.m_pBluetoothCaptain.RequestGPSPosition();
            return;//need to request each status item one at a time, waiting for a response before getting the next status item
        }
        if (sDiagnosticData != null) {
            MainActivity.m_sDiagnosticsText = sDiagnosticData;
        } else {
            MainActivity.m_sDiagnosticsText = "Diagnostics = N.A.";
        }
    }

    private Captain GetCaptain() {
        if (MainActivity.m_pNetCaptain != null) {
            return (Captain) MainActivity.m_pNetCaptain;
        } else if (MainActivity.m_pBluetoothCaptain != null) {
            return (Captain) MainActivity.m_pBluetoothCaptain;
        }
        return null;
    }

    // UpdateSpeedAndAngle: updates the text controls for the
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
        String sRudderAngleText = "", sPropSpeedText = "";
        sRudderAngleText = String.format("R. Angle: %.1f", fRudderAngle);
        sPropSpeedText = String.format("Prop Speed: %.1f", fPropSpeed);
        m_cRudderAngle.setText(sRudderAngleText);
        m_cPropSpeed.setText(sPropSpeedText);
    }

    private void IncreaseCommandCount() {
        m_nCommandCount++;
        if (m_nCommandCount>MapActivity.MAX_COMMAND_COUNT) {
            m_nCommandCount = MapActivity.MAX_COMMAND_COUNT;
        }
    }

    private void DecreaseCommandCount() {
        m_nCommandCount--;
        if (m_nCommandCount<0) {
            m_nCommandCount = 0;
        }
    }

    private Runnable Timer_Tick = new Runnable() {
        public void run() {
            DrawStickFigure();
            DrawBoatShape();
            if (m_nCommandCount>0) {
                DecreaseCommandCount();
            }
            else {
                RequestStatus();
            }
            //update diagnostics text
            m_cDiagnosticsData.setText(MainActivity.m_sDiagnosticsText);
            UpdateSpeedAndAngle();
        }
    };
}


