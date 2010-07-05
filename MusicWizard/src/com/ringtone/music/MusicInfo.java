package com.ringtone.music;

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
	
	// For Debugging.
	public String toString() {
		StringBuffer sb = new StringBuffer("title=" + mTitle);
		sb.append(",artist=" + mArtist);
		sb.append(",album=" + mAlbum);
		sb.append(",url=" + mUrl);
		sb.append(",downloadurl=" + mDownloadUrl);
		sb.append(",displaysize=" + mDisplaySize);
		sb.append(",filesize=" + mFileSize);
		sb.append(",lyricurl=" + mLyricUrl);
		sb.append(",type=" + mType);
		return sb.toString();
	}
	
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
		mLyricUrl = url;
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