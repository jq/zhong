package com.happy.life;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import com.admob.android.ads.AdView;
import com.adwhirl.AdWhirlLayout;
import com.qwapi.adclient.android.view.QWAdView;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class Utils {
	
	static String TAG = "MBlaster";
	
	static public final boolean DEBUG = false;
	
    /**
     * The number of bytes in a kilobyte.
     */
    public static final long ONE_KB = 1024;

    /**
     * The number of bytes in a megabyte.
     */
    public static final long ONE_MB = ONE_KB * ONE_KB;

    /**
     * The number of bytes in a gigabyte.
     */
    public static final long ONE_GB = ONE_KB * ONE_MB;
	
	static public void D(String msg) {
		if (DEBUG) {
			Log.d(TAG, msg);
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
    		try {
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
    		} catch (Exception e) {
    			e.printStackTrace();
    			return 0;
    		}
    	}
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
    
    public static ArrayList<SogouSearchResult> dedup(ArrayList<SogouSearchResult> mp3List) {
		if (mp3List == null) {
			return null;
		}
		
		if (mp3List.size() == 0)
			return mp3List;
		
		//combine same music
        Hashtable<String, SogouSearchResult> seen = new Hashtable<String, SogouSearchResult>();
        ArrayList<SogouSearchResult> newList = new ArrayList<SogouSearchResult>();
     
        for (Iterator<SogouSearchResult> it = mp3List.iterator(); it.hasNext();) {
          SogouSearchResult mp3 = it.next();
          String title = mp3.getTitle();
          String artist = mp3.getArtist();
          String displaySize = mp3.getDisplayFileSize();
          final String key = title + artist + displaySize;
          if (seen.containsKey(key)) {
            SogouSearchResult info = (SogouSearchResult) seen.get(key);
            // Assumes URL does not have duplicates.
            info.addUrl(mp3.getUrl());
          } else {
            seen.put(key, mp3);      
            newList.add(mp3);
          }
        }
        
        return newList;
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

	public static void copyConfigFile(Context context) {
        // copy file
        File gnutella = new File("/sdcard/musiclife/setting/gnutella.net");
        if (!gnutella.exists()) {
            InputStream in = context.getResources().openRawResource(R.raw.gnutella);
            OutputStream out;
            try {
                out = new FileOutputStream(gnutella);
    
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0){
                  out.write(buf, 0, len);
                }
                in.close();
                out.close();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
	}
	
	public static String getHexString(byte[] b) {
		String result = "";
		for (int i = 0; i < b.length; i++) {
			result +=
				Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
		}
		return result;
	}

    public static String displaySizeInMB(long size) {
    	return String.format("%.2fM", size * 1.0 / ONE_MB);
    }
    
    public static int getSizeInM(int size) {
      return (int) (size / ONE_MB);
    }
	
    public static boolean sameGuid(byte[] guid1, byte[] guid2) {
		int size = guid1.length;
		if (size != guid2.length) {
			return false;
		}
		for (int i = 0; i < size; i++)
			if (guid1[i] != guid2[i]) {
				return false;
			}
		return true;
    }
    
    /**
     * Returns whether the network is available
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            Log.w(TAG, "couldn't get connectivity manager");
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        Log.v(TAG, "network is not available");
        return false;
    }
    
    private static final boolean blackscreen = isBlackScreen();
    
    private static boolean isBlackScreen() {
        // http://since2006.com/blog/google-io2010-android-devices/
        return Build.VERSION.SDK.equalsIgnoreCase("3");
    }
    
	public static void addMixedAds(Activity activity) {
	  addMixedAds(activity, R.id.layout_ad);
	}
	public static void addMixedAds(Activity activity, int id) {
		  int w;
	      if (blackscreen) {
	          w = 48;
	      } else {
	          w = LayoutParams.WRAP_CONTENT;
	      }
	      AdWhirlLayout adWhirlLayout = new AdWhirlLayout(activity, "b2c900faac5d44f5a2358df294d75309");
	      LayoutParams adWhirlLayoutParams = new LayoutParams(LayoutParams.FILL_PARENT, w);
	      LinearLayout layout = (LinearLayout) activity.findViewById(id);
	      if (layout != null)
	        layout.addView(adWhirlLayout, adWhirlLayoutParams);
		}
	
	private static final int CONNECT_TIMEOUT = 10000;  // 10s
	private static final int INITIAL_BUFFER_SIZE = 16000;  // 16K

	public static String fetchHtmlPage(String link, String coding) throws IOException {
		URL url = new URL(link);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		connection.setRequestProperty("User-Agent",
									  "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.3) Gecko/20100401 Firefox/3.6.3");
		connection.setRequestProperty("Accept", "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
		connection.setRequestProperty("Accept-Language", "en-us");
		connection.setRequestProperty("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7");
		connection.setRequestProperty("Keep-Alive", "300");
		connection.setRequestProperty("Connection", "keep-alive");
		
		connection.setConnectTimeout(CONNECT_TIMEOUT);
		connection.connect();
		
		StringBuilder builder = new StringBuilder(INITIAL_BUFFER_SIZE);

		// Will throw exception when there is 404
		InputStreamReader is = coding != null ? new InputStreamReader(connection.getInputStream(), coding) :
			new InputStreamReader(connection.getInputStream());
		
		BufferedReader reader = new BufferedReader(is);

		String line = null;
		while ((line = reader.readLine()) != null) {
			builder.append(line + "\n");
		}
		return builder.toString();
	}
	
	
    public static InetAddress getLocalIpAddress() throws UnknownHostException {
        
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress;
                    }
                }
            }
        } catch (SocketException ex) {
        }
        
        return InetAddress.getLocalHost();
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
