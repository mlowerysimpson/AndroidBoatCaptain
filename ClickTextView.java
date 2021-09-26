package com.example.boatcaptain;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.TextView;

public class ClickTextView extends TextView {
    public boolean m_bSelected;//true if the user has clicked on this text item to select it
    public ClickTextView(Context context) {
        super(context);
        m_bSelected = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        //x,y coordinates of touch event
        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                m_bSelected = !m_bSelected;
                performClick();
                UpdateControl();
                break;
        }
        return false;
    }

    public void UpdateControl() {
        if (m_bSelected) {
            //change background color to white and text color to black
            this.setTextColor(getResources().getColor(R.color.black, null));
            this.setBackgroundColor(getResources().getColor(R.color.white1, null));
        }
        else {
            //change background color to black and text color to white
            this.setTextColor(getResources().getColor(R.color.white1,null));
            this.setBackgroundColor(getResources().getColor(R.color.black, null));
        }
    }
}
