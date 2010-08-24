package com.util;

import com.limegroup.gnutella.Downloader;

public abstract class DownloadInfo {
    public static final int PENDING = 100;

    public static boolean isDownloading(int state) {
        return (state == Downloader.CONNECTING ||
                state == Downloader.DOWNLOADING ||
                state == Downloader.REMOTE_QUEUED ||
                state == Downloader.SAVING ||
                state == Downloader.IDENTIFY_CORRUPTION ||
                state == Downloader.QUEUED ||
                state == Downloader.PAUSED ||
                state == Downloader.WAITING_FOR_RETRY ||
                state == Downloader.WAITING_FOR_USER ||
                state == Downloader.WAITING_FOR_RESULTS ||
                state == Downloader.WAITING_FOR_CONNECTIONS ||
                state == Downloader.ITERATIVE_GUESSING);
    }

  
	protected int mTotalBytes;
	protected int mCurrentBytes;
	protected String mError;
	
	public abstract int getState();
	
	public boolean showToastForLongPress() {
	  return false;
	}
	
	public boolean ableToResume() {return false;}
	
	public boolean ableToRetry() {return true;}
	
	public boolean isScheduled() {
	  return true;
	}
	public abstract void download(DownloadService mDownloadService);
	
	public abstract void resumeDownload();
	
	public void setScheduled(boolean scheduled) {
	  
	}
    public abstract void setFailed(boolean failed);
  
    public abstract boolean hasFailed();
	
	public boolean pendingFailed() {
	  return false;
	}
	public abstract boolean valid();
	
	public boolean same(DownloadInfo info) {
	  return getTarget().equals(info.getTarget());
	}
	public abstract String getTarget();
	
	public abstract void stopDownload();
	public abstract void pauseDownload();
	public abstract void deleteDownload();
	
	public abstract String getFileName();
	
	public abstract SearchResult getSearchResult();
	
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
