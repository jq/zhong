package com.feebe.lib;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.feebe.rings.R;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class Const {	
	//public static DownloadProvider downloadDb;
  public static DbAdapter dbAdapter;
	// const same for all
  public static final String expire = "expire";
  public static final long OneDay = 1000 * 3600 * 24;
  public static final long OneWeek = OneDay * 7;
  public static final long OneMonth = OneDay * 30;
  public static final long OneYear = OneDay * 365;

  // resources
//	protected static int AdsViewID;
	protected static int LAYOUT_LIST = R.layout.list;
	protected static int tabheader = R.layout.tabheader;
	protected static int tab_label = R.id.tab_label;
	protected static int tab_image= R.id.tab_image;;
	protected static int no_result = R.string.no_result;
	protected static int no_sd;
	protected static int dlprogress_message= R.string.dlprogress_message;
	protected static int dlprogress_title = R.string.dlprogress_title;
	
	protected static int icon = R.drawable.ring;
	protected static int app_name= R.string.app_name;
	protected static int notification_text_failed= R.string.notification_text_failed;;
	protected static int notification_text_finish = R.string.notification_text_finish;
	
	// other changed const var
	public static String QWName;
	public static String QWID;
	public static Activity main = null;
    //public static Object obj;
   // public static DownloadAdapter mDownload;
	
  public static final int NO_FILE_KIND = -1;
  public static final int FILE_KIND_MUSIC = 0;
  public static final int FILE_KIND_ALARM = 1;
  public static final int FILE_KIND_NOTIFICATION = 2;
  public static final int FILE_KIND_RINGTONE = 3;

  protected static String appname;
  public static String contentDir;
  public static String jsondir;
  public static String homedir;
  public static String cachedir;
  public static final int ver = Integer.parseInt(Build.VERSION.SDK);
  
  public static void init(Activity c) {
	main = c;
  	//downloadDb = new DownloadProvider(c);
    File sdcardRoot = Environment.getExternalStorageDirectory();
    dbAdapter = new DbAdapter(c);
    File homeDir = new File(sdcardRoot, appname);
    File songdir = new File(homeDir, "mp3");
    File jdir = new File(homeDir, "json");
    File cache = new File(homeDir, "cache");
    if (!cache.exists()) {
      createDir(homeDir);
      createDir(songdir);
      createDir(jdir);
      createDir(cache);
      // Create an empty .nomedia file to prevent Gallery caching of images.
      File noMedia = new File(jdir, ".nomedia");
      try {
        noMedia.createNewFile();
      } catch (IOException e) {
        // Log.e("nomedia", e.getMessage());
      }
    }
    homedir = homeDir.getAbsolutePath() + "/";
    contentDir = songdir.getAbsolutePath() + "/";;
    jsondir = jdir.getAbsolutePath() + "/";
    cachedir = cache.getAbsolutePath() + "/";
    // Log.e("cache", cachedir);
    /*
    if (!homeDir.canWrite()) {
      Toast.makeText(Ring.main, R.string.no_sd_space,Toast.LENGTH_SHORT).show();
      return;
    }
    */
    //exceptionHandler();
  }
  
  public static void trimCache() {
    new Thread(new Runnable() {

      @Override
      public void run() {
        File dir = new File(cachedir);
        if(dir!= null && dir.isDirectory()){
            File[] children = dir.listFiles();
            if (children == null) {
                // Either dir does not exist or is not a directory
            } else {
                File temp;
                for (int i = 0; i < children.length; i++) {
                    temp = children[i];
                    temp.delete();
                }
            }
        }
        
        //dir.delete();
      }
      
    }).start();
  }

  public static String pkg;
  /*
  private static void reportCrash(Throwable ex) {
    try {
      HttpPost report = new HttpPost("http://ggapp.appspot.com/ringtone/bug/");
      List<BasicNameValuePair> formparams = new ArrayList<BasicNameValuePair>();
      formparams.add(new BasicNameValuePair("device", Build.DEVICE));
      formparams.add(new BasicNameValuePair("model", Build.MODEL));
      formparams.add(new BasicNameValuePair("sdk", Build.VERSION.SDK));

      formparams.add(new BasicNameValuePair("version", pkg + getVersion()));
      String data = ex.getMessage();
      Throwable real = ex.getCause();
      if (real == null) {
        return;
      }
      StackTraceElement[] stack = real.getStackTrace();
      for(int i = 0; i < stack.length; ++i) {
        data += stack[i].toString();
      }
      formparams.add(new BasicNameValuePair("bug", data));
      UrlEncodedFormEntity entity;
   
      entity = new UrlEncodedFormEntity(formparams, "UTF-8");
      report.setEntity(entity);
      HttpClient httpclient = new DefaultHttpClient();
      httpclient.execute(report);
      httpclient.getConnectionManager().shutdown();        
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  
  private static void exceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread thread, Throwable ex) {
        reportCrash(ex);
        main.finish();
      }
    });
  }
  
  private static int getVersion() {
    PackageInfo pInfo = null;
    try {
      pInfo =  main.getPackageManager().getPackageInfo(pkg, PackageManager.GET_META_DATA);
      return pInfo.versionCode;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return 0;
  }
*/
  private static void createDir(File dir) {
    if (!dir.mkdir()) {
      Toast.makeText(Const.main, no_sd,Toast.LENGTH_SHORT).show();
      return;
    }
  }

}
