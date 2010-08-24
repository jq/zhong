package com.happy.life;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.text.TextUtils;
import android.util.Log;

import com.limegroup.gnutella.Downloader;
import com.util.DownloadInfo;
import com.util.DownloadService;
import com.util.SearchResult;


public class SogouDownloadInfo extends com.util.DownloadInfo {
    public static final int PENDING = 100;
    
    private static final int BUFFER_SIZE = 4096;
    private static final int MIN_PROGRESS_STEP = 4096;
    private static final long MIN_PROGRESS_TIME = 1500;
    
    public static final String TAG = "SogouDownloadInfo";
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

    @Override
    public void stopDownload() {
      setStatus(Downloader.PAUSED);
      getThread().interrupt();
    }

    @Override
    public void pauseDownload() {
      setStatus(Downloader.PAUSED);
      getThread().interrupt();
    }

    @Override
    public String getFileName() {
      return (new File (getTarget())).getName();
    }

    @Override
    public void deleteDownload() {
      File file = new File(getTarget());
      if (file.exists()) {
        file.delete();
      }
        
      file = new File(getTarget() + ".tmp");
      if (file.exists()) {
        file.delete();
      }
    }

    @Override
    public boolean valid() {
      if (TextUtils.isEmpty(getTarget())) {
        Log.e(TAG, "Empty source or target");
        return false;
      } 
      return true;
    }

    @Override
    public boolean hasFailed() {
      return this.getStatus() == Downloader.GAVE_UP;
    }

    @Override
    public void setFailed(boolean failed) {
      if (failed) {
        setStatus(Downloader.GAVE_UP);
      }
    }

    @Override
    public SearchResult getSearchResult() {
      return mSearchResult;
    }

    public void download(DownloadService mDownloadService) {
      if (getSogouSearchResult().getDownloadUrl() == null) {
          //fetch download url
          SogouMusicSearcher.setMusicDownloadUrl(mDownloadService.getApplication(), getSogouSearchResult());
          if (getSogouSearchResult().getDownloadUrl() == null) {
              Log.e(TAG, "Empty source or target");
              setStatus(Downloader.GAVE_UP);
              return;
          }
      }
      URL url = null;
      synchronized(this) {
          setThread(Thread.currentThread());
          try {
            url = new URL(getSogouSearchResult().getDownloadUrl());
          } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
      }
      HttpURLConnection connection = null;
      try {
        connection = (HttpURLConnection)url.openConnection();
      } catch (IOException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      connection.setRequestProperty("User-Agent", Constants.USER_AGENT);

      RandomAccessFile outFile = null;
      InputStream input = null;
      String tmpFile = getTarget() + ".tmp";
      try {
          outFile = new RandomAccessFile(tmpFile, "rw");
          if (outFile.length() > 0)
              connection.setRequestProperty("Range", "bytes=" + outFile.length() + "-");

          connection.connect();

          if (connection.getResponseCode() < 200 ||
                  connection.getResponseCode() >= 300) {
              synchronized(this) {
                  //mInfo.setStatus(DownloadInfo.STATUS_FAILED);
                  setStatus(Downloader.GAVE_UP);
              }
              setError("Connection error (" +
                      connection.getResponseCode() + "): " + connection.getResponseMessage());
              return;
                  }

          try {
              input = connection.getInputStream();
          } catch (FileNotFoundException e) {
              if (outFile.length() > 0) {
                  // The remote server does not support Range.
                  outFile.close();
                  new File(tmpFile).delete();

                  outFile = new RandomAccessFile(tmpFile, "rw");

                  connection = (HttpURLConnection)url.openConnection();
                  connection.setRequestProperty("User-Agent", Constants.USER_AGENT);
                  connection.connect();
                  input = connection.getInputStream();
              } else {
                  throw e;
              }
          }

          synchronized(this) {
              setCurrentBytes((int)outFile.length());
              setTotalBytes((int)outFile.length() + connection.getContentLength());
              //mInfo.setStatus(DownloadInfo.STATUS_DOWNLOADING);
              setStatus(Downloader.DOWNLOADING);
              mDownloadService.notifyChanged();
          }
          outFile.seek(outFile.length());

          byte[] buffer = new byte[BUFFER_SIZE];
          int len;

          int bytesUnaccounted = 0;
          long timeLastNotification = System.currentTimeMillis();

          while ((len = input.read(buffer)) >= 0) {
              synchronized(this) {
                  //if (mInfo.getStatus() == DownloadInfo.STATUS_STOPPED) {
                  if (getStatus() == Downloader.PAUSED) {
                      return;
                  }
                  }
              outFile.write(buffer, 0, len);
              bytesUnaccounted += len;
              long now = System.currentTimeMillis();

              if (bytesUnaccounted > MIN_PROGRESS_STEP &&
                      now - timeLastNotification > MIN_PROGRESS_TIME) {
                  synchronized(this) {
                      setCurrentBytes(getCurrentBytes() + bytesUnaccounted);
                  }
                  bytesUnaccounted = 0;
                  timeLastNotification = now;
                  mDownloadService.notifyChanged();
                      }
              }

              synchronized(this) {
                  if (getCurrentBytes() < 100) {
                      //mInfo.setStatus(DownloadInfo.STATUS_FAILED);
                      setStatus(Downloader.GAVE_UP);
                      setError("Incomplete file");
                      return;
                  }
                  setCurrentBytes(getTotalBytes());
                  //mInfo.setStatus(DownloadInfo.STATUS_FINISHED);
                  setStatus(Downloader.COMPLETE);
                  File oldFile = new File(tmpFile);
                  if (oldFile.renameTo(new File(getTarget()))) {
                      //mScanner.ScanMediaFile(DownloadService.this, mInfo.getTarget());
                      mDownloadService.ScanMediaFile(getTarget());
                  }
              }
          } catch (Exception e) { 
            e.printStackTrace();
          }
            finally {
              if (outFile != null)
                try {
                  outFile.close();
                } catch (IOException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                }
              if (input != null)
                try {
                  input.close();
                } catch (IOException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                }
          }
    }
    /**
    public void download2(DownloadService mDownloadService) {
      if (getSogouSearchResult().getDownloadUrl() == null) {
          //fetch download url
          SogouMusicSearcher.setMusicDownloadUrl(mDownloadService.getApplication(), getSogouSearchResult());
          if (getSogouSearchResult().getDownloadUrl() == null) {
              Log.e(TAG, "Empty source or target");
              setStatus(Downloader.GAVE_UP);
              return;
          }
      }
      URL url = null;
      synchronized(this) {
        setThread(Thread.currentThread()); 
        url = new URL(getSogouSearchResult().getDownloadUrl()); 
      }
      HttpURLConnection connection = (HttpURLConnection)url.openConnection();
      connection.setRequestProperty("User-Agent", Constants.USER_AGENT);

      RandomAccessFile outFile = null;
      InputStream input = null;
      String tmpFile = getTarget() + ".tmp";
      try {
          outFile = new RandomAccessFile(tmpFile, "rw");
          if (outFile.length() > 0)
              connection.setRequestProperty("Range", "bytes=" + outFile.length() + "-");

          connection.connect();

          if (connection.getResponseCode() < 200 ||
                  connection.getResponseCode() >= 300) {
              synchronized(this) {
                  //mInfo.setStatus(DownloadInfo.STATUS_FAILED);
                  setStatus(Downloader.GAVE_UP);
              }
              setError("Connection error (" +
                      connection.getResponseCode() + "): " + connection.getResponseMessage());
              return;
                  }

          try {
              input = connection.getInputStream();
          } catch (FileNotFoundException e) {
              if (outFile.length() > 0) {
                  // The remote server does not support Range.
                  outFile.close();
                  new File(tmpFile).delete();

                  outFile = new RandomAccessFile(tmpFile, "rw");

                  connection = (HttpURLConnection)url.openConnection();
                  connection.setRequestProperty("User-Agent", Constants.USER_AGENT);
                  connection.connect();
                  input = connection.getInputStream();
              } else {
                  throw e;
              }
          }

          synchronized(this) {
              setCurrentBytes((int)outFile.length());
              setTotalBytes((int)outFile.length() + connection.getContentLength());
              //mInfo.setStatus(DownloadInfo.STATUS_DOWNLOADING);
              setStatus(Downloader.DOWNLOADING);
              mDownloadService.notifyChanged();
          }
          outFile.seek(outFile.length());

          byte[] buffer = new byte[BUFFER_SIZE];
          int len;

          int bytesUnaccounted = 0;
          long timeLastNotification = System.currentTimeMillis();

          while ((len = input.read(buffer)) >= 0) {
              synchronized(this) {
                  //if (mInfo.getStatus() == DownloadInfo.STATUS_STOPPED) {
                  if (getStatus() == Downloader.PAUSED) {
                      return;
                  }
                  }
              outFile.write(buffer, 0, len);
              bytesUnaccounted += len;
              long now = System.currentTimeMillis();

              if (bytesUnaccounted > MIN_PROGRESS_STEP &&
                      now - timeLastNotification > MIN_PROGRESS_TIME) {
                  synchronized(this) {
                      setCurrentBytes(getCurrentBytes() + bytesUnaccounted);
                  }
                  bytesUnaccounted = 0;
                  timeLastNotification = now;
                  mDownloadService.notifyChanged();
                      }
              }

              synchronized(this) {
                  if (getCurrentBytes() < 100) {
                      //mInfo.setStatus(DownloadInfo.STATUS_FAILED);
                      setStatus(Downloader.GAVE_UP);
                      setError("Incomplete file");
                      return;
                  }
                  setCurrentBytes(getTotalBytes());
                  //mInfo.setStatus(DownloadInfo.STATUS_FINISHED);
                  setStatus(Downloader.COMPLETE);
                  File oldFile = new File(tmpFile);
                  if (oldFile.renameTo(new File(getTarget()))) {
                      //mScanner.ScanMediaFile(DownloadService.this, mInfo.getTarget());
                      mDownloadService.ScanMediaFile(getTarget());
                  }
              }
          } finally {
              if (outFile != null)
                  outFile.close();
              if (input != null)
                  input.close();
          }      
    }
    **/
    
    public void resumeDownload() {
      setStatus(DownloadInfo.PENDING);
    }
}
