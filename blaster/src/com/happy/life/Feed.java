package com.happy.life;

import com.limegroup.gnutella.settings.SharingSettings;

import org.json.JSONArray;
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
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Random;

public class Feed {
    public static final int DOWNLOAD_APP_DIG = 10000;
    
    private static final String urlString = "http://chaowebs.appspot.com/feeds/music_wizard_feed.txt";
    private static final String feedsFile = "feeds";

    private static Random generator = new Random();
    
    private static String title;
    private static String des;
    private static Uri intent;
    private static String finalIntent = "market://search?q=pub:mobileworld";
    
    private static SharedPreferences sSetting;
    
    private static boolean sFeedAlreadyRun = false;
    
    // Returns whether we actually ran feed.
    public static boolean runFeed(Activity at, int resource) {
    	if (sFeedAlreadyRun)
    		return false;
    	
	    // Show feeds 1/8 of the time.
        if (shouldRun(8)) {
        	sFeedAlreadyRun = true;
            sSetting = at.getPreferences(0);
            return getFeeds(at, resource, urlString);
        }
        
        return false;
    }
    
    public static Dialog createDownloadDialog(final Activity at) {
        if (intent == null) {
            intent = Uri.parse(finalIntent);
        }
        return new AlertDialog.Builder(at)
        .setTitle(title)
        .setMessage(des).setPositiveButton("Download",
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog,
                    int whichButton) {
                try {
                	Intent i = new Intent(
                			Intent.ACTION_VIEW, intent);
                	at.startActivity(i);
                } catch (Exception e) {
                	e.printStackTrace();
                }
            }
        }).setNegativeButton("Ignore Forever",
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog,
                    int whichButton) {
                at.removeDialog(DOWNLOAD_APP_DIG);
            }
        }).create();
    }
    
    
    public static boolean shouldRun(int chance) {
        int t = generator.nextInt();
        return t % chance == 0;
    }

    // TODO(zyu): This can use the util function in NetUtils.
    private static String download(String urlStr) {
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
        	e.printStackTrace();
            return null;
        }
    }

    private static boolean saveDownload(String urlStr, String filePath) {
        try {
            String httpresponse = download(urlStr);
            if (httpresponse == null)
            	return false;
            String tmpPath = filePath + ".tmp";
            FileOutputStream stream =  new FileOutputStream(tmpPath);
            stream.write(httpresponse.getBytes());
            
            new File(tmpPath).renameTo(new File(filePath));
            
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void downloadRandom(final String urlStr) {
        if (!shouldRun(20)) {
            return;
        }

        (new Thread() {
            public void run() {
                saveDownload(urlStr, SharingSettings.HOME.getAbsolutePath() + "/" + feedsFile);
            }
        }).start();
    }
    
    private static boolean getFeeds(Activity at, int resource, String urlStr) {
        downloadRandom(urlStr);
        
        // if we have feedsFile then read it, otherwise read from resource
        InputStream feeds;
        try {
            if (shouldRun(2)) {
                feeds = new FileInputStream(SharingSettings.HOME.getAbsolutePath() + "/" + feedsFile);
            } else {
                feeds = at.getResources().openRawResource(resource);
            }
        } catch (Exception e) {
            feeds = at.getResources().openRawResource(resource);
        }
        return getFeedsFromStream(at, feeds);
    }

    private static boolean hasKey(String key) {
        return sSetting.getBoolean(key, false);
    }

    private static void setBoolKey(String key) {
        Editor e = sSetting.edit();
        e.putBoolean(key, true);
        e.commit();
    }

    private static boolean getFeedsFromStream(Activity at, InputStream feeds) {
        StringBuilder builder;
        try {
            InputStreamReader r = new InputStreamReader(feeds);
            char[] buf = new char[4096];
            int len;
            builder = new StringBuilder(4096);
            while ((len = r.read(buf)) > 0) {
                builder.append(buf, 0, len);
            }
        } catch (Exception e) {
        	e.printStackTrace();
            return false;
        }
        
        try {
            String json = builder.toString();
            if (TextUtils.isEmpty(json))
            	return false;
            JSONArray entries = new JSONArray(json);
            int len = entries.length();
            if (len == 0)
            	return false;
            boolean showDialog = false;
            for (int i = 0; i < len; i++) {
                if( entries.isNull(i) )
                    break;

                JSONObject mp3 = entries.getJSONObject(i);
                String uri = mp3.getString("uri");
                // market://search?q=pname:
                // market://search?q= 18
                String pkg;
                if (uri.charAt(20) == ':') {
                  pkg = uri.substring(21);
                } else if (uri.charAt(23) == ':') {
                    pkg = uri.substring(24);
                } else {
                    pkg = uri.substring(18);
                }

                // See if we already installed.
                if (hasPackage(pkg, at))
                    continue;
                
                // see if we save it before
                if (hasKey(pkg)) {
                    continue;
                }

                title = mp3.getString("name");
                des = mp3.getString("descript");
                intent = Uri.parse(uri);
                
                setBoolKey(pkg);
                at.showDialog(DOWNLOAD_APP_DIG);
                showDialog = true;
                break;
            }
            return showDialog;
        } catch (Exception e) {
        	e.printStackTrace();
            return false;
        }
    }
    
    private static boolean hasPackage(String packageName, Context ct) {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final PackageManager manager = ct.getPackageManager();
        // TODO manager.getApplicationInfo

        final List<ResolveInfo> apps = manager.queryIntentActivities(
                mainIntent, 0);
        for (int i = 0; i < apps.size(); i++) {
            ResolveInfo info = apps.get(i);
            if (info.activityInfo.applicationInfo.packageName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}
