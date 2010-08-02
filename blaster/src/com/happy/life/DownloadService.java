package com.happy.life;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.downloader.AlreadyDownloadingException;
import com.limegroup.gnutella.downloader.FileExistsException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

public class DownloadService extends Service {

    private static final int POOL_SIZE = 4;

    private static final long POLL_INTERVAL = 1500;

    private static final int BUFFER_SIZE = 4096;
    private static final int MIN_PROGRESS_STEP = 4096;
    private static final long MIN_PROGRESS_TIME = 1500;

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

    private void notifyChanged() {
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


    //    public boolean fileBeingDownloaded(SearchResult mp3) {
    //    	if (mp3 == null)
    //    		return false;
    //    	
    //    	String filename = mp3.getFileName();
    //    	if (filename == null)
    //    		return false;
    //    	synchronized(mDownloads) {
    //    		for (DownloadInfo d : mDownloads) {
    //    		  if(d instanceof SogouDownloadInfo){
    //    		    if (((SogouDownloadInfo) d).getSogouSearchResult() == null)
    //                  continue;
    //                if (((SogouDownloadInfo) d).getSogouSearchResult().getFileName() != null && 
    //                    filename.equals(((SogouDownloadInfo) d).getSogouSearchResult().getFileName()))
    //                  return true;
    //    		  }
    //    		  if(d instanceof P2pDownloadInfo) {
    //    		    if (((P2pDownloadInfo) d).getP2pSearchResult() == null)
    //    		      continue;
    //    		    if (((P2pDownloadInfo) d).getP2pSearchResult().getFileName() != null && 
    //                    filename.equals(((P2pDownloadInfo) d).getP2pSearchResult().getFileName()))
    //                  return true;
    //    		  }
    //    			
    //    		}
    //    		return false;
    //    	}
    //    }

    public boolean fileBeingDownloaded(SogouSearchResult mp3) {
        if (mp3 == null)
            return true;
        String title = mp3.getTitle();
        String artist = mp3.getArtist();
        String filesize = mp3.getDisplayFileSize();
        synchronized(mDownloads) {
            for (DownloadInfo d : mDownloads) {
                if (d instanceof SogouDownloadInfo) {
                    SogouSearchResult result = ((SogouDownloadInfo) d).getSogouSearchResult();
                    if(result.getTitle().equals(title) &&
                            result.getArtist().equals(artist) &&
                            result.getDisplayFileSize() == filesize)
                        return true;
                }
            }
        }
        return false;
    }

    public boolean fileBeingDownloaded(P2pSearchResult mp3) {
        if (mp3 == null)
            return true;
        String filename = mp3.getFileName();
        if (filename == null)
            return false;
        synchronized(mDownloads) {
            for (DownloadInfo d : mDownloads) {
                if (d instanceof P2pDownloadInfo) {
                    if (((P2pDownloadInfo) d).getP2pSearchResult() == null)
                        continue;
                    if (((P2pDownloadInfo) d).getP2pSearchResult().getFileName() != null && 
                            filename.equals(((P2pDownloadInfo) d).getP2pSearchResult().getFileName()))
                        return true;
                }
            }
        }
        return false;
    }

    public void insertDownload(P2pDownloadInfo info) {
        if (info == null || info.getP2pSearchResult() == null)
            return;

        synchronized(mDownloads) {
            for (DownloadInfo d : mDownloads) {
                if (d instanceof P2pDownloadInfo) {
                    if (((P2pDownloadInfo)d).getP2pSearchResult().getFileName().equals(
                                info.getP2pSearchResult().getFileName()))
                        return;
                }
            }

            info.setFailed(false);
            mDownloads.add(info);
        }

        synchronized(info) {
            if (info.isScheduled()) {
                // No re-entrance.
                return;
            } else {
                info.setScheduled(true);
            }
        }
        // This should not block.
        mPool.execute(new Task(info));
        notifyChanged();
    }

    public void insertDownload(SogouDownloadInfo info) {
        if (info == null)
            return;

        if (TextUtils.isEmpty(info.getTarget())) {
            Log.e(TAG, "Empty source or target");
            return;
        }

        synchronized(mDownloads) {
            // Check if the request has already been added.
            for (DownloadInfo d : mDownloads) {
                if(d instanceof SogouDownloadInfo) {
                    if (( (SogouDownloadInfo)d ).getTarget().equals(info.getTarget()))
                        return;
                }
            }

            mDownloads.add(info);
            // This should not block.
            mPool.execute(new Task(info));
        }
        notifyChanged();
    }


    public void retryDownload(P2pDownloadInfo info) {
        if (info == null)
            return;

        synchronized(info) {
            if (info.getP2pSearchResult() == null)
                return;

            if (info.isScheduled()) {
                // No re-entrance.
                return;
            } else {
                info.setFailed(false);
                info.setScheduled(true);
            }
        }
        // This should not block.
        mPool.execute(new Task(info));
        notifyChanged();
    }

    public void retryDownload(SogouDownloadInfo info) {
        synchronized(info) {
           mPool.execute(new Task(info));
        }
    }

    public void resumeDownload(SogouDownloadInfo info) {
        synchronized(info) {
            mPool.execute(new Task(info));
        }
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

            if (mInfo instanceof P2pDownloadInfo) {
                P2pDownloadInfo p2pInfo = (P2pDownloadInfo)mInfo;
                SearchResult rs;
                synchronized(p2pInfo) {
                    if (p2pInfo.getP2pSearchResult() == null)
                        return;
                    rs = p2pInfo.getP2pSearchResult();
                }

                try {
                    Downloader d = RouterService.download(((P2pSearchResult)rs).getRFDArray(),
                            ((P2pSearchResult)rs).getAlt(), true, new GUID(((P2pSearchResult)rs).getGuid()));

                        Utils.D("Start downloading " + d.getFileName());

                        synchronized(p2pInfo) {
                            if (!p2pInfo.isScheduled()) {
                                // This is a hack. It means user aborts/clears the download.
                                return;
                            }
                            p2pInfo.setTotalBytes(d.getContentLength());
                            p2pInfo.setDownloader(d);
                        }

                    // Polling.
                    int bytesRead = 0;
                    Utils.D("old state: " + d.getState());

                        int state = d.getState();
                    while (isDownloading(state)) {
                        bytesRead = d.getAmountRead();
                        synchronized(p2pInfo) {
                            if (!p2pInfo.isScheduled()) {
                                Utils.D("Aborted");
                                return;  // Abort.
                            }
                            p2pInfo.setCurrentBytes(bytesRead);
                            notifyChanged();
                        }
                        Thread.sleep(POLL_INTERVAL);
                        state = d.getState();
                        Utils.D(d.getFileName() + ":" + state);
                    }
                    Utils.D("new state: " + d.getState());
                    synchronized(p2pInfo) {
                        p2pInfo.setCurrentBytes(p2pInfo.getTotalBytes());
                    }
                    if (state == Downloader.COMPLETE) {
                        Utils.D("Scanning file: " + d.getFile().getAbsolutePath());
                        ScanMediaFile(d.getFile().getAbsolutePath());
                    }
                } catch (FileExistsException e) {
                    synchronized(p2pInfo) {
                        p2pInfo.setFailed(true);
                            p2pInfo.setError(getString(R.string.target_exists));
                    }
                    e.printStackTrace();
                } catch (AlreadyDownloadingException e) {
                    synchronized(p2pInfo) {
                        p2pInfo.setFailed(true);
                            p2pInfo.setError(getString(R.string.already_downloading));
                    }
                    e.printStackTrace();
                }
            }

            if (mInfo instanceof SogouDownloadInfo) {
            	SogouDownloadInfo sogouInfo = (SogouDownloadInfo) mInfo;
                if (sogouInfo.getSogouSearchResult().getDownloadUrl() == null) {
                    //fetch download url
                    SogouMusicSearcher.setMusicDownloadUrl(getApplication(), sogouInfo.getSogouSearchResult());
                    if (sogouInfo.getSogouSearchResult().getDownloadUrl() == null) {
                        Log.e(TAG, "Empty source or target");
                        sogouInfo.setStatus(Downloader.GAVE_UP);
                        return;
                    }
                }
                URL url;
                synchronized(sogouInfo) {
                    sogouInfo.setThread(Thread.currentThread());
                    url = new URL(sogouInfo.getSogouSearchResult().getDownloadUrl());
                }
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                connection.setRequestProperty("User-Agent", Constants.USER_AGENT);

                RandomAccessFile outFile = null;
                InputStream input = null;
                String tmpFile = sogouInfo.getTarget() + ".tmp";
                try {
                    outFile = new RandomAccessFile(tmpFile, "rw");
                    if (outFile.length() > 0)
                        connection.setRequestProperty("Range", "bytes=" + outFile.length() + "-");

                    connection.connect();

                    if (connection.getResponseCode() < 200 ||
                            connection.getResponseCode() >= 300) {
                        synchronized(sogouInfo) {
                            //mInfo.setStatus(DownloadInfo.STATUS_FAILED);
                            sogouInfo.setStatus(Downloader.GAVE_UP);
                        }
                        sogouInfo.setError("Connection error (" +
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

                    synchronized(sogouInfo) {
                        sogouInfo.setCurrentBytes((int)outFile.length());
                        sogouInfo.setTotalBytes((int)outFile.length() + connection.getContentLength());
                        //mInfo.setStatus(DownloadInfo.STATUS_DOWNLOADING);
                        sogouInfo.setStatus(Downloader.DOWNLOADING);
                        notifyChanged();
                    }
                    outFile.seek(outFile.length());

                    byte[] buffer = new byte[BUFFER_SIZE];
                    int len;

                    int bytesUnaccounted = 0;
                    long timeLastNotification = System.currentTimeMillis();

                    while ((len = input.read(buffer)) >= 0) {
                        synchronized(sogouInfo) {
                            //if (mInfo.getStatus() == DownloadInfo.STATUS_STOPPED) {
                            if (sogouInfo.getStatus() == Downloader.PAUSED) {
                                return;
                            }
                            }
                        outFile.write(buffer, 0, len);
                        bytesUnaccounted += len;
                        long now = System.currentTimeMillis();

                        if (bytesUnaccounted > MIN_PROGRESS_STEP &&
                                now - timeLastNotification > MIN_PROGRESS_TIME) {
                            synchronized(sogouInfo) {
                                sogouInfo.setCurrentBytes(sogouInfo.getCurrentBytes() + bytesUnaccounted);
                            }
                            bytesUnaccounted = 0;
                            timeLastNotification = now;
                            notifyChanged();
                                }
                        }

                        synchronized(sogouInfo) {
                            if (sogouInfo.getCurrentBytes() < 100) {
                                //mInfo.setStatus(DownloadInfo.STATUS_FAILED);
                                sogouInfo.setStatus(Downloader.GAVE_UP);
                                sogouInfo.setError("Incomplete file");
                                return;
                            }
                            sogouInfo.setCurrentBytes(sogouInfo.getTotalBytes());
                            //mInfo.setStatus(DownloadInfo.STATUS_FINISHED);
                            sogouInfo.setStatus(Downloader.COMPLETE);
                            File oldFile = new File(tmpFile);
                            if (oldFile.renameTo(new File(sogouInfo.getTarget()))) {
                                //mScanner.ScanMediaFile(DownloadService.this, mInfo.getTarget());
                                ScanMediaFile(sogouInfo.getTarget());
                            }
                        }
                    } finally {
                        if (outFile != null)
                            outFile.close();
                        if (input != null)
                            input.close();
                    }
                }
            }

            public Task(DownloadInfo download) {
                mInfo = download;
            }

            @Override
            public void run() {
                if (mInfo == null)
                    return;

                PowerManager.WakeLock wakeLock = null;
                try {
                    PowerManager pm = (PowerManager)DownloadService.this.getSystemService(Context.POWER_SERVICE);
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
                    wakeLock.acquire();
                    download();
                } catch (Exception e) {
                    e.printStackTrace();
                    synchronized(mInfo) {
                        if(mInfo instanceof P2pDownloadInfo) {
                            ((P2pDownloadInfo) mInfo).setFailed(true);	
                        }
                        if(mInfo instanceof SogouDownloadInfo) {
                            //mInfo.setStatus(DownloadInfo.STATUS_FAILED);
                            ((SogouDownloadInfo) mInfo).setStatus(Downloader.GAVE_UP);
                        }
                        mInfo.setError(e.getMessage());
                    }
                } finally {
                    if (wakeLock != null) {
                        wakeLock.release();
                        wakeLock = null;
                    }
                    notifyChanged();
                    Utils.D("task finished: " + mInfo);
                    synchronized(mInfo) {
                        if(mInfo instanceof P2pDownloadInfo)
                            ((P2pDownloadInfo) mInfo).setScheduled(false);
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
                            Utils.D("File scanned: " + path);
                                mConnection.disconnect();
                                synchronized(mScanners) {
                                    mScanners.remove(this);
                                }
                        }
            }

        }


        private void ScanMediaFile(final String musicPath) {
            synchronized(mScanners) {
                mScanners.add(new MediaScannerNotifier(musicPath, "audio/mpeg"));
            }
        }
    }
