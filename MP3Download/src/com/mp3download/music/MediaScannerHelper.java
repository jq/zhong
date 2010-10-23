package com.mp3download.music;

import java.util.ArrayList;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

public class MediaScannerHelper {
	
	private ArrayList<MediaScannerNotifier> mScanners;
	
	public MediaScannerHelper() {
		mScanners = new ArrayList<MediaScannerNotifier>();
	}

	private class MediaScannerNotifier implements MediaScannerConnectionClient {
		private MediaScannerConnection mConnection;
		private String mPath;
		private String mMimeType;

		public MediaScannerNotifier(Context context, String path, String mimeType) {
			mPath = path;
			mMimeType = mimeType;
			mConnection = new MediaScannerConnection(context, this);
			mConnection.connect();
		}

		public void onMediaScannerConnected() {
			mConnection.scanFile(mPath, mMimeType);
		}

		public void onScanCompleted(String path, Uri uri) {
			if (mPath == null)
				return;
			if (mPath.equals(path)) {
				mConnection.disconnect();
				synchronized(mScanners) {
					mScanners.remove(this);
				}
			}
		}

	}
	
	public void ScanMediaFile(Context context, String musicPath) {
		synchronized(mScanners) {
			mScanners.add(new MediaScannerNotifier(context, musicPath, "audio/mpeg"));
		}
	}
}
