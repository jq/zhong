package com.happy.life;

import android.text.TextUtils;

public class MusicInfo {

	private String mTitle;
	private String mArtist;
	private String mAlbum;
	private String mUrl;
	private String mDownloadUrl;
	
	private String mDisplaySize;
	private int mFileSize;
	private String mLyricUrl;
	private String mType;
	
	public void setTitle(String title) {
		mTitle = title;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public void setArtist(String artist) {
		mArtist = artist;
	}
	
	public String getArtist() {
		if (TextUtils.isEmpty(mArtist)) {
			return "<Unknown>";
		}
		return mArtist;
	}
	
	public void setAlbum(String album) {
		mAlbum = album;
	}
	
	public String getAlbum() {
		return mAlbum;
	}
	
	public void setUrl(String url) {
		mUrl = url;
	}
	
	public String getUrl() {
		return mUrl;
	}
	
	public void setDownloadUrl(String url) {
		mDownloadUrl = url;
	}
	
	public String getDownloadUrl() {
		return mDownloadUrl;
	}
	
	public void setFileSize(int size) {
		mFileSize = size;
	}
	
	public int getFilesize() {
		return mFileSize;
	}
	
	public void setDisplayFileSize(String displaySize) {
		mDisplaySize = displaySize;
	}
	
	public String getDisplayFileSize() {
		return mDisplaySize;
	}
	
	public void setLyricUrl(String url) {
		// Don't set it since we don't use it. This is to save memory.
		// mLyricUrl = url;
	}
	
	public String getLyricUrl() {
		return mLyricUrl;
	}
	
	public void setType(String type) {
		mType = type;
	}
	
	public String getType() {
		return mType;
	}
	
	public static String downloadFilename(MusicInfo info) {
		return info.getTitle() + "[" + info.getArtist() + "].mp3";
	}
}
