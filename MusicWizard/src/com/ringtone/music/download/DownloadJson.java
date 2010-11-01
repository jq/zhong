package com.ringtone.music.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import com.ringtone.music.App;

import android.util.Log;


public class DownloadJson {
  public static final long OneDay = 1000 * 3600 * 24;
  public static final long OneWeek = OneDay * 7;
  public static final long OneMonth = OneDay * 30;
  public static final long OneYear = OneDay * 365;
  
  
  public static JSONObject getJsonFromUrl(String url, long expire) {
    File cache = null;
    String data = null;
    if (expire > 0) {
      cache = new File(App.getJsonPath(), getHashcode(url));
      if (inCache(url, expire, cache)) {
        data = readFile(cache);
      }
    }
    boolean newDownload = data == null || data.length() == 0;
    if (newDownload) {
      data = download(url);
    }
    if (data != null) {
      try {
        JSONObject obj;
        obj = new JSONObject(data);
        if (obj != null) {
            if (expire > 0 && newDownload)
            saveFile(data, cache);
          return obj;
        } 
      } catch (JSONException e) {
        //Log.e("getJsonFromUrl", e.getMessage());
      }
    }
    //Const.noResultToast();
    return null;
  }
  
  public static void saveFile(String content, File name) {
    try {
      name.createNewFile();
      FileOutputStream file =  new FileOutputStream(name);
      file.write(content.getBytes());
    } catch (IOException e) {
      // Log.e("saveFile", e.getMessage() + " file "+ name.getAbsolutePath() + " cache dir " + Const.cachedir);
    }
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
        // Log.e("download", e.getMessage());
        return null;
      }
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
  
  
  private static final String UNUSED1 = "http://";
  public static String getHashcode(String str) {
    String s;
    if (str.startsWith(UNUSED1)) {
    s = str.substring(UNUSED1.length());
    } else {
      s = str;
    }
    int hash = s.hashCode();
    return String.valueOf(hash);
  }
}
