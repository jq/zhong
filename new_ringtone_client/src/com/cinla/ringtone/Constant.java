package com.cinla.ringtone;

import java.io.File;
import java.io.IOException;

import android.R.integer;
import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

public class Constant {

	public static String sAppName;
	
	public static final String UUID	= "uuid";
	public static final String TITLE = "title";
	public static final String CATEGORY = "category";
	public static final String AVG_RATE = "avg_rate";
	public static final String S3URL = "s3url";
	public static final String IMAGE = "image";
	public static final String ARTIST = "artist";
	public static final String DOWNLOAD_COUNT = "download_count";
	public static final String SIZE = "size";
	public static final String ADD_DATE = "add_date";
	
	public static final String BASE_URL = "http://bingliu630.appspot.com";
//	public static final String BASE_URL = "http://10.0.2.2:8888";
	public static final String SEARCH_URL = "/ringtoneserver/search?q=";
	
	public static final String SEARCH_TYPE = "search_type";
	public static final int TYPE_KEY = 0;
	public static final int TYPE_TOP_DOWNLOAD = 1;
	public static final int TYPE_NEWEST = 2;
	public static final int TYPE_ARTIST = 3;
	public static final int TYPE_CATEGORY = 4;
	
	public static final String QUERY = "query";
	
	public static final int EACH_MAX_RESULTS_NUM = 10;
	
	public static final int NO_FILE_KIND = -1;
	public static final int FILE_KIND_MUSIC = 0;
	public static final int FILE_KIND_ALARM = 1;
	public static final int FILE_KIND_NOTIFICATION = 2;
	public static final int FILE_KIND_RINGTONE = 3;
	
	//key of intent for MusicInfo object
	public static final String MUSIC_INFO = "music_info";
	
	public static final String ITEM_TITLE = "ITEM_TITLE";
	
	private static final String MUSIC_DIR = "music";
	private static final String OBJ_DIR = "obj";
	private static final String CACHE_DIR = "cache";
	public static String sHomeDir;
	public static String sObjDir;
	public static String sMusicDir;
	public static String sCacheDir;
	
	public static void init(Context context) {
		sAppName = context.getString(R.string.app_name);
		File sdCardRoot = Environment.getExternalStorageDirectory();
	    File homeDir = new File(sdCardRoot, sAppName);
	    File musicDir = new File(homeDir, MUSIC_DIR);
	    File objDir = new File(homeDir, OBJ_DIR);
	    File cacheDir = new File(homeDir, CACHE_DIR);
	    if (!cacheDir.exists() || !homeDir.exists() || !musicDir.exists() || !objDir.exists()) {
	    	createDir(homeDir, context);
	    	createDir(objDir, context);
	    	createDir(musicDir, context);
	    	createDir(cacheDir, context);
	    }
	    sHomeDir = homeDir.getAbsolutePath() + "/";
	    sMusicDir = musicDir.getAbsolutePath() + "/";
	    sObjDir = objDir.getAbsolutePath() + "/";
	    sCacheDir = cacheDir.getAbsolutePath() + "/";
	}
	
	private static void createDir(File dir, Context context) {
		if (!dir.mkdir()) {
			Toast.makeText(context, context.getString(R.string.no_sd), Toast.LENGTH_SHORT).show();
			return;
		}
	}
	
}
