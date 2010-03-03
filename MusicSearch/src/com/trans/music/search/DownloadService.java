
package com.trans.music.search;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;
import android.widget.RemoteViews;
import android.net.Uri;
import android.util.Log;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;

public class DownloadService extends Service {
    private NotificationManager mNM;
    boolean mDownloading = false;
    private Thread mDownloadThread = null;
    public static String SavedPath = "/sdcard/MusicSearch";

    public long mTotalBytes;
	public long mCurrentBytes;
    private long mLastUpdatedTime = 0;
    String mFullpathname;
    String mTitle;
    
    public class LocalBinder extends Binder {
    	DownloadService getService() {
            return DownloadService.this;
        }
    }
    
    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.  We put an icon in the status bar.
        //DownloadingNotification();
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(R.string.local_service_started);

        // Tell the user we stopped.
        Toast.makeText(this, "Download finished", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /**
     * Show a notification while this service is running.
     */
    private void DownloadingNotification(String title) {
		long curTime = System.currentTimeMillis();
		if(curTime - mLastUpdatedTime>1000)
		{
			mLastUpdatedTime = curTime;
		}
		else
			return;

        Notification n = new Notification();
        n.icon = android.R.drawable.stat_sys_download;
        n.flags |= Notification.FLAG_ONGOING_EVENT;
        
        // Build the RemoteView object
        RemoteViews expandedView = new RemoteViews("com.trans.music.search", R.layout.notify_downloading);
        //expandedView.setTextViewText(R.id.description, "des");
        expandedView.setTextViewText(R.id.title, title);
        expandedView.setProgressBar(R.id.progress_bar,(int)mTotalBytes,(int)mCurrentBytes, mTotalBytes<0);
        expandedView.setTextViewText(R.id.progress_text,getDownloadingText(mTotalBytes, mCurrentBytes));
        expandedView.setImageViewResource(R.id.appIcon,android.R.drawable.stat_sys_download);
        n.contentView = expandedView;
        Intent intent = new Intent();
        intent.setClass(this,  MusicSearch.class);
        n.contentIntent = PendingIntent.getActivity(this, 0,intent, 0);
        
        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.local_service_started, n);
    }

	public void showDownloadOKNotification(String title)
	{
	            
		Notification n = new Notification();
        n.icon = android.R.drawable.stat_sys_download_done;
		n.flags |= Notification.FLAG_AUTO_CANCEL;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + mFullpathname), "audio/mp3");

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,intent, 0);
        n.setLatestEventInfo(
        		this, 
        		title,
        		"Download completed",
        		contentIntent);
        mNM.notify(R.string.local_service_started, n);
	}

	public void showDownloadFailedNotification(String title)
	{
		Notification n = new Notification();
        n.icon = android.R.drawable.stat_notify_error;
        n.flags |= Notification.FLAG_AUTO_CANCEL;

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,new Intent(), 0);
        n.setLatestEventInfo(
        		this, 
        		title,
        		"Download failed",
        		contentIntent);
        mNM.notify(R.string.local_service_started, n);
	}
    
	private String getDownloadingText(long totalBytes, long currentBytes) {
        if (totalBytes <= 0) {
            return "";
        }
        long progress = currentBytes * 100 / totalBytes;
        StringBuilder sb = new StringBuilder();
        sb.append(progress);
        sb.append('%');
        return sb.toString();
    }
    
	public boolean Download(String url, String title) {
        if(mDownloading)
            return false;
        
        final String request = url;
        final String filename = title;
        final String ftitle = title;

        DownloadingNotification(title);
		mDownloadThread = new Thread() {
			public void run() {				
				DownloadFile(filename, request, ftitle);    
			}
		};		
		mDownloadThread.start();
		return true;
        
    }

    

    
	private void DownloadFile(String filename, String videourl, String title) {
		URL url = null;
		HttpURLConnection urlConn = null;

		String urlString;
        Log.e("musicsearch", "DownloadFile : " + videourl);
		urlString = videourl;
		mDownloading = true;
        try {
        	url = new URL(urlString);
        	urlConn = (HttpURLConnection)url.openConnection();
        	urlConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3 -Java");
        	
        	urlConn.connect();

			mTotalBytes = urlConn.getContentLength();
			mCurrentBytes = 0;
			
			DataInputStream fileStream;
			mFullpathname = SavedPath + "/" + filename;
			//FileOutputStream filemp3 =   openFileOutput(filename, MODE_WORLD_READABLE);	
			Log.e("musicsearch", "new file : " + mFullpathname);
			FileOutputStream filemp3 = new FileOutputStream(mFullpathname);
            
			byte[] buff = new byte[64 * 1024];
			int len ;
			fileStream = new DataInputStream(new BufferedInputStream(urlConn.getInputStream()));
			while ((len = fileStream.read(buff)) > 0) {
				filemp3.write(buff, 0, len);
				mCurrentBytes +=  len;

                DownloadingNotification(title);
			}        	

			filemp3.close();


			ScanMediafile(mFullpathname);

            mNM.cancel(R.string.local_service_started);
			showDownloadOKNotification(title);
        } catch (IOException e) {
        	e.printStackTrace();
            mNM.cancel(R.string.local_service_started);
            showDownloadFailedNotification(title);
		}
        mDownloading = false;

	}

    private MediaScannerConnection mScanner;
    private void ScanMediafile(final String fullpathame) {
		
		mScanner = new MediaScannerConnection(this,
			new MediaScannerConnectionClient() {
				public void onMediaScannerConnected() {
					mScanner.scanFile(fullpathame, null /*mimeType*/);
				}

				public void onScanCompleted(String path, Uri uri) {
					if (path.equals(fullpathame)) {
						mScanner.disconnect();
					}
				}
			
		});
		mScanner.connect();

    }   
    
}
