package com.ringdroid;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Random;

public class Feed {
	private static final String urlString = "http://chaowebs.appspot.com/feeds/music_wizard_feed.txt";
	private static final String feedsFile = "feeds";

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

	private static boolean saveDownload(String urlStr, File file) {
		try {
			String httpresponse = download(urlStr);
			if (httpresponse == null) return false;
			FileOutputStream stream =  new FileOutputStream(file);
			stream.write(httpresponse.getBytes());
			// feeds.writeBytes(httpresponse);
			return true;
		} catch (IOException e) {
			//ShowToastMessage("get feeds error: network");
			e.printStackTrace();
			return false;
		}
	}

	private static void downloadRandom(final String urlStr) {
		if (!run(20)) {
			return;
		}

		(new Thread() {
			public void run() {
				saveDownload(urlStr, new File(Constants.getBaseDir(),feedsFile));
			}
		}).start();
	}
	
	public static boolean getFeeds(Activity at, int resource, String urlStr) {
		downloadRandom(urlStr);
		// if we have feedsFile then read it, otherwise read from resource
		InputStream feeds;
		try {
			if (run(2)) {
				feeds = new FileInputStream(Constants.getBaseDir().getAbsolutePath() + "/" +feedsFile);
			} else {
				feeds = at.getResources().openRawResource(resource);
			}
		} catch (FileNotFoundException e) {
			feeds = at.getResources().openRawResource(resource);
		}
		return getFeeds(at, feeds);
	}

	public static String title;
	public static String des;
	public static String intent;
	//public static String finalIntent = "market://search?q=pub:mobileworld";
	public static final int DOWNLOAD_APP_DIG = 10000;

	public static Dialog createDownloadDialog(final Activity at) {
		return new AlertDialog.Builder(at)
		.setTitle(title)
		.setMessage(des).setPositiveButton("Download",
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,
					int whichButton) {

				Intent i = new Intent(
						Intent.ACTION_VIEW,
						Uri.parse(intent));
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
			int len = entries.length();
			boolean showDialog = false;
			for (int i = 0; i < len; i++){
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

				if (has(pkg, at))
					continue;

				String sdkver = android.os.Build.VERSION.SDK;
				try{
					String ver = mp3.getString("v");
					if (!ver.equals(sdkver))
						continue;
				} catch(JSONException e) {
					e.printStackTrace();
				}   

				// see if we save it before
				if (hasKey(pkg)) {
					continue;
				}

				title = mp3.getString("name");
				des = mp3.getString("descript");
				intent = uri;
				setBoolKey(pkg);
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
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
	}
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


}
