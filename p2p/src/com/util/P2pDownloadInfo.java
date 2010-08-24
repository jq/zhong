package com.util;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.Context;

import com.limegroup.gnutella.R;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.downloader.AlreadyDownloadingException;
import com.limegroup.gnutella.downloader.FileExistsException;
import com.util.DownloadInfo;

public class P2pDownloadInfo extends com.util.DownloadInfo {
    public static final int PENDING = 100;
    
    private static final long POLL_INTERVAL = 1500;
    
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
    
    @Override
    public boolean showToastForLongPress() {
      int state = getState();
      if(state != Downloader.QUEUED && !(state == DownloadInfo.PENDING && hasFailed()))
        return true;
      return false;
    }
    
    public String getFileName() {
        return mSearchResult.getFileName();
    }
    
    //public void setFileName(String name) {
    //    mFileName = name;
    //}
    
    public P2pSearchResult getP2pSearchResult() {
        return mSearchResult;
    }
    public boolean valid() {
      return mSearchResult != null;
    }

    public void setScheduled(boolean scheduled) {
        mScheduled = scheduled;
    }
    
    public boolean isScheduled() {
        return mScheduled;
    }
    
    public boolean pendingFailed() {
      if(getState( )== DownloadInfo.PENDING && hasFailed())
        return true;
      return false;
    }
    
    public void setFailed(boolean failed) {
        mFailed = failed;
    }
    
    public boolean hasFailed() {
        return mFailed;
    }

    @Override
    public String getTarget() {
      String target = "";
      try {
        target = getDownloader().getFile().getAbsolutePath();
      } catch (Exception e) {
      }
      return target;
    }

    @Override
    public void stopDownload() {
      getDownloader().stop();
      setScheduled(false);
    }

    @Override
    public void pauseDownload() {
      getDownloader().pause();
    }

    @Override
    public void deleteDownload() {
      setScheduled(false);
      if (mDownloader != null) {
        mDownloader.stop();
        File file = mDownloader.getFile();
        if (file != null && file.exists()) {
            file.delete();
        }
      }
    }
    public void download(DownloadService mDownloadService) {
      SearchResult rs;
      synchronized(this) {
          if (getP2pSearchResult() == null)
              return;
          rs = getP2pSearchResult();
      }

      try {
          Downloader d = null;
          try {
            d = RouterService.download(((P2pSearchResult)rs).getRFDArray(),
                    ((P2pSearchResult)rs).getAlt(), true, new GUID(((P2pSearchResult)rs).getGuid()));
          } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
          }

          com.util.Utils.D("Start downloading " + d.getFileName());

          synchronized (this) {
              if (!isScheduled()) {
                  // This is a hack. It means user aborts/clears the download.
                  return;
              }
              setTotalBytes(d.getContentLength());
              setDownloader(d);
          }

          // Polling.
          int bytesRead = 0;
          com.util.Utils.D("old state: " + d.getState());

          int state = d.getState();
          while (DownloadInfo.isDownloading(state)) {
              bytesRead = d.getAmountRead();
              synchronized (this) {
                  if (!isScheduled()) {
                      com.util.Utils.D("Aborted");
                      return;  // Abort.
                  }
                  setCurrentBytes(bytesRead);
                  mDownloadService.notifyChanged();
              }
              try {
                Thread.sleep(POLL_INTERVAL);
              } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
              state = d.getState();
              com.util.Utils.D(d.getFileName() + ":" + state);
          }
          com.util.Utils.D("new state: " + d.getState());
          synchronized(this) {
              setCurrentBytes(getTotalBytes());
          }
          if (state == Downloader.COMPLETE) {
              com.util.Utils.D("Scanning file: " + d.getFile().getAbsolutePath());
              mDownloadService.ScanMediaFile(d.getFile().getAbsolutePath());
          }
      } catch (FileExistsException e) {
          synchronized(this) {
              setFailed(true);
              setError(mDownloadService.getString(R.string.target_exists));
          }
          e.printStackTrace();
      } catch (AlreadyDownloadingException e) {
          synchronized(this) {
              setFailed(true);
              setError(mDownloadService.getString(R.string.already_downloading));
          }
          e.printStackTrace();
      } 
    }
    
    public void resumeDownload() {
      try {
        getDownloader().resume();
      } catch (AlreadyDownloadingException e) {
        e.printStackTrace();
      }
    }
    
    @Override
    public SearchResult getSearchResult() {
      return mSearchResult;
    }
    
    public boolean ableToResume() {return true;}
    
    public boolean ableToRetry() {
      File file = getDownloader().getFile();
      if (file.exists()) {
          file.delete();
      }
      synchronized(this) {
        if (getP2pSearchResult() == null)
            return false;

        if (isScheduled()) {
            // No re-entrance.
            return false;
        } else {
            setFailed(false);
            setScheduled(true);
        }
      }
      return true;
    }
}
