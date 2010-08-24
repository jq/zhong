package com.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.util.DownloadInfo;
import com.util.DownloadObserver;
import com.util.P2pDownloadInfo;
import com.limegroup.gnutella.Downloader;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class DownloadService extends Service {

    private static final int POOL_SIZE = 4;


    private static final String TAG = "DownloadService";

    private ArrayList<DownloadInfo> mDownloads = new ArrayList<DownloadInfo>();
    private ArrayList<DownloadObserver> mObservers = new ArrayList<DownloadObserver>();
    private ExecutorService mPool;

    private ArrayList<MediaScannerNotifier> mScanners = new ArrayList<MediaScannerNotifier>();

    public class LocalBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "Download service started");
        mPool = Executors.newFixedThreadPool(POOL_SIZE);
    } 

    public void registerDownloadObserver(DownloadObserver observer) {
        if (observer == null) {
            throw new IllegalArgumentException("The observer is null.");
        }
        synchronized(mObservers) {
            if (mObservers.contains(observer)) {
                throw new IllegalStateException("Observer " + observer + " is already registered.");
            }
            mObservers.add(observer);
        }
    }

    public void unregisterObserver(DownloadObserver observer) {
        if (observer == null) {
            throw new IllegalArgumentException("The observer is null.");
        }
        synchronized(mObservers) {
            int index = mObservers.indexOf(observer);
            if (index == -1) {
                throw new IllegalStateException("Observer " + observer + " was not registered.");
            }
            mObservers.remove(index);
        }
    }

    /**
     * Remove all registered observer
     */
    public void unregisterAll() {
        synchronized(mObservers) {
            mObservers.clear();
        }
    }

    public void notifyChanged() {
        synchronized(mObservers) {
            for (DownloadObserver o : mObservers) {
                o.onChange();
            }
        }
    }


    public ArrayList<DownloadInfo> getDownloadInfos() {
        synchronized(mDownloads) {
            return new ArrayList<DownloadInfo>(mDownloads);
        }
    }

    public boolean fileBeingDownloaded(com.util.SearchResult mp3) {
        if (mp3 == null)
            return true;
        String filename = mp3.getFileName();
        if (filename == null)
            return false;
        synchronized(mDownloads) {
            for (DownloadInfo d : mDownloads) {
                if (d.getSearchResult() == null)
                  continue;
                if (d.getSearchResult().getFileName() != null &&
                      filename.equals(d.getSearchResult().getFileName()))
                  return true;
            }
        }
        return false;
    }

    public void insertDownload(DownloadInfo info) {
        if (info == null || !info.valid())
            return;

        synchronized(mDownloads) {
            for (DownloadInfo d : mDownloads) {
                    if (d.same(info))
                        return;
            }

            mDownloads.add(info);
        }
        if (info.ableToResume()) {
          synchronized(info) {
              info.setFailed(false);
              if (info.isScheduled()) {
                  // No re-entrance.
                  return;
              } else {
                  info.setScheduled(true);
              }
          }
        }
        // This should not block.
        mPool.execute(new Task(info));
        notifyChanged();
    }

    public void retryDownload(DownloadInfo info) {
      if (info == null)
        return;
      if (info.ableToRetry()) {
        mPool.execute(new Task(info));
        notifyChanged();
      }
    }

    public void resumeDownload(DownloadInfo info) {
      if (info == null)
        return;
      info.resumeDownload();
      if (!info.ableToResume())
        retryDownload(info);
    }

    public void removeDownload(DownloadInfo info) {
        if (info == null)
            return;

        synchronized(mDownloads) {
            int index = mDownloads.indexOf(info);
            if (index == -1) {
                return;
            }
            mDownloads.remove(index);
        }
        notifyChanged();
    }


    public void clearFinished() {
        ArrayList<DownloadInfo> downloads = new ArrayList<DownloadInfo>();
        boolean changed = false;
        synchronized(mDownloads) {
            for (DownloadInfo d : mDownloads) {
                if (d.getState() != Downloader.COMPLETE) {
                    downloads.add(d);
                } else {
                    changed = true;
                }
            }
            if (changed) {
                mDownloads = downloads;
            }
        }
        if (changed) {
            notifyChanged();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new LocalBinder();

    private class Task implements Runnable {
        private DownloadInfo mInfo;

        private void download() throws IOException, InterruptedException {
            if (mInfo == null)
                return;
            mInfo.download(DownloadService.this);
        }

        public Task(DownloadInfo download) {
            mInfo = download;
        }

            @Override
            public void run() {
                if (mInfo == null)
                    return;

                PowerManager.WakeLock wakeLock = null;
                try {                    PowerManager pm = (PowerManager)DownloadService.this.getSystemService(Context.POWER_SERVICE);
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
                    wakeLock.acquire();
                    download();
                } catch (Exception e) {
                    e.printStackTrace();
                    synchronized(mInfo) {
                        mInfo.setFailed(true);
                        mInfo.setError(e.getMessage());
                    }
                } finally {
                    if (wakeLock != null) {
                        wakeLock.release();
                        wakeLock = null;
                    }
                    notifyChanged();
                    com.util.Utils.D("task finished: " + mInfo);
                    synchronized(mInfo) {
                        if (mInfo instanceof P2pDownloadInfo)
                            ((P2pDownloadInfo) mInfo).setScheduled(false);
                            notifyChanged();
                    }
                }
            }
        }

        private class MediaScannerNotifier implements MediaScannerConnectionClient {
            private MediaScannerConnection mConnection;
            private String mPath;
            private String mMimeType;

            public MediaScannerNotifier(String path, String mimeType) {
                mPath = path;
                mMimeType = mimeType;
                mConnection = new MediaScannerConnection(DownloadService.this, this);
                mConnection.connect();
            }

            public void onMediaScannerConnected() {
                mConnection.scanFile(mPath, mMimeType);
            }

            public void onScanCompleted(String path, Uri uri) {
                if (mPath == null)
                    return;
                        if (mPath.equals(path)) {
                            com.util.Utils.D("File scanned: " + path);
                                mConnection.disconnect();
                                synchronized(mScanners) {
                                    mScanners.remove(this);
                                }
                        }
            }

        }


        public void ScanMediaFile(final String musicPath) {
            synchronized(mScanners) {
                mScanners.add(new MediaScannerNotifier(musicPath, "audio/mpeg"));
            }
        }
    }
