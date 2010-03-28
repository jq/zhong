package com.macrohard.musicbug;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

// A singleton to manage all the necessary files.
public class FileManager {
	private static FileManager sInstance;

	File mHomeDir;
	
    String mHomeDirPath;
    String mContentDirPath;
    String mJsonDirPath;
    String mCacheDirPath;
    
    String getHomeDir() {
    	return mHomeDirPath;
    }
    
    String getContentDir() {
    	return mContentDirPath;
    }
    
    String getJsonDirPath() {
    	return mJsonDirPath;
    }
    
    String getCacheDirPath() {
    	return mCacheDirPath;
    }
	
	public static FileManager getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new FileManager(context);
		}
		return sInstance;
	}
	
	private String createDirectory(String name) {
		File dir = new File(mHomeDir, name);
		if (!dir.exists()) {
			dir.mkdir();  // TODO: Check return value?
		}
		return dir.getAbsolutePath();
	}
	
	private FileManager(Context context) {
		File sdCardRoot = Environment.getExternalStorageDirectory();
		File mHomeDir = new File(sdCardRoot, Const.APP_BASE_DIR);
		
		if (!mHomeDir.exists() && !mHomeDir.mkdir()) {
			Toast.makeText(context, R.string.no_sd, Toast.LENGTH_LONG).show();
			return;
		}
		
		mHomeDirPath = mHomeDir.getAbsolutePath() + "/";

		mContentDirPath = createDirectory("mp3") + "/";
		mJsonDirPath = createDirectory("json") + "/";
		mCacheDirPath = createDirectory("cache") + "/";

		// Create an empty .nomedia file to prevent Gallery caching of images.
		File noMedia = new File(mJsonDirPath, ".nomedia");
		try {
	        noMedia.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
