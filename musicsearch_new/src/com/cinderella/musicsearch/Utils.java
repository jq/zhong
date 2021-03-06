package com.cinderella.musicsearch;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.io.File;
import java.net.ContentHandler;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class Utils {
	static String TAG = "MP3Download";
	
	static public final boolean DEBUG = true;
	
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
	
	static public int getSizeInM(String sizeStr) {
      int size = 0;
      if(sizeStr.startsWith("unknown"))
        return 0;
      if(sizeStr.endsWith("k") || sizeStr.endsWith("K"))
        return 0;
      if(sizeStr.endsWith("m") || sizeStr.endsWith("M")) {
        String sizeString = sizeStr.substring(0, sizeStr.length()-1);
        try {
          size = (int) Double.parseDouble(sizeString);
        } catch (Exception e) {
          return 0;
        }
        return size;
      }
      return size;
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
    
    private static final boolean blackscreen = isBlackScreen();
    
    private static boolean isBlackScreen() {
        // http://since2006.com/blog/google-io2010-android-devices/
        return Build.VERSION.SDK.equalsIgnoreCase("3");
    }
    
//    public static void addAds(Activity act) {
//      int w;
//      if (blackscreen) {
//          w = 48;
//      } else {
//          w = LayoutParams.WRAP_CONTENT;
//      }
//      AdWhirlLayout adWhirlLayout = new AdWhirlLayout(act, "e383f83acfec4f34b591486a93c4da96");
//      LayoutParams adWhirlLayoutParams = new LayoutParams(LayoutParams.FILL_PARENT, w);
//      LinearLayout layout = (LinearLayout) act.findViewById(R.id.layout_ad);
//      layout.addView(adWhirlLayout, adWhirlLayoutParams);
//    }
    
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
   
    public static boolean isUrl(String url) {
    	url = url.trim().substring(0, 4);
    	if (url.equalsIgnoreCase("http")) {
    		return true;
    	}
    	return false;
    }
    
    public static void startMusicPlayer(Context context, String musicPath) {
    	Utils.D("Start Media Player: "+musicPath);
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setDataAndType(Uri.parse("file://" + musicPath), "audio");
		context.startActivity(intent);
    }
    
    public static void deleteFile(String path) {
    	try {
    		File file = new File(path);
    		if (file.exists()) {
    			file.delete();
    		}
    	} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public static void deleteFromMediaStore(Context context, String path) {
    	try {
			ContentResolver cr = context.getContentResolver();
    		Uri musics = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    		int n = 0;
    		if (!path.startsWith("/mnt")) {
        		cr.delete(musics, MediaStore.MediaColumns.DATA + "=?", new String[] { "/mnt" + path });
    		}
    		if (n == 0) {
    			cr.delete(musics, MediaStore.MediaColumns.DATA + "=?", new String[] { path });
    		}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public static String lengthToDisplaySize(long length) {
    	Double sizeInM = (double)length/(double)(1024*1024);
    	java.text.DecimalFormat   df=new   java.text.DecimalFormat("#0.00");
    	String sizeString = df.format(sizeInM)+"M";
    	return sizeString;
    }
    
}
