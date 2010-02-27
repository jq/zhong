package com.feebe.lib;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;


import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

public class Const {	
	// const same for all
  public static final long OneDay = 1000 * 3600 * 24;
  public static final long OneWeek = OneDay * 7;
  public static final long OneMonth = OneDay * 30;
  public static final long OneYear = OneDay * 365;

  // resources
	protected static int AdsViewID;
	protected static int LAYOUT_LIST;
	protected static int tabheader;
	protected static int tab_label;
	protected static int tab_image;
	protected static int no_result;
	protected static int no_sd;

	// other changed const var
	public static String QWName;
	public static String QWID;
	public static String SearchBase;
	public static Activity main;
    //public static Object obj;
   // public static DownloadAdapter mDownload;

  public static String contentDir;
  public static String jsondir;
  public static String homedir;
  public static String cachedir;
  public static final int ver = Integer.parseInt(Build.VERSION.SDK);
  private static String exception_base;
  private static final String exception_url="http://ggapp.appspot.com/ringtone/bug/";
  public static String pkg;
  public static void init() {
    File sdcardRoot = Environment.getExternalStorageDirectory();
    
    File homeDir = new File(sdcardRoot, "FeebeRings");
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
    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread thread, Throwable ex) {
        if (exception_base == null) {
          StringBuilder b = new StringBuilder(128);
          b.append("device=").append(Build.DEVICE).append("&model=")
            .append(Build.MODEL).append("&sdk=").append(Build.VERSION.SDK).append("&version=").append(pkg).append(getVersion()).append('&');
          exception_base = b.toString();
        }
        //PrintStream
        StackTraceElement[] stack = ex.getStackTrace();
        //ex.printStackTrace(err)
        String data = exception_base + "bug=" + URLEncoder.encode(ex.getMessage());
        for(int i = 0; i < stack.length; ++i) {
          data += stack[i].toString();
        }
        //s
        //+ ex.);
        
        Util.post(exception_url, data);
        main.finish();
      }
    });
  }
  private static int getVersion() {
    PackageInfo pInfo = null;
    try {
      pInfo =  main.getPackageManager().getPackageInfo(pkg, PackageManager.GET_META_DATA);
      return pInfo.versionCode;
    } catch (NameNotFoundException e) {
    }
    return 0;
  }

  private static void createDir(File dir) {
    if (!dir.mkdir()) {
      Toast.makeText(Const.main, no_sd,Toast.LENGTH_SHORT).show();
      return;
    }
  }

}
