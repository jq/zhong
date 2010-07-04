package com.ringdroid;

import java.io.File;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

public class Constants {
	public static final String ADS_KEYWORD = "";
	private static File sBaseDirPath;
	public static final String APP_NAME = "ringdroid-g56rajjb";
	
	public static void init(Context context) {
		File sdCardRoot = Environment.getExternalStorageDirectory();
		sBaseDirPath = new File(sdCardRoot, APP_NAME);
		if (!sBaseDirPath.exists() && !sBaseDirPath.mkdir()) {
			Toast.makeText(context, "Create feed dir error!", Toast.LENGTH_LONG).show();
			return;
		}
	}
	
	public static File getBaseDir() {
	    return sBaseDirPath;
	}
}
