package com.ringtone.music.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.ringtone.music.Constants;
import com.ringtone.music.MediaScannerHelper;
import com.ringtone.music.Utils;


import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

public class DownloadService extends Service {
	
	private static final int POOL_SIZE = 4;
	private static final int BUFFER_SIZE = 4096;
	
	private static final int MIN_PROGRESS_STEP = 4096;
	private static final long MIN_PROGRESS_TIME = 1500;
	
	private static final String TAG = "DownloadService";
	
	private ArrayList<DownloadInfo> mDownloads = new ArrayList<DownloadInfo>();
	private ArrayList<DownloadObserver> mObservers = new ArrayList<DownloadObserver>();
	private ExecutorService mPool;
	
	private MediaScannerHelper mScanner = new MediaScannerHelper();
	
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
        synchronized (mObservers) {
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
        synchronized (mObservers) {
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
		
		synchronized (mDownloads) {
			// Check if the request has already been added.
			// But if the previous download failed. We replace the old one with the new one.
			DownloadInfo failed = null;
			for (DownloadInfo d : mDownloads) {
				if (d.getTarget().equals(info.getTarget())) {
					if (d.getStatus() == DownloadInfo.STATUS_FAILED) {
						failed = d;
						break;
					} else {
						return;
					}
				}
			}
			
			if (failed != null) {
	            int index = mDownloads.indexOf(failed);
	            if (index != -1) {
		            mDownloads.remove(index);
	            } else {
	            	// What happened?
	            }
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
        
        synchronized (mDownloads) {
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
		synchronized (mDownloads) {
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
	
	@Override
	public boolean onUnbind(Intent intent) {
        if (mDownloads.size() > 0) {
        	return false;
        }
        
        stopSelf();
        return false;
	}
	
	private final IBinder mBinder = new LocalBinder();
	
	private class Task implements Runnable {
		private DownloadInfo mInfo;

		private void download() throws IOException {
			if (mInfo == null)
				return;
			
			URL url;
			synchronized (mInfo) {
				mInfo.setThread(Thread.currentThread());
				url = new URL(mInfo.getSource());
			}
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setRequestProperty("User-Agent", Constants.USER_AGENT);
			
			RandomAccessFile outFile = null;
			InputStream input = null;
			String tmpFile = mInfo.getTarget() + ".tmp";
			try {
				outFile = new RandomAccessFile(tmpFile, "rw");
				if (outFile.length() > 0)
					connection.setRequestProperty("Range", "bytes=" + outFile.length() + "-");

				connection.connect();
				
				if (connection.getResponseCode() < 200 ||
					connection.getResponseCode() >= 300) {
					synchronized (mInfo) {
						mInfo.setStatus(DownloadInfo.STATUS_FAILED);
					}
					mInfo.setError("Connection error (" +
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

				synchronized(mInfo) {
					mInfo.setCurrentBytes((int)outFile.length());
					mInfo.setTotalBytes((int)outFile.length() + connection.getContentLength());
					mInfo.setStatus(DownloadInfo.STATUS_DOWNLOADING);
					notifyChanged();
				}
				outFile.seek(outFile.length());

				byte[] buffer = new byte[BUFFER_SIZE];
				int len;

				int bytesUnaccounted = 0;
				long timeLastNotification = System.currentTimeMillis();

				while ((len = input.read(buffer)) >= 0) {
					synchronized(mInfo) {
						if (mInfo.getStatus() == DownloadInfo.STATUS_STOPPED) {
							return;
						}
					}
					outFile.write(buffer, 0, len);
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
					if (mInfo.getCurrentBytes() < 100) {
						mInfo.setStatus(DownloadInfo.STATUS_FAILED);
						mInfo.setError("Incomplete file");
						return;
					}
					mInfo.setCurrentBytes(mInfo.getTotalBytes());
					mInfo.setStatus(DownloadInfo.STATUS_FINISHED);
					File oldFile = new File(tmpFile);
					if (oldFile.renameTo(new File(mInfo.getTarget()))) {
						mScanner.ScanMediaFile(DownloadService.this, mInfo.getTarget());
					}
				}
			} finally {
				if (outFile != null)
					outFile.close();
				if (input != null)
					input.close();
			}
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
	
	// Some helper routines.
    public static DownloadService sService = null;
    private static HashMap<Context, ServiceBinder> sConnectionMap = new HashMap<Context, ServiceBinder>();

    public static class ServiceToken {
        ContextWrapper mWrappedContext;
        ServiceToken(ContextWrapper context) {
            mWrappedContext = context;
        }
    }

    public static ServiceToken bindToService(Activity context) {
        return bindToService(context, null);
    }

    public static ServiceToken bindToService(Activity context, ServiceConnection callback) {
        Activity realActivity = context.getParent();
        if (realActivity == null) {
            realActivity = context;
        }
        ContextWrapper cw = new ContextWrapper(realActivity);
        cw.startService(new Intent(cw, DownloadService.class));
        ServiceBinder sb = new ServiceBinder(callback);
        if (cw.bindService((new Intent()).setClass(cw, DownloadService.class), sb, 0)) {
            sConnectionMap.put(cw, sb);
            return new ServiceToken(cw);
        }
        Log.e("Music", "Failed to bind to service");
        return null;
    }

    public static void unbindFromService(ServiceToken token) {
        if (token == null) {
            Log.e("MusicUtils", "Trying to unbind with null token");
            return;
        }
        ContextWrapper cw = token.mWrappedContext;
        ServiceBinder sb = sConnectionMap.remove(cw);
        if (sb == null) {
            Log.e("MusicUtils", "Trying to unbind for unknown Context");
            return;
        }
        cw.unbindService(sb);
        if (sConnectionMap.isEmpty()) {
            // presumably there is nobody interested in the service at this point,
            // so don't hang on to the ServiceConnection
            sService = null;
        }
    }

    private static class ServiceBinder implements ServiceConnection {
        ServiceConnection mCallback;
        ServiceBinder(ServiceConnection callback) {
            mCallback = callback;
        }
        
        public void onServiceConnected(ComponentName className, android.os.IBinder service) {
			sService = ((DownloadService.LocalBinder) service).getService();
            if (mCallback != null) {
                mCallback.onServiceConnected(className, service);
            }
        }
        
        public void onServiceDisconnected(ComponentName className) {
            if (mCallback != null) {
                mCallback.onServiceDisconnected(className);
            }
            sService = null;
        }
    }
}
