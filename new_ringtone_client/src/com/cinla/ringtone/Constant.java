package com.cinla.ringtone;

import java.io.File;
import java.io.IOException;

import com.latest.ringtone.R;

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
	public static final String RATE = "rate";
	public static final String USER_RATE = "user_rate";
	
	public static final String BASE_URL = "http://bingliu630.appspot.com";
//	public static final String BASE_URL = "http://10.0.2.2:8888";
	public static final String SEARCH_URL = "/ringtoneserver/search?q=";
	public static final String RATE_URL = "/ringtoneserver/";
	public static final String COUNT_URL = "/ringtoneserver/allsong";
	
	public static final String SEARCH_TYPE = "search_type";
	public static final int TYPE_EMPTY = 0;
	public static final int TYPE_KEY = 1;
	public static final int TYPE_TOP_DOWNLOAD = 2;
	public static final int TYPE_NEWEST = 3;
	public static final int TYPE_ARTIST = 4;
	public static final int TYPE_CATEGORY = 5;
	public static final String HIDE_SEARCHBAR = "hide_searchbar";
	
	public static final String QUERY = "query";
	
	public static final int EACH_MAX_RESULTS_NUM = 10;
	
	public static final int NO_FILE_KIND = -1;
	public static final int FILE_KIND_MUSIC = 0;
	public static final int FILE_KIND_ALARM = 1;
	public static final int FILE_KIND_NOTIFICATION = 2;
	public static final int FILE_KIND_RINGTONE = 3;
	
	public static final long ONE_HOUR = 60*60*1000;
	public static final long ONE_DAY = ONE_HOUR*24;
	public static final long ONE_WEEK = ONE_DAY*7;
//	public static final long ONE_WEEK = 0;
	public static final long ONE_MONTH = ONE_DAY*30;
	public static final long ONE_YEAR = ONE_DAY*365;
	
	public static final int MIN_RESPONSE_LENGTH = 10;
	
	public static final int MIN_FILE_LENGTH = 1024;
	
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
	
	public static final String[] CATEGORIES_NAME = { "Christian and Gospel", "Metal", "Holiday", "R&B Soul",
		 "Techno", "Pop", "Rock", "Video Games", "Jazz", "International",
		 "Hip-Hop", "Movies and TV", "Latin", "Blues", "Sound Effects",
		 "Classical", "Comedy", "Country", "Reggae"};

	public static final String[] CATEGORIES_VALUE = { "Christian and Gospel", "Metal", "Holiday", "Rnb_Soul",
		  "Techno", "Pop", "Rock", "Vedio Games", "Jazz", "Internation",
		  "Hip-Hop", "Movies and TV", "Latin", "Blues", "Sound Effects", 
		  "Classical", "Comedy", "Country", "Reggae"};
	
	//for lastfm chart
//	public static final String LASTFM_API_KEY = "ffffffff";
	public static final String LASTFM_API_KEY = "047394ee33f2383f2ea559d4c1d640cb";
//	public static final String LASTFM_API_KEY = "b25b959554ed76058ac220b7b2e0a026";
	public static final int CHART_TYPE_TOPTRACKS = 0;
	public static final int CHART_TYPE_TOPARTISTS = 1;
	public static final int CHART_TYPE_LOVEDTRACKS = 2;
	public static final int CHART_TYPE_TOPTAGS = 3;
	public static final int CHART_TYPE_HYPEDTRACKS = 4;
	public static final int CHART_TYPE_HYPEDARTISTS = 5;
	public static final String CHART_TYPE = "chart_tpye";
	public static final String[] CHART_TYPE_NAME = {"Top Tracks", "Top Artist", "Loved Tracks", "Top Tags", "Hyped Tracks", "Hyped Artists"};

	
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
