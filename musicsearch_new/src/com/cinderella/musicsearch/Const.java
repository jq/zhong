package com.cinderella.musicsearch;

import java.io.File;
import java.io.IOException;

import android.R.integer;
import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

public class Const {
	public static final String QUERY = "QUERY";
	public static final String INDEX = "INDEX";
	public static final String Search_Url_Base = "http://msearchtest.appspot.com/search?key=";
	public static final String Link_Url_Base = "http://msearchtest.appspot.com/downloadlink?key=";
	public static final String QQ_Search_Url_Base = "http://cgi.music.soso.com/fcgi-bin/m.q?t=1&w=";
	public static final String QQ_Link_Url = "http://cgi.music.soso.com/fcgi-bin/fcg_download_song.q";
	public static String app_name;
	private static final String MusicDir = "music";
	private static final String JsonDir = "json";
	private static final String CacheDir = "cache";
	
	public static final long MIN_MP3_LENGTH = 1024*100;	//100k
	
	private static String sHomeDir;
	public static String sMusicDir;
	public static String sJsonDir;
	public static String sCacheDir;
	
	public class JSON {
		public static final String Downlink = "downlink";
		public static final String Singer = "singer";
		public static final String Size = "size";
		public static final String Songname = "songname";
		public static final String Album = "album";
	}
	
	
	public static void init(Context context) {
		app_name = context.getString(R.string.app_name);
		File sdCardRoot = Environment.getExternalStorageDirectory();
	    File homeDir = new File(sdCardRoot, app_name);
	    File musicDir = new File(homeDir, MusicDir);
	    File jsonDir = new File(homeDir, JsonDir);
	    File cacheDir = new File(homeDir, CacheDir);
	    if (!cacheDir.exists() || !homeDir.exists() || !jsonDir.exists() || !musicDir.exists()) {
	    	createDir(homeDir, context);
	    	createDir(musicDir, context);
	    	createDir(jsonDir, context);
	    	createDir(cacheDir, context);
	    }
	    sHomeDir = homeDir.getAbsolutePath() + "/";
	    sMusicDir = musicDir.getAbsolutePath() + "/";;
	    sJsonDir = jsonDir.getAbsolutePath() + "/";
	    sCacheDir = cacheDir.getAbsolutePath() + "/";
	}
	
	private static void createDir(File dir, Context context) {
		if (!dir.mkdir()) {
			Toast.makeText(context, context.getString(R.string.no_sd),Toast.LENGTH_SHORT).show();
			return;
		}
	}
}
