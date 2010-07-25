package com.fungame.music.download;

import java.util.ArrayList;

import com.fungame.music.IMusicSearcher;
import com.fungame.music.MusicInfo;

public class DownloadInfo {
	
	public static final int STATUS_PENDING = 0;
	public static final int STATUS_FINISHED = 1;
	public static final int STATUS_FAILED = 2;
	public static final int STATUS_DOWNLOADING = 3;
	public static final int STATUS_STOPPED = 4;
	
	private MusicInfo mMusicInfo;
	private IMusicSearcher mMusicSearcher;
	private String mTarget;
	private int mStatus;
	private int mTotalBytes;
	private int mCurrentBytes;
	private Thread mThread;
	private String mError;
	
	public DownloadInfo(MusicInfo info, IMusicSearcher searcher) {
		mMusicInfo = info;
		mTarget = MusicInfo.downloadPath(info);
		mMusicSearcher = searcher;
		mStatus = STATUS_PENDING;
		mTotalBytes = 0;
		mCurrentBytes = 0;
	}
	
	public MusicInfo getMusicInfo() {
		return mMusicInfo;
	}
	
	public IMusicSearcher getMusicSearcher() {
	  return mMusicSearcher;
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
