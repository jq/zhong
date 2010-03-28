package com.macrohard.musicbug;

import java.io.File;
import java.util.ArrayList;

import com.feebe.lib.DefaultDownloadListener;

public interface Mp3FetcherInterface {
	public static class Mp3FetcherException extends Exception {
		private static final long serialVersionUID = 4287875835890330413L;
		private String mError;
		
		public Mp3FetcherException(String error) {
			mError = error;
		}
		public String getError() {
			return mError;
		}
	}
	
	public boolean listDone();
	
	// This is run synchronously.
	public ArrayList<MP3Info> getNextListBatch() throws Mp3FetcherException;
	public void resetList();
	
	public void downloadMp3(MP3Info mp3, String saveFile, DefaultDownloadListener listener) throws Mp3FetcherException;

}
