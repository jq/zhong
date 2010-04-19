package com.popczar.music.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.popczar.music.Constants;
import com.popczar.music.Utils;


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
	
	private static final int POOL_SIZE = 5;
	private static final int BUFFER_SIZE = 4096;
	
	private static final int MIN_PROGRESS_STEP = 4096;
	private static final long MIN_PROGRESS_TIME = 1500;
	
	private static final String TAG = "DownloadService";
	
	private ArrayList<DownloadInfo> mDownloads = new ArrayList<DownloadInfo>();
	private ArrayList<DownloadObserver> mObservers = new ArrayList<DownloadObserver>();
	private ExecutorService mPool;
	
	private MediaScannerConnection mScanner;
	
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
    
	
	public void insertDownload(DownloadInfo info) {
		if (info == null)
			return;
		
		if (TextUtils.isEmpty(info.getSource()) || 
			TextUtils.isEmpty(info.getTarget())) {
			Log.e(TAG, "Empty source or target");
			return;
		}
		
		synchronized(mDownloads) {
			// Check if the request has already been added.
			for (DownloadInfo d : mDownloads) {
				if (d.getTarget().equals(info.getTarget()))
					return;
			}
			
			mDownloads.add(info);
			// This should not block.
			mPool.execute(new Task(info));
		}
		notifyChanged();
	}
	
	
	public void resumeDownload(DownloadInfo info) {
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
				if (d.getStatus() != DownloadInfo.STATUS_FINISHED) {
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

		private void download() throws IOException {
			URL url;
			synchronized(mInfo) {
				mInfo.setThread(Thread.currentThread());
				url = new URL(mInfo.getSource());
			}
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setRequestProperty("User-Agent", Constants.USER_AGENT);
			
			synchronized(mInfo) {
				// The following code for resuming downloading does not seem to work.
				/*
				if (mInfo.getStatus() == DownloadInfo.STATUS_PENDING) {
					File outFile = new File(mInfo.getTarget());
					if (outFile.exists())
						outFile.delete();
					mInfo.setCurrentBytes(0);
					mInfo.setTotalBytes(connection.getContentLength());
				} else if (mInfo.getStatus() == DownloadInfo.STATUS_STOPPED) {
					if (mInfo.getCurrentBytes() > 0) {
						connection.setRequestProperty("Range", "bytes=" + mInfo.getCurrentBytes() + "-");
					}
				} else {
					throw new IllegalStateException("Invalid download status: " + mInfo.getStatus());
				}
				*/
				
				File outFile = new File(mInfo.getTarget());
				if (outFile.exists())
					outFile.delete();
				mInfo.setCurrentBytes(0);
				mInfo.setTotalBytes(connection.getContentLength());
				
				mInfo.setStatus(DownloadInfo.STATUS_DOWNLOADING);
			}
			notifyChanged();
			
			connection.connect();
			
			RandomAccessFile out = new RandomAccessFile(mInfo.getTarget(), "rw");
			out.seek(mInfo.getCurrentBytes());
			byte[] buffer = new byte[BUFFER_SIZE];

			InputStream input = connection.getInputStream();
			int len;
			
			int bytesUnaccounted = 0;
			long timeLastNotification = System.currentTimeMillis();
			
			while ((len = input.read(buffer)) >= 0) {
				synchronized(mInfo) {
					if (mInfo.getStatus() == DownloadInfo.STATUS_STOPPED ||
						mInfo.getStatus() == DownloadInfo.STATUS_STOPPING) {
						mInfo.setStatus(DownloadInfo.STATUS_STOPPED);
						notifyChanged();
						return;
					}
				}
				out.write(buffer, 0, len);
				bytesUnaccounted += len;
				long now = System.currentTimeMillis();

				if (bytesUnaccounted > MIN_PROGRESS_STEP &&
					now - timeLastNotification > MIN_PROGRESS_TIME) {
					synchronized(mInfo) {
						mInfo.setCurrentBytes(mInfo.getCurrentBytes() + bytesUnaccounted);
					}
					bytesUnaccounted = 0;
					timeLastNotification = now;
					notifyChanged();
				}
			}

			synchronized(mInfo) {
				mInfo.setCurrentBytes(mInfo.getTotalBytes());
				mInfo.setStatus(DownloadInfo.STATUS_FINISHED);
				ScanMediaFile(mInfo.getTarget());
			}
			out.close();
		}


		public Task(DownloadInfo download) {
			mInfo = download;
		}

		@Override
		public void run() {
			
			Utils.D("++++ task: " + mInfo);
			
			
			PowerManager.WakeLock wakeLock = null;
			try {
				PowerManager pm = (PowerManager)DownloadService.this.getSystemService(Context.POWER_SERVICE);
				wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
				wakeLock.acquire();
				download();
			} catch (Exception e) {
				synchronized(mInfo) {
					mInfo.setStatus(DownloadInfo.STATUS_FAILED);
				}
				e.printStackTrace();
				mInfo.setError(e.getMessage());
			} finally {
				if (wakeLock != null) {
					wakeLock.release();
					wakeLock = null;
				}
				notifyChanged();
				Utils.D("task finished: " + mInfo);
			}
		}
	}
	
	private void ScanMediaFile(final String musicPath) {
		mScanner = new MediaScannerConnection(getApplicationContext(),
				new MediaScannerConnectionClient() {
			public void onMediaScannerConnected() {
				mScanner.scanFile(musicPath, "audio/mpeg");
			}

			public void onScanCompleted(String path, Uri uri) {
				if (path.equals(musicPath)) {
					mScanner.disconnect();
				}
			}

		});
		mScanner.connect();
	}
}
