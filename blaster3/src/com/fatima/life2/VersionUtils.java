package com.fatima.life2;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * 
 * @version 2009-01-15
 * @author Peli
 *
 */
public class VersionUtils {
    private static final String urlString = "http://chaowebs.appspot.com/";
	private static final String TAG = "VersionUtils";
	
	/*
    public void checkUpdate(Context context) {
        String pkg = context.getPackageName();
        int version;
        try {
          PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
          version = pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package name not found", e);
            return;
        };
        // get version from url
        String newVer = Feed.download(urlString + pkg);
        if (newVer == null) {
            return;
        }
        
        int newVersion = Integer.valueOf(newVer);
        if (newVersion > version) {
            // pop up upgrade buttons.
        }
    }
    */
    
    
    
	/**
	 * Get current version number.
	 * 
	 * @return
	 */
	public static String getVersionNumber(Context context) {
		String version = "?";
		try {
			PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			version = pi.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(TAG, "Package name not found", e);
		};
		return version;
	}
	
	public static int getVersionCode(Context context) {
		int code = 0;
		try {
			PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			code = pi.versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(TAG, "Package name not found", e);
		};
		return code;
	}
	
	/**
	 * Get application name.
	 * 
	 * @return
	 */
	public static String getApplicationName(Context context) {
		String name = "?";
		try {
			PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			name = context.getString(pi.applicationInfo.labelRes);
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(TAG, "Package name not found", e);
		};
		return name;
	}

}
