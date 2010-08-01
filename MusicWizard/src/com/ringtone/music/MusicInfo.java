package com.ringtone.music;

import java.util.ArrayList;

import android.text.TextUtils;

public class MusicInfo {

	private String mTitle;
	private String mArtist;
	private String mAlbum;
	private ArrayList<String> mUrls = new ArrayList<String>();
	private ArrayList<String> mDownloadUrls = new ArrayList<String>();
	
	private String mDisplaySize;
	private int mFileSize;
	private String mLyricUrl;
	private String mType;
	
	public void setTitle(String title) {
		mTitle = title;
	}
	
	public String getTitle() {
		if (TextUtils.isEmpty(mTitle)) {
			return "Unknown";
		}
		return mTitle;
	}
	
	public void setArtist(String artist) {
		mArtist = artist;
	}
	
	public String getArtist() {
		if (TextUtils.isEmpty(mArtist)) {
			return "Unknown";
		}
		return mArtist;
	}
	
	public void setAlbum(String album) {
		mAlbum = album;
	}
	
	public String getAlbum() {
		if (TextUtils.isEmpty(mAlbum))
			return "Unknown";
		return mAlbum;
	}
	
	public void addUrl(String url) {
	    mUrls.add(url);
	}
	
	public String getUrl() {
		if (mUrls == null || mUrls.size() == 0)
			return null;
		return mUrls.get(0);
	}
	
	public void addDownloadUrl(String url) {
	    mDownloadUrls.add(url);
	}
	
	public String getDownloadUrl() {
		if (mDownloadUrls == null || mDownloadUrls.size() == 0)
			return null;
		return mDownloadUrls.get(0);
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
		if (mDisplaySize == null)
			return "";
		return mDisplaySize;
	}
	
	public void setLyricUrl(String url) {
        // To save memory, we don't store lyrics url for now. Change it when we need it.
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
		return (info.getTitle() + "[" + info.getArtist() + "].mp3").replaceAll("/", "").replaceAll("\\\\", "");
	}
	
	public static String downloadPath(MusicInfo info) {
		return App.getMp3Path() + "/" + downloadFilename(info);
	}
}
