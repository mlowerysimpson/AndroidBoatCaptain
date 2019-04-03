package com.example.boatcaptain;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class SimpleMessage {
	public static YesNoDecision YES_NO =null;
	public static Context CONTEXT = null;
	public static void msbox(String sTitle,String sMessage, Context ctext)
	{
	    AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(ctext);                      
	    dlgAlert.setTitle(sTitle); 
	    dlgAlert.setMessage(sMessage); 
	    dlgAlert.setPositiveButton("OK",new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	             return; 
	        }
	   });
	    dlgAlert.setCancelable(true);
	    dlgAlert.create().show();
	}
	public static void yesno_box(String sTitle, String sMessage, Context cText, YesNoDecision yesNo) {
		SimpleMessage.YES_NO=yesNo;
		SimpleMessage.CONTEXT=cText;
		AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(cText);                      
	    dlgAlert.setTitle(sTitle); 
	    dlgAlert.setMessage(sMessage);
	    dlgAlert.setPositiveButton("YES",new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	             SimpleMessage.YES_NO.Yes(SimpleMessage.CONTEXT); 
	        }
	    });
	    dlgAlert.setNegativeButton("NO",new DialogInterface.OnClickListener() {
	    	public void onClick(DialogInterface dialog, int whichButton) {
	    		SimpleMessage.YES_NO.No(SimpleMessage.CONTEXT);
	    	}
	    });
	    dlgAlert.setCancelable(true);
	    dlgAlert.create().show();
	}
}

