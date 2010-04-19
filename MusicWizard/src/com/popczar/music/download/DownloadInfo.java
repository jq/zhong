package com.popczar.music.download;

public class DownloadInfo {
	
	public static final int STATUS_PENDING = 0;
	public static final int STATUS_FINISHED = 1;
	public static final int STATUS_FAILED = 2;
	public static final int STATUS_DOWNLOADING = 3;
	public static final int STATUS_STOPPED = 4;
	public static final int STATUS_STOPPING = 5;
	
	private String mSource;
	private String mTarget;
	private int mStatus;
	private int mTotalBytes;
	private int mCurrentBytes;
	private Thread mThread;
	private String mError;
	
	public DownloadInfo(String source, String target) {
		mSource = source;
		mTarget = target;
		mStatus = STATUS_PENDING;
		mTotalBytes = 0;
		mCurrentBytes = 0;
	}
	
	public String getSource() {
		return mSource;
	}
	
	public String getTarget() {
		return mTarget;
	}
	
	public void setStatus(int status) {
		mStatus = status;
	}
	
	public int getStatus() {
		return mStatus;
	}
	
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
	
	public void setThread(Thread thread) {
		mThread = thread;
	}
	
	public Thread getThread() {
		return mThread;
	}
	
	public void setError(String error) {
		mError = error;
	}
	
	public String getError() {
		return mError;
	}
}
