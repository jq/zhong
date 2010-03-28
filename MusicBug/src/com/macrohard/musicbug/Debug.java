package com.macrohard.musicbug;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

public class Debug {

	static final int DebugDialogID = 100;
	static String TAG = "MusicBug";
	
	static public final boolean DEBUG = true;
	
	static public void D(String msg) {
		if (DEBUG) {
			Log.d(TAG, "[MB] " + msg);
		}
	}
	
	static public void assertD(boolean b) {
		if (DEBUG)
			assert b;
	}
	
	// A dialogue showing a specified message.
	static public void DP(Context a, String msg) {
		new AlertDialog.Builder(a).setPositiveButton(
				"OK", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        }
	    }).setTitle("Debug").setMessage(msg).create().show();
	}
}
