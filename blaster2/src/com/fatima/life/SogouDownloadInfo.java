package com.fatima.life;

import java.util.ArrayList;


public class SogouDownloadInfo extends DownloadInfo {
	
//	public static final int STATUS_PENDING = 0;
//	public static final int STATUS_FINISHED = 1;
//	public static final int STATUS_FAILED = 2;
//	public static final int STATUS_DOWNLOADING = 3;
//	public static final int STATUS_STOPPED = 4;
    public static final int PENDING = 100;
  
	private SogouSearchResult mSearchResult;
	private String mTarget;
	private int mStatus;
	private Thread mThread;
	
	public SogouDownloadInfo(SogouSearchResult result) {
		mSearchResult = result;
		mTarget = SogouSearchResult.downloadPath(mSearchResult);
		mStatus = PENDING;
		mTotalBytes = 0;
		mCurrentBytes = 0;
	}
	
	public SogouSearchResult getSogouSearchResult() {
		return mSearchResult;
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
	
	public void setThread(Thread thread) {
		mThread = thread;
	}
	
	public Thread getThread() {
		return mThread;
	}

    @Override
    public int getState() {
      // TODO Auto-generated method stub
      return mStatus;
    }
}
