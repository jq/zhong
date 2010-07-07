package com.fungame.music;

import java.util.Collection;
import java.util.Iterator;

import com.fungame.music.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;

public class Utils {
	static String TAG = "MusicWizard";
	
	static public final boolean DEBUG = false;
	
	static public void D(String msg) {
		if (DEBUG) {
			Log.d(TAG, msg);
		}
	}
	
	static public void printD(String msg) {
		if (DEBUG) {
			System.out.println(msg);
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
    
    public static String trimTag(String s) {
    	return s.replaceAll("\\<.*?>", "");
    }
    
    
    private static int sNotificationId = 0;
    
	public static void addNotification(Context context, Intent intent, String title,
			String resTitle, String resText, String resExpandedTitle, String resExpandedText) {
    	int icon = R.drawable.icon;
    	String tickerText ="\"" + title + "\"" + resTitle;
    	long when = System.currentTimeMillis();
    	Notification notification = new Notification(icon, tickerText, when);
    	String expandedText = "\"" + title + "\" " + resExpandedText;
    	String expandedTitle = resExpandedTitle;

    	PendingIntent launchIntent = PendingIntent.getActivity(context, 0, intent, 0);
        notification.setLatestEventInfo(context, expandedTitle, expandedText, launchIntent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        NotificationManager notificationManager;
        notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        notificationManager.notify(R.layout.search + sNotificationId++, notification);
	}
	/*
    public static void createQWAd(Activity activity){
        ViewGroup parentView = (ViewGroup)activity.findViewById(R.id.AdsView);
        QWAdView adView = new QWAdView(activity,
                "Musicmp3-g8886byb",
                "a7beb24f184b4305804013cbc447f678",
                MediaType.banner,
                Placement.bottom,
                DisplayMode.autoRotate,30,
                AnimationType.slide,
                new QWAdEventsListener(),
                true);
        parentView.addView(adView);
    }
    */
}
