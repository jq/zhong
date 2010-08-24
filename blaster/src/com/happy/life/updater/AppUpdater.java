package com.happy.life.updater;

import java.io.IOException;

import org.json.JSONException;

import com.happy.life.Constants;
import com.happy.life.Utils;
import com.happy.life.VersionUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

public class AppUpdater {
	
	private static final String APP_INFO_BASE = "http://chaowebs.appspot.com/appinfo/";
	
	// For testing
	// private static final String APP_INFO_BASE = "http://192.168.1.144:8080/appinfo/";
	
	// Returns an non-null value if we received a valid and new update.
	public static synchronized UpdateInfo checkUpdate(Context context) {
		String url = APP_INFO_BASE + context.getPackageName();
		try {
			String json = Utils.fetchHtmlPage(url, null);
			UpdateInfo update = new UpdateInfo(json);
			
			if (TextUtils.isEmpty(update.getUrl()) ||
				TextUtils.isEmpty(update.getVersion()) ||
				TextUtils.isEmpty(update.getMessage())) {
		    	com.util.Utils.D("Update: incomplete field");
				return null;
			}
			
			if (update.getSeq() <= VersionUtils.getVersionCode(context)) {
		    	com.util.Utils.D("new update is no newer than installed version");
				return null;
			}
			
			SharedPreferences setting = context.getSharedPreferences(Constants.PREFS_UPDATE, 0);
			if (update.getSeq() <= setting.getInt(Constants.UPDATE_SEQ, 0)) {
		    	com.util.Utils.D("Already received update.");
				return null;
			}
			
			Editor e = setting.edit();
			e.putInt(Constants.UPDATE_SEQ, update.getSeq());
			e.putString(Constants.UPDATE_URL, update.getUrl());
			e.commit();
					
			return update;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	// If we have a new update, return the url. Returns null otherwise.
	public static synchronized String getNewUpdateUrl(Context context) {
		SharedPreferences setting = context.getSharedPreferences(Constants.PREFS_UPDATE, 0);

		// versionCode contains the code we know from last update. If versionCode is
		// -1, it means we never received an update.
		int versionCode = setting.getInt(Constants.UPDATE_SEQ, -1);
		int myVersion = VersionUtils.getVersionCode(context);
		
		com.util.Utils.D("version code = " + versionCode);
		com.util.Utils.D("My version code = " + myVersion);
		
		if (versionCode == -1 || versionCode > myVersion) {
			return setting.getString(Constants.UPDATE_URL, "market://search?q=pname:" + context.getPackageName());
		} else {
			return null;
		}
	}
}
