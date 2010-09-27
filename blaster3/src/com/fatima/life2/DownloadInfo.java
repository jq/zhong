package com.fatima.life2;

import com.limegroup.gnutella.Downloader;

public abstract class DownloadInfo {
    public static final int PENDING = 100;
  
	protected int mTotalBytes;
	protected int mCurrentBytes;
	protected String mError;
	
	public abstract int getState();
	
	public void setTotalBytes(int totalBytes) {
		mTotalBytes = totalBytes;
	}
	
	public int getTotalBytes() {
		return mTotalBytes;
	}
	
	public void setCurrentBytes(int currentBytes) {
		mCurrentBytes = currentBytes;
	}
	
	public int getCurrentBytes() {
		return mCurrentBytes;
	}
	
	
	public void setError(String error) {
		mError = error;
	}
	
	public String getError() {
		return mError;
	}
}
