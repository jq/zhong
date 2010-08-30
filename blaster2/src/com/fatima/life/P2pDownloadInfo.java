package com.fatima.life;

import com.limegroup.gnutella.Downloader;

public class P2pDownloadInfo extends DownloadInfo {
    public static final int PENDING = 100;
    
    private P2pSearchResult mSearchResult;
    private Downloader mDownloader;
    private String mFileName;
    private boolean mScheduled;
    private boolean mFailed;
    
    public P2pDownloadInfo(P2pSearchResult rs) {
        mSearchResult = rs;
    }
    
    public Downloader getDownloader() {
        return mDownloader;
    }
    
    public void setDownloader(Downloader d) {
        mDownloader = d;
    }
    
    @Override
    public int getState() {
        if (mDownloader == null)
            return PENDING;
        return mDownloader.getState();
    }
    
    public String getFileName() {
        return mFileName;
    }
    
    public void setFileName(String name) {
        mFileName = name;
    }
    
    public P2pSearchResult getP2pSearchResult() {
        return mSearchResult;
    }
    
    public void setScheduled(boolean scheduled) {
        mScheduled = scheduled;
    }
    
    public boolean isScheduled() {
        return mScheduled;
    }
    
    public void setFailed(boolean failed) {
        mFailed = failed;
    }
    
    public boolean hasFailed() {
        return mFailed;
    }
}
