package com.example.boatcaptain;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadFilesActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private int m_nDownloadType;//the type of files being downloaded
    public static TextView m_cDownloadText;
    private Switch m_cSelectAllSwitch;
    private Button m_cDownloadFilesButton;
    public static ProgressBar m_cProgressBar;
    private ScrollView m_cFilesAvailable;
    private LinearLayout m_cLinearLayout;
    private List<ClickTextView> m_fileNameItems;
    private boolean[] m_selected;//each element is true if selected by the user, otherwise false
    private String[] m_remoteFilenames;//array of remote files on AMOS that are available for download
    public static String m_sDownloadFilename;//the name of the file currently being downloaded

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.downloadlayout);
        getActionBar().setIcon(R.drawable.robot_only);
        m_cDownloadText = (TextView) findViewById(R.id.download_text);
        m_cProgressBar = (ProgressBar) findViewById(R.id.download_progressBar);
        m_cFilesAvailable = (ScrollView) findViewById(R.id.download_scrollview);
        m_cLinearLayout = (LinearLayout) findViewById(R.id.download_scrollview_layout);
        m_cSelectAllSwitch = (Switch) findViewById(R.id.selectallfiles_switch);
        m_cDownloadFilesButton = (Button) findViewById(R.id.downloadfiles_button);

        addDownloadFilesButtonListener();
        addAllFilesListener();

        Intent i = this.getIntent();
        m_remoteFilenames = i.getStringArrayExtra("download_files");
        m_nDownloadType = i.getIntExtra("download_type", REMOTE_COMMAND.LIST_REMOTE_DATA);
        SetDownloadFilesTitle();
        //add filenames to scrollview
        int nNumFiles = m_remoteFilenames.length;
        m_fileNameItems = new ArrayList<ClickTextView>();
        m_selected = new boolean[nNumFiles];
        for (int j = 0; j < nNumFiles; j++) {
            //create a TextView for each remote filename
            ClickTextView tvItem = new ClickTextView(this);
            tvItem.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
            tvItem.setTextColor(getResources().getColor(R.color.white1, null));
            tvItem.setText(m_remoteFilenames[j]);
            m_cLinearLayout.addView(tvItem);
            m_fileNameItems.add(tvItem);
        }
    }

    private void SetDownloadFilesTitle() {
        if (m_nDownloadType == REMOTE_COMMAND.LIST_REMOTE_DATA) {
            this.setTitle("Download data files");
            m_cDownloadText.setText("Select data file(s) to download.");
        } else if (m_nDownloadType == REMOTE_COMMAND.LIST_REMOTE_IMAGE) {
            this.setTitle("Download image files");
            m_cDownloadText.setText("Select image file(s) to download.");
        } else if (m_nDownloadType == REMOTE_COMMAND.LIST_REMOTE_LOG) {
            this.setTitle("Download log files");
            m_cDownloadText.setText("Select log file(s) to download.");
        } else if (m_nDownloadType == REMOTE_COMMAND.LIST_REMOTE_SCRIPTS) {
            this.setTitle("Download remote script file");
            m_cDownloadText.setText("Select script file(s) to download.");
        }
    }

    private void addDownloadFilesButtonListener() {
        m_cDownloadFilesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //download the selected files
                int nNumFiles = m_fileNameItems.size();
                for (int i = 0; i < nNumFiles; i++) {
                    if (m_fileNameItems.get(i).m_bSelected) {
                        String sFileName = (String) m_fileNameItems.get(i).getText();
                        DownloadFile(sFileName);
                    }
                }
            }
        });
    }

    private void DownloadFile(String sFilename) {
        new FileDownloadTask().execute(sFilename);
    }

    private void addAllFilesListener() {
        m_cSelectAllSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int nNumFiles = m_fileNameItems.size();
                if (m_cSelectAllSwitch.isChecked()) {// use Bluetooth connection to
                    //select all the files
                    for (int i = 0; i < nNumFiles; i++) {
                        m_fileNameItems.get(i).m_bSelected = true;
                        m_fileNameItems.get(i).UpdateControl();
                    }
                } else {
                    //unselect all the files
                    for (int i = 0; i < nNumFiles; i++) {
                        m_fileNameItems.get(i).m_bSelected = false;
                        m_fileNameItems.get(i).UpdateControl();
                    }
                }
            }
        });
    }

    /**
     * save bytes to a file
     * @param fileBytes the bytes to save to file
     */
    public static void SaveDownloadedFileBytes(byte []fileBytes) {
        if (fileBytes==null) {
            return;
        }
        if (Util.IsImageFile(m_sDownloadFilename)) {
            m_sDownloadFilename = BOAT_DATA.SaveImageFile(fileBytes, m_sDownloadFilename);
        }
        else {
            m_sDownloadFilename = BOAT_DATA.SaveDataFile(fileBytes, m_sDownloadFilename);
        }
    }

    /**
     * Update the progress bar and progress text for the current image download
     * @param nNumBytesDownloaded the number of image capture bytes downloaded so far.
     * @param nTotalNumBytes the total number of image capture bytes to download.
     */
    public static void UpdateDownloadStatus(int nNumBytesDownloaded, int nTotalNumBytes) {
        String sProgress = String.format("%d of %d bytes downloaded",nNumBytesDownloaded, nTotalNumBytes);
        int nProgress = 0;
        if (nTotalNumBytes>0) {
            nProgress = 100 * nNumBytesDownloaded / nTotalNumBytes;
        }
        m_cDownloadText.setText(sProgress);
        m_cProgressBar.setProgress(nProgress);
    }
}
    class FileDownloadTask extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            //make sure progress bar and text are visible
            DownloadFilesActivity.m_cProgressBar.setVisibility(View.VISIBLE);
            DownloadFilesActivity.m_cDownloadText.setVisibility(View.VISIBLE);
            DownloadFilesActivity.m_cProgressBar.setMax(100);
            DownloadFilesActivity.m_cProgressBar.setProgress(0);
            DownloadFilesActivity.m_cDownloadText.setText("");
        }

        @Override
        protected String doInBackground(String... params) {
            //download file from boat
            Log.d("debug","FileDownloadTask, doInBackground function.\n");
            String sDownloadFilename = params[0];
            DownloadFilesActivity.m_sDownloadFilename = sDownloadFilename;
            if (MainActivity.m_pNetCaptain!=null) {
                if (!MainActivity.m_pNetCaptain.RequestRemoteFile(sDownloadFilename)) {
                    String sError = String.format("Error requesting file: %s",sDownloadFilename);
                    return sError;
                }
                boolean bReceivedAcknowledgement = MainActivity.m_pNetCaptain.ReceiveBoatData();
                if (!bReceivedAcknowledgement) {
                    String sError = new String("Error, did not receive acknowledgement for remote file request.");
                    return sError;
                }
                int nNumBytes = MainActivity.m_pNetCaptain.m_nNumLargeBlockBytes;
                int nNumFilePackets = nNumBytes / NetCaptain.CHUNKSIZE;
                if ((nNumBytes % NetCaptain.CHUNKSIZE)>0) {
                    nNumFilePackets++;
                }
                byte []receivedBytes = null;
                for (int i=0;i<nNumFilePackets;i++) {
                    int nNumToReceive = Math.min(NetCaptain.CHUNKSIZE,nNumBytes);
                    byte []packetBytes = MainActivity.m_pNetCaptain.ReceiveLargeDataChunk(nNumToReceive);
                    if (packetBytes!=null) {
                        receivedBytes = Util.appendBytes(packetBytes, receivedBytes);
                        nNumBytes -= packetBytes.length;
                    }
                    if (receivedBytes.length>0) {
                        publishProgress(MainActivity.m_pNetCaptain.m_nNumLargeBlockBytes, receivedBytes.length);
                    }
                    else publishProgress(0, 0);
                }
                if (receivedBytes!=null) {
                    if (Util.IsImageFile(sDownloadFilename)) {
                        sDownloadFilename = BOAT_DATA.SaveImageFile(receivedBytes, sDownloadFilename);
                    }
                    else {
                        sDownloadFilename = BOAT_DATA.SaveDataFile(receivedBytes, sDownloadFilename);
                    }
                }
            }
            else if (MainActivity.m_pBluetoothCaptain!=null) {
                if (!MainActivity.m_pBluetoothCaptain.RequestRemoteFile(sDownloadFilename)) {
                    String sError = new String("Error trying to send Bluetooth data.\n");
                    return sError;
                }
            }
            return sDownloadFilename;
        }

        @Override
        protected void onPostExecute(String sDownloadedFilename) {

        }

        @Override
        protected void onProgressUpdate(Integer... bytes_downloaded) {
            //This method runs on the UI thread, it receives progress updates from the background thread and updates the status text and progress bar
            int nProgress = 100 * bytes_downloaded[0] / ImageCapActivity.m_nImageSize;
            String sProgress = String.format("%d of %d bytes downloaded.",bytes_downloaded[0], ImageCapActivity.m_nImageSize);
            DownloadFilesActivity.m_cProgressBar.setProgress(bytes_downloaded[0]);
            DownloadFilesActivity.m_cDownloadText.setText(sProgress);
        }

}