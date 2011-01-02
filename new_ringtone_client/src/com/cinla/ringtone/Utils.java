package com.cinla.ringtone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLEncoder;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

public class Utils {
	
	private static final String TAG = "New Ringtone Client";
	private static final boolean DEBUG = true;
	
	public static void D(String msg) {
		if (DEBUG) {
			Log.d(TAG, msg);
		}
	}
	
	public static boolean isEclairOrLater() {
		return Build.VERSION.SDK.compareTo("5") >=0;
	}

	public static boolean isCupcakeOrBefore() {
		return Build.VERSION.SDK.compareTo("3") <=0;
	}

	public static Uri saveToMediaLib(String title, String outPath, long length, String artist, ContentResolver cr) {
		String mimeType = "audio/mpeg";
		ContentValues values = new ContentValues();
		// // // Log.e("save", " title " + title + " out " + outPath +
		// " artist " + artist);
		values.put(MediaStore.MediaColumns.DATA, outPath);
		values.put(MediaStore.MediaColumns.TITLE, title);
		values.put(MediaStore.MediaColumns.SIZE, length);
		values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);

		values.put(MediaStore.Audio.Media.ARTIST, artist);
		values.put(MediaStore.Audio.Media.COMPOSER, Constant.sAppName);
		// values.put(MediaStore.Audio.Media.DURATION, duration);
		values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
		values.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
		values.put(MediaStore.Audio.Media.IS_ALARM, true);
		values.put(MediaStore.Audio.Media.IS_MUSIC, true);

		// Insert it into the database
		Uri uri = MediaStore.Audio.Media.getContentUriForPath(outPath);
		try {
			final Uri newUri = cr.insert(uri, values);
			// // // Log.e("save", " ok ");
			// TODO: do we need this?
			// setResult(RESULT_OK, new Intent().setData(newUri));
			return newUri;
		} catch (Exception e) {

		}
		return null;
	}

	public static String encodeUrlTail(String url) {
		String fileName = url.substring(url.lastIndexOf('/')+1);
		url = url.substring(0, url.lastIndexOf('/')+1);
		fileName = URLEncoder.encode(fileName);	
		return url+fileName;
	}
	
	public static boolean writeToDisk(MusicInfo musicInfo) {
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(musicInfo.getObjFilePath()));
			oos.writeObject(musicInfo);
			oos.flush();
			oos.close();
		} catch (Exception e) {
			Utils.D("Write obj to file failed. "+e.getMessage());
			return false;
		} 
		return true;
	}
	
	public static MusicInfo readMusicInfoFromFile(String path) {
		ObjectInputStream ois = null;
		MusicInfo musicInfo = null;
		try {
			ois = new ObjectInputStream(new FileInputStream(path));
			musicInfo = (MusicInfo)ois.readObject();
			ois.close();
			return musicInfo;
		} catch (Exception e) {
			Utils.D("Read obj from file failed.");
			return null;
		}
	}
	
	public static String musicPathToObjPath(String musicPath) {
		String objFilePath = null;
		try {
		String fileName = musicPath.substring(musicPath.lastIndexOf('/')+1, musicPath.lastIndexOf('.'));
		objFilePath = Constant.sObjDir+fileName;
		} catch (Exception e) {
			return null;
		}
		return objFilePath;
	}

}
