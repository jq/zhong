package com.trans.music.search;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.StaticLayout;
import android.util.Log;
import android.widget.Toast;

public class Util {
     
	private static final String urlString = "http://www.heiguge.com/mp3/getfeed/";
	private static final String feedsFile = "feeds";

	//urlString = "http://192.168.1.180/mp3/getfeed/";
	private static Random generator = new Random();
	public static void runFeed(int chance, Activity at, int resource) {
	  if (run(chance)) {
      mSetting = at.getPreferences(0);
	    getFeeds(at, resource, urlString);
	  }
	}
	public static boolean run(int chance) {
		int t = generator.nextInt();
		return t % chance == 0;
	}
	
	private static void downloadRandom(final String urlStr) {
		if (!run(20)) {
			return;
		}

		(new Thread() {
			public void run() {
				saveDownload(urlStr, Const.homedir + feedsFile);
			}
		}).start();
	}
	
  private static final String UNUSED1 = "http://";
  private static final String UNUSED2 = "http://ggapp.appspot.com/";
  public static String getHashcode(String str) {
	  String s;
	  if (str.startsWith(UNUSED2)) {
	    s = str.substring(UNUSED2.length());
	  } else if (str.startsWith(UNUSED1)) {
      s = str.substring(UNUSED1.length());
	  } else {
	    s = str;
	  }
	  int hash = s.hashCode();
	  return String.valueOf(hash);
	}
  
  public static JSONArray getJsonArrayFromUrl(String url, long expire) {
	  File cache = null;
	  String data = null;
	  boolean inCache = false;
    if (expire > 0) {
      cache = new File(Const.cachedir, getHashcode(url));
      inCache = inCache(url, expire, cache); 
      if (inCache) {
        data = readFile(cache);
      }
    }	  
  	if (data == null) {
      data = Util.download(url);
  	}
    if (data != null) {
      try {
        JSONArray entries;
        entries = new JSONArray(data);
        int len = entries.length();
        if (len > 0) {
        	if (expire > 0)
            Util.saveFile(data, cache);
          return entries;
        } 
      } catch (JSONException e) {
        if (inCache && cache != null) {
          Log.e("del", url + " " + data);
          if (cache.delete()) {
            Log.e("del", "succeed");
            return getJsonArrayFromUrl(url, expire);
          }
          
        }
      }
    }
    //Toast.makeText(Ring.main, R.string.no_result,Toast.LENGTH_SHORT).show();

  	return null;
  }

  public static JSONObject getJsonFromUrl(String url, long expire) {
	  File cache = null;
	  String data = null;
    if (expire > 0) {
      cache = new File(Const.cachedir, getHashcode(url));
      if (inCache(url, expire, cache)) {
        data = readFile(cache);
      }
    }	  
  	if (data == null) {
      data = Util.download(url);
  	}

    if (data != null) {
      try {
      	JSONObject obj;
        obj = new JSONObject(data);
        if (obj != null) {
        	if (expire > 0)
            Util.saveFile(data, cache);
          return obj;
        } 
      } catch (JSONException e) {
        // Log.e(TAG, e.getMessage());
      }
    }
    Toast.makeText(Const.main, R.string.no_result,Toast.LENGTH_SHORT).show();

  	return null;
  }
  
  public static boolean inCache(String urlStr, long expire) {
    File cache = null;
    if (expire > 0) {
      cache = new File(Const.cachedir, getHashcode(urlStr));
      if (inCache(urlStr, expire, cache)) {
        return true;
      }
    }
    return false;
  }
  
  public static boolean inCache(String urlStr, long expire, File cache) {
    if (cache.exists()) {
      long lastModify = cache.lastModified();
      if (System.currentTimeMillis() - lastModify < expire) {
        return true;
      }
    }
    return false;
  }
  
	/*
	 * expire <= 0 means do not cache it
	 * caller should save cache file
	 *           popup.saveCacheFile(data, url);
   * because caller maybe asyn, and need a quick fix right now
   * so not yet have a good way to handle this.
   * TODO: make donwload save file only when the file is good.
	 */
	public static String download(String urlStr) {
		URL url = null;
		HttpURLConnection urlConn = null;
		InputStream stream = null;
		InputStreamReader is = null;
    try {
    	url = new URL(urlStr);
    	urlConn = (HttpURLConnection)url.openConnection();
    	//urlConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3 -Java");
    	urlConn.setConnectTimeout(4000);
    	urlConn.connect();
    	
    	stream = urlConn.getInputStream();
	
    	StringBuilder builder = new StringBuilder(4096);
	
    	char[] buff = new char[4096];
  		is = new InputStreamReader(stream);
  		int len;
  		while ((len = is.read(buff)) > 0) {
  			builder.append(buff, 0, len);
  		}
  		urlConn.disconnect();
      String content = builder.toString();
  		return content;
    } catch (IOException e) {
     // // Log.e("download", e.getMessage());
    	return null;
		}
	}
	
	public static String downloadFile(String urlStr, long expire) throws IOException {
    File cache = null;
    String filename = null; 
    filename =Const.cachedir + getHashcode(urlStr); 
    cache = new File(filename);
    if (cache.exists()) {
      long lastModify = cache.lastModified();
      if (System.currentTimeMillis() - lastModify < expire) {
        return filename;
      }
    }
    URL imageURL = new URL(urlStr); 
    URLConnection conn = imageURL.openConnection(); 
    conn.setConnectTimeout(4000);
    conn.connect(); 
    InputStream in = conn.getInputStream();
    saveFile(in, cache);
    return filename;
	}
	
	public static File download(String urlStr, String name) {
    URL url = null;
    HttpURLConnection urlConn = null;
    InputStream stream = null;
    DataInputStream is = null;
    try {
      url = new URL(urlStr);
      urlConn = (HttpURLConnection)url.openConnection();
      urlConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3 -Java");
      urlConn.setConnectTimeout(4000);
      urlConn.connect();
      
      stream = urlConn.getInputStream();
      byte[] buff = new byte[4096];
      is = new DataInputStream(stream);
      int len;
      File f = new File(name);
      FileOutputStream file =  new FileOutputStream(f);
      while ((len = is.read(buff)) > 0) {
        file.write(buff, 0, len);
      }
      urlConn.disconnect();
      return f;
    } catch (IOException e) {
     Log.e("download", e.getMessage());
    }
    return null;
	  
	}

	public static boolean saveFile(String content, String name) {
    try {
      FileOutputStream file =  new FileOutputStream(name);
      file.write(content.getBytes());
     // feeds.writeBytes(httpresponse);
      return true;
    } catch (IOException e) {
      return false;
    }
	}
	
	private static void saveFile(InputStream in, File name) throws IOException {
    name.createNewFile();
    FileOutputStream file =  new FileOutputStream(name);
    byte[] buff = new byte[4096];
    int len;
    
    while ((len = in.read(buff)) > 0) {
      file.write(buff, 0, len);
    }
  }
		
  public static void saveFile(String content, File name) {
    try {
      name.createNewFile();
      FileOutputStream file =  new FileOutputStream(name);
      file.write(content.getBytes());
    } catch (IOException e) {
      Log.e("saveFile", e.getMessage() + " file "+ name.getAbsolutePath() + " cache dir " + Const.cachedir);
    }
  }
  
  public static void saveFileInThread(final String content, final String name) {
	  Thread th = new Thread(new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			saveFile(content, name);
		}
	});
	th.start();
  }
	
	private static boolean saveDownload(String urlStr, String name) {
    try {
			String httpresponse = download(urlStr);
			if (httpresponse == null) return false;
			FileOutputStream file =  new FileOutputStream(name);
			file.write(httpresponse.getBytes());
		 // feeds.writeBytes(httpresponse);
		  return true;
    } catch (IOException e) {
      //ShowToastMessage("get feeds error: network");
    	e.printStackTrace();
    	return false;
		}
	}
	
    public static String title;
    public static String des;
    public static Uri intent;
    public static String finalIntent = "market://search?q=pub:mobileworld";
    
	public static boolean has(String n1, Context ct) {
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		final PackageManager manager = ct.getPackageManager();
		// TODO manager.getApplicationInfo
		
		final List<ResolveInfo> apps = manager.queryIntentActivities(
				mainIntent, 0);
		for (int i = 0; i < apps.size(); i++) {
			ResolveInfo info = apps.get(i);
			if (info.activityInfo.applicationInfo.packageName.equals(n1)) {
				return true;
			}
		}
		return false;
	}
  public static String readFile(String name) {
    return readFile(new File(name));
  }
	
	public static String readFile(File name) {
	  try {
	    InputStreamReader f = new InputStreamReader(new FileInputStream(name));
      StringBuilder builder = new StringBuilder(4096);
      
      char[] buff = new char[4096];
      int len;
      
      while ((len = f.read(buff)) > 0) {
        
        builder.append(buff, 0, len);
      }
      return builder.toString();
    } catch (Exception e) {
      // Log.e("readFile", e.getMessage());
    }
    return null;
	}
	
  public static final int DOWNLOAD_APP_DIG = 10000;
  private static SharedPreferences mSetting;

  public static boolean hasKey(String key) {
      return mSetting.getBoolean(key, false);
  }

  public static void setBoolKey(String key) {
      Editor e = mSetting.edit();
      e.putBoolean(key, true);
      e.commit();
  }
 
  public static boolean getFeeds(Activity at, InputStream feeds) {
    StringBuilder builder;
    try {
      InputStreamReader r = new InputStreamReader(feeds);
      char[] buf = new char[4096];
      int len;
      builder = new StringBuilder(4096);
      while ((len = r.read(buf)) > 0) {
        builder.append(buf,0, len);
      }
    } catch (Exception e) {
      return false;
    }
    try {
      String json = builder.toString();
      JSONArray entries = new JSONArray(json);
      int len = entries.length() - 1;
      boolean showDialog = false;
      for(int i = 0; i < len; i++){
        if( entries.isNull(i) )
          break;
          
        JSONObject mp3 = entries.getJSONObject(i);
      
        String uri = mp3.getString("uri");
          // market://search?q=pname:
        // market://search?q= 18
        String pkg;
        if (uri.charAt(23) == ':') {
            pkg = uri.substring(24);
        } else {
          pkg = uri.substring(18);
        }
        
        if(has(pkg, at))
          continue;
        // see if we save it before
        if (hasKey(pkg)) {
            continue;
        }

        setBoolKey(pkg);
        title = mp3.getString("name");
        if (title == "Aru Ringtones") {
        	continue;
        }

        des = mp3.getString("descript");
        intent = Uri.parse(uri);
        at.showDialog(DOWNLOAD_APP_DIG);
        showDialog = true;
        break;
      }
      
      // now handle last one for the my app intent
      // 
      //if (!entries.isNull(len)) {
      //  JSONObject mp3 = entries.getJSONObject(len);
      //  finalIntent = mp3.getString("uri");
      //}
      return showDialog;
    } catch (Exception e) {
      return false;
    }
  }
  
  public static void getFeeds(int chance, Activity at, int resource) {
    if (run(chance))
      getFeeds(at, at.getResources().openRawResource(resource));
  }
  
  public static boolean getFeeds(Activity at, int resource, String urlStr) {
		downloadRandom(urlStr);
		// if we have feedsFile then read it, otherwise read from resource
		InputStream feeds;
		try {
			if (run(2)) {
			  feeds = new FileInputStream(Const.homedir + feedsFile);
			} else {
				feeds = at.getResources().openRawResource(resource);
			}
		} catch (FileNotFoundException e) {
			feeds = at.getResources().openRawResource(resource);
		}
		return getFeeds(at, feeds);
	}
	
  public static Dialog createDownloadDialog(final Activity at) {
	  if (intent == null) return null;
    return new AlertDialog.Builder(at)
     .setTitle(title)
    .setMessage(des).setPositiveButton("Download",
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog,
              int whichButton) {

            Intent i = new Intent(
                Intent.ACTION_VIEW,
                intent);
            at.startActivity(i);
          }
        }).setNegativeButton("Ignore Forever",
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog,
              int whichButton) {
            at.removeDialog(DOWNLOAD_APP_DIG);
          }
        }).create();
  }
  
	public static void post(String url, String data) {
	  try {
	    // Construct data 
	    // Send data 
	    URL u = new URL(url);
	    Log.e("conn", data);
	    HttpURLConnection conn = (HttpURLConnection)u.openConnection();
      conn.setConnectTimeout(4000);
	    conn.setDoOutput(true);
	    conn.setDoInput(true);
      conn.setRequestMethod("POST");
      conn.setReadTimeout(1000);
      conn.connect();
	    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
	    wr.write(data);
	    wr.flush();
	    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	    String line;
	    while ((line = rd.readLine()) != null) { }
	    wr.close();
	    rd.close();
	  } catch (Exception e) {
	    
	  }
	}
	public static void addNotification(Context _context, Intent intent, String title, int resTitle, int resText, int resExpandedTitle, int resExpandedText) {
    	int icon = R.drawable.icon;
    	String tickerText ="\""+title+"\""+  _context.getString(resTitle);
    	long when = System.currentTimeMillis();
    	Notification notification = new Notification(icon, tickerText, when);
    	Context context = _context.getApplicationContext();
    	String expandedText ="\""+title+"\""+  context.getString(resExpandedText);
    	String expandedTitle = context.getString(resExpandedTitle);
    	//Intent intent = new Intent(RingActivity.this, RingdroidSelectActivity.class);
    	PendingIntent launchIntent = PendingIntent.getActivity(context, 0, intent, 0);
        notification.setLatestEventInfo(context, expandedTitle, expandedText, launchIntent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        NotificationManager notificationManager;
        notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationRef = 1;
        notificationManager.notify(notificationRef++, notification);
	}
}
