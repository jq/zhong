package com.macrohard.musicbug;

import java.util.Collection;
import java.util.Iterator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;

public class Utils {
	
    public static String join(Collection<String> s, String delimiter) {
        StringBuffer buffer = new StringBuffer();
        Iterator<String> iter = s.iterator();
        while (iter.hasNext()) {
            buffer.append(iter.next().toString());
            if (iter.hasNext()) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }
    
	static public void Error(Context a, String msg) {
		new AlertDialog.Builder(a).setPositiveButton(
				"OK", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        }
	    }).setTitle("Error").setMessage(msg).create().show();
	}
	
	static public void Info(Context a, String msg) {
		new AlertDialog.Builder(a).setPositiveButton(
				"OK", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        }
	    }).setTitle("Info").setMessage(msg).create().show();
	}
	
    public static long sizeFromStr(String sizeStr) {
    	if (TextUtils.isEmpty(sizeStr)) {
    		return 0;
    	} else {
    		if (sizeStr.endsWith("K") ||
    			sizeStr.endsWith("k")) {
    			return (long) (1024 * Float.valueOf(sizeStr.substring(0, sizeStr.length() - 1)));
    		} else if (sizeStr.endsWith("M") ||
    			sizeStr.endsWith("m")) {
    			return (long) (1024 * 1024 * Float.valueOf(sizeStr.substring(0, sizeStr.length() - 1)));
    		} else {
    			try {
        			return Long.valueOf(sizeStr);
    			} catch (java.lang.NumberFormatException e) {
    				e.printStackTrace();
    				return 0;
    			}
    		}
    	}
    }
}
