package com.fungame.music;

import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

import com.adwhirl.AdWhirlLayout;
import com.fungame.music.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class Utils {
	static String TAG = "MP3Download";
	
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
	
	public static ArrayList<MusicInfo> dedup(ArrayList<MusicInfo> mp3List) {
	  if(mp3List == null)
	    return null;
	  //combine same music
      Hashtable htNewList = new Hashtable();
      Iterator<MusicInfo> it = mp3List.iterator();
      while (it.hasNext()) {
        MusicInfo mp3 = it.next();
        String title = mp3.getTitle();
        String artist = mp3.getArtist();
        boolean in = false;
        if (htNewList.containsKey(artist+title)) {
          in = true;
          MusicInfo info = (MusicInfo) htNewList.get(artist+title);
          info.addUrl(mp3.getUrl().get(0));
        }
        if (!in)
          htNewList.put(artist+title, mp3);      
      }
      ArrayList<MusicInfo> newList = new ArrayList<MusicInfo>();
      Iterator it2 = htNewList.values().iterator();
      while (it2.hasNext()) {
        newList.add((MusicInfo) it2.next());
      }
      
//    final int MINSIZE = 10;
//    if (newList.size() < MINSIZE) {
//      for (Iterator<MusicInfo> it = mp3List.iterator(); it.hasNext();) {
//        MusicInfo mp3 = it.next();
//        if (!newList.contains(mp3))
//          newList.add(mp3);
//        if (newList.size() > MINSIZE-1)
//          break;
//      }
//    }
      return newList;
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
    public static void addAds(Activity act) {
    	AdWhirlLayout adWhirlLayout = new AdWhirlLayout(act, "e383f83acfec4f34b591486a93c4da96");
    	RelativeLayout.LayoutParams adWhirlLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
    	LinearLayout layout = (LinearLayout) act.findViewById(R.id.layout_ad);
    	layout.addView(adWhirlLayout, adWhirlLayoutParams);

    }
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

    private static CharsetEncoder sEncoder = Charset.forName("ISO-8859-1").newEncoder();
    private static CharsetDecoder sDecoder = Charset.forName("GBK").newDecoder();
    public static String convertGBK(String input) {
    	try {
    		ByteBuffer bbuf = sEncoder.encode(CharBuffer.wrap(input));
    		CharBuffer cbuf = sDecoder.decode(bbuf);
    		String output = cbuf.toString();
    		return output;
    	} catch (Exception e) {
    		//e.printStackTrace();
    		return input;
    	}
    }
}
