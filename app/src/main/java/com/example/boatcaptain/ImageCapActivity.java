package com.example.boatcaptain;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;


public class ImageCapActivity extends Activity {
	public static ImageView m_cImage;//used to display the image that gets downloaded from AMOS
	public static TextView m_cProgressText;//text describing how much of the image has been downloaded eg: (30000 of 50000 bytes downloaded)
    public static ProgressBar m_cProgressBar;//progress bar used to indicate how much of the image has been downloaded
    public static int m_nImageSize;//the size (in bytes) of the image being downloaded

	public String m_sImageFilename;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.image_layout);
	    View main_view = findViewById(R.id.activity_image_layout);
	    RelativeLayout thisLayout = (RelativeLayout) findViewById(R.id.activity_image_layout);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        m_cImage = (ImageView)findViewById(R.id.amos_image);
        m_cProgressText = (TextView)findViewById(R.id.imagecap_text);
        m_cProgressBar = (ProgressBar)findViewById(R.id.imagecap_progress);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, -1);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, -1);
        //Intent i = getIntent();
        //m_sImageFilename = i.getStringExtra("image_filename");
        new ImageDownloadTask().execute();
	}

    /**
     * display the image downloaded from AMOS in this activity window.
     * @param sImageFilename the filename of the image file to display.
     */
	public void ShowImage(String sImageFilename) {
        File imgFile = new  File(sImageFilename);
        if(imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            m_cImage.setImageBitmap(myBitmap);
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
	    m_cProgressText.setText(sProgress);
	    m_cProgressBar.setProgress(nProgress);
    }

    public static void DisplayDownloadedImageBytes(byte []imageCaptureBytes) {
        if (imageCaptureBytes==null) return;
        String sImageFilename = BOAT_DATA.SaveTmpImageFile(imageCaptureBytes);
        if (sImageFilename==null||sImageFilename.length()<=0) {
            return;
        }
        //hide progress bar and progress text
        m_cProgressBar.setVisibility(View.INVISIBLE);
        m_cProgressText.setText("");
        m_cProgressText.setVisibility(View.INVISIBLE);
        File imgFile = new File(sImageFilename);
        if (imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            m_cImage.setImageBitmap(myBitmap);
        }
    }


}

class ImageDownloadTask extends AsyncTask<String, Integer, String> {
    @Override
    protected void onPreExecute() {
        //make sure progress bar and text are visible
        ImageCapActivity.m_cProgressBar.setVisibility(View.VISIBLE);
        ImageCapActivity.m_cProgressText.setVisibility(View.VISIBLE);
        ImageCapActivity.m_cProgressBar.setMax(100);
        ImageCapActivity.m_cProgressBar.setProgress(0);
    }

    @Override
    protected String doInBackground(String... params) {
        //download image from boat
        String sImageFilename = "";
        if (MainActivity.m_pNetCaptain!=null) {
            if (!MainActivity.m_pNetCaptain.RequestVideoImage(65536)) {//65536 (0x00010000) is code for getting normal quality image without any feature detection markers
                String sError = new String("Error,  problem requesting image.");
                return sError;
            }
            boolean bReceivedAcknowledgement = MainActivity.m_pNetCaptain.ReceiveBoatData();
            if (!bReceivedAcknowledgement) {
                String sError = new String("Error, did not receive acknowledgement for image request.");
                return sError;
            }
            int nNumBytes = MainActivity.m_pNetCaptain.m_nNumImageBytes;
            ImageCapActivity.m_nImageSize = nNumBytes;
            int nNumImagePackets = nNumBytes / NetCaptain.CHUNKSIZE;
            if ((nNumBytes % NetCaptain.CHUNKSIZE)>0) {
                nNumImagePackets++;
            }
            byte []receivedBytes = null;
            for (int i=0;i<nNumImagePackets;i++) {
                int nNumToReceive = Math.min(NetCaptain.CHUNKSIZE,nNumBytes);
                byte []packetBytes = MainActivity.m_pNetCaptain.ReceiveLargeDataChunk(nNumToReceive);
                if (packetBytes!=null) {
                    receivedBytes = Util.appendBytes(packetBytes, receivedBytes);
                    nNumBytes -= packetBytes.length;
                }
                if (receivedBytes.length>0) {
                    publishProgress(receivedBytes.length);
                }
                else publishProgress(0);
            }
            if (receivedBytes!=null) {
                sImageFilename = BOAT_DATA.SaveTmpImageFile(receivedBytes);
            }
        }
        else if (MainActivity.m_pBluetoothCaptain!=null) {
            if (!MainActivity.m_pBluetoothCaptain.RequestVideoImage(131072)) {//131072 (0x00020000 is code for getting low quality image without any feature detection stuff)
                String sError = new String("Error trying to send Bluetooth data.\n");
                return sError;
            }
        }
        return sImageFilename;
    }

    @Override
    protected void onPostExecute(String sImageFilename) {
        //show the result obtained from doInBackground
        //save frame capture bytes to temporary image file
        if (sImageFilename!=null) {
            if (sImageFilename.length()>0) {
                ImageCapActivity.m_cProgressBar.setVisibility(View.INVISIBLE);//hide progress bar
                if (sImageFilename.toLowerCase().indexOf("error") >= 0) {//an error occurred
                    ImageCapActivity.m_cProgressText.setText(sImageFilename);//set progress text to error message
                } else {//file was received successfully, hide progress text
                    ImageCapActivity.m_cProgressText.setVisibility(View.INVISIBLE);//hide progress text
                    File imgFile = new File(sImageFilename);
                    if (imgFile.exists()) {
                        Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        ImageCapActivity.m_cImage.setImageBitmap(myBitmap);
                    }
                }
            }
        }
    }
    protected void onProgressUpdate(Integer... bytes_downloaded) {
        //This method runs on the UI thread, it receives progress updates from the background thread and updates the status text and progress bar
        int nProgress = 100 * bytes_downloaded[0] / ImageCapActivity.m_nImageSize;
        String sProgress = String.format("%d of %d bytes downloaded.",bytes_downloaded[0], ImageCapActivity.m_nImageSize);
        ImageCapActivity.m_cProgressBar.setProgress(bytes_downloaded[0]);
        ImageCapActivity.m_cProgressText.setText(sProgress);
    }

}
