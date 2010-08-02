package com.happy.life;

import java.io.File;
import java.util.ArrayList;

import android.text.TextUtils;

public class SogouSearchResult extends SearchResult {

    private String mTitle;
    private String mArtist;
    private String mAlbum;
    private ArrayList<String> mUrls = new ArrayList<String>();
    private String mDownloadUrl;
    private int mUrlIndex = 0;

    private String mDisplaySize;
    private long mFileSize;
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

    public ArrayList<String> getUrls() {
        return mUrls;
    }

    public void setDownloadUrl(String url) {
        mDownloadUrl = url;
    }

    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    public int getUrlIndex() {
        return mUrlIndex;
    }

    public void setUrlIndex(int index) {
        mUrlIndex = index;
    }

    public void setFileSize(long size) {
        mFileSize = size;
    }

    public long getFileSize() {
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


    public static String downloadFilename(SogouSearchResult info) {
        return info.getTitle() + "[" + info.getArtist() + "].mp3";
    }
    
    public static String downloadPath(SogouSearchResult info) {
        return new File(com.limegroup.gnutella.settings.SharingSettings.DEFAULT_SAVE_DIR, downloadFilename(info)).getAbsolutePath();
    }

    @Override
        public String getFileName() {
            return getTitle() + "[" + getArtist() + "].mp3";
        }
}
