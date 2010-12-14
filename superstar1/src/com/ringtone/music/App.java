package com.ringtone.music;

import java.io.File;

import com.ringtone.music.download.DownloadService;

import android.app.Application;
import android.content.Intent;
import android.os.Environment;
import android.widget.Toast;

public class App extends Application {
	
	private static File sBaseDirPath;
	private static File sMp3Path;
	private static File sJsonPath;
	
	public static String getMp3Path() {
		if (sMp3Path != null)
			return sMp3Path.getAbsolutePath();
		return null;
	}
	
	public static File getBaseDir() {
	    return sBaseDirPath;
	}
	
	@Override
    public void onCreate() {
		File sdCardRoot = Environment.getExternalStorageDirectory();

		File singerPath = new File(sdCardRoot, "ringtonehelper");
		if (!singerPath.exists() && !singerPath.mkdir()) {
			Toast.makeText(this, R.string.create_app_dir_error, Toast.LENGTH_LONG).show();
			return;
		}
		
		sBaseDirPath = new File(singerPath, getString(R.string.app_name));
		if (!sBaseDirPath.exists() && !sBaseDirPath.mkdir()) {
			Toast.makeText(this, R.string.create_app_dir_error, Toast.LENGTH_LONG).show();
			return;
		}
			
		sMp3Path = new File(sBaseDirPath, "mp3");
		if (!sMp3Path.exists() && !sMp3Path.mkdir()) {
			Toast.makeText(this, R.string.create_mp3_dir_error, Toast.LENGTH_LONG).show();
			return;
		}
		
		sJsonPath = new File(sBaseDirPath, "json");
		if (!sJsonPath.exists() && !sJsonPath.mkdir()) {
		    Toast.makeText(this, R.string.create_mp3_dir_error, Toast.LENGTH_LONG).show();
		    return;
		}
		
		// Start service
		Intent intent = new Intent(this, DownloadService.class);
		startService(intent);
    }

	public static String getJsonPath() {
		// TODO Auto-generated method stub
		  if (sJsonPath != null)
			    return sJsonPath.getAbsolutePath();
		return null;
	}
}


