package com.feebe.musicsearch;


import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Browser;

import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.SeekBar;
import android.widget.Toast;
import android.view.View;
import android.view.Window;
import android.view.KeyEvent;
import android.view.View.OnClickListener;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.util.Log;
import android.net.Uri;
import android.graphics.Bitmap;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.media.RingtoneManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;

/**
 * 歌手名列表，被Feature和Category调用
 */
public class RDetail extends Activity
	implements SeekBar.OnSeekBarChangeListener,
	MediaPlayer.OnCompletionListener,
	View.OnClickListener
    {

    private static final int IO_BUFFER_SIZE = 4 * 1024;
    private WebView mWebView = null;
    String  mRequstUrl = null;
    boolean mShouldShow = true;



	private View mSetButton;
    private View mPreviewButton;
    private View mShareButton;

    SeekBar  mSeekBar;
    MediaPlayer mMediaPlayer;

    long  mSongDuration;
    String mRingtoneLink = null;
    String mRingtoneRealLink = null;
    private String mMp3title;
    private String SavedPath = "/sdcard/Ringtones";
    Uri mCurrentFileUri = null ;
    private boolean mDownloading = false;
    private boolean mPaused = false;
    private static final int REFRESH = 1;

    MediaScannerConnection mScanner;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        setContentView(R.layout.detail);
        
 		Bundle extras = getIntent().getExtras();

        if(extras != null){
    		mRequstUrl = extras.getString("url");
            mRingtoneLink = mRequstUrl.replaceAll("/show/", "/getlink/");
            Log.e("Ringtone", "Ringtonelink : " + mRingtoneLink);
        }
        
        mWebView = (WebView) findViewById(R.id.webview);
		mWebView.setWebViewClient(new SimpleWebViewClient(this));		
		WebSettings webSettings = mWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);       
        
        if(mRequstUrl != null && mRequstUrl.length() > 0){
            Log.e("Ringtone", "Load page : " + mRequstUrl);
            mWebView.loadUrl(mRequstUrl + "?nh=1");
        }else{
            mWebView.loadUrl("http://ggapp.appspot.com/mobile/home/");
        }


        mSeekBar = (SeekBar)findViewById(R.id.play_seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setMax(1000);
        mSeekBar.setEnabled(false);

        
        mSetButton = findViewById(R.id.setButton);
        mSetButton.setOnClickListener(this);
        mPreviewButton = findViewById(R.id.previewButton); 
        mPreviewButton.setOnClickListener(this);
        mShareButton = findViewById(R.id.shareButton);
        mShareButton.setOnClickListener(this);


        try{
			String status = Environment.getExternalStorageState();
			if(!status.equals(Environment.MEDIA_REMOVED)){
				
				File saveddir = new File(SavedPath);
				if(!saveddir.exists())
					saveddir.mkdirs();
							
			}
			
        }catch(Exception e) {
			e.printStackTrace();
		} 			
        
    }
    public void onDestroy(){
        if(mWebView != null){
            Log.e("Ringtone ", "Clear webview : " );
    		mWebView.clearCache(true);	
    		mWebView.destroy();
    		mWebView = null;
        }
        
        try {
			if(mMediaPlayer != null)
				mMediaPlayer.stop();
	            mMediaPlayer.release();
	            mMediaPlayer = null;
        } catch (Exception ex) {
        	;
        }
		        
    	super.onDestroy();
    }

    private class SimpleWebViewClient extends WebViewClient {
		private RDetail mActivity;
		SimpleWebViewClient(RDetail activity){
			mActivity = activity;
		}
        
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    	Log.e("Ringtones :search ", "shouldOverrideUrlLoading: " + url);
	        //view.loadUrl(url);
	        //super.shouldOverrideUrlLoading();
	        
            if(url.indexOf("http://ggapp.appspot.com/mobile/show/") != -1){
                
            }else if(url.indexOf("http://ggapp.appspot.com/mobile/") != -1){

                Intent intent = new Intent();
 				intent.putExtra("url", url);
            	intent.setClass(RDetail.this, ArtistList.class);
				startActivity(intent);	
            }else{
    			Intent i = new Intent(Intent.ACTION_VIEW);
    			i.setData(Uri.parse(url));
    			startActivity(i);

            }
			
	        return true;
	    }
        @Override
		public void  onPageFinished(WebView view, String url){
            if(mShouldShow == true){
                findViewById(R.id.center_text).setVisibility(View.GONE);
                mWebView.setVisibility(View.VISIBLE);
            }
            mActivity.getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,Window.PROGRESS_VISIBILITY_OFF );

		}

        @Override
		public void  onPageStarted(WebView view, String url, Bitmap favicon){
			mActivity.getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,Window.PROGRESS_VISIBILITY_ON );

		}

		public void onLoadResource (WebView view, String url){
			super.onLoadResource(view, url);
		}

        
        @Override
        public void  onReceivedError  (WebView view, int errorCode, String description, String failingUrl){
            if(errorCode != 200)
                mShouldShow = false;
                
        }
        
	}
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
			Bundle extras = data.getExtras();
			Uri uri = data != null ? data.getData() : null;
            setResult(RESULT_OK, new Intent().setData(uri));
            finish();            
        } 
    }

	private void goBackOnePageOrQuit() {
        WebView webView = mWebView;
		if (webView.canGoBack()) {
			webView.goBack();
		} else {
			finish();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		boolean handle = (keyCode == KeyEvent.KEYCODE_BACK);
		return handle || super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			goBackOnePageOrQuit();
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
		
	}


    public void onCompletion(MediaPlayer mp) {
        if (mMediaPlayer == mp) {
            mp.stop();
            mp.release();
            mMediaPlayer = null;

        }
    }

	
    void stopMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;

        }
    }

	private void SeekBarSetProgress(final int progress) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				mSeekBar.setProgress(progress);
			}
		});
	}
    
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
    	;
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        ;
    }

    public void onStopTrackingTouch(SeekBar seekBar) { 
 		;
    }

    private void queueNextRefresh(long delay) {
        if (!mPaused) {
            Message msg = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            mHandler.sendMessageDelayed(msg, delay);
        }
    }   

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH:
                    long next = refreshSeekBarNow();
                    queueNextRefresh(next);
                    break;                   

                default:
                    break;
            }
        }
    };

    private long refreshSeekBarNow() {
        try {
            long pos = mMediaPlayer.getCurrentPosition();
            long remaining = 1000 - (pos % 1000);
            if ((pos >= 0) && (mSongDuration > 0)) {

            	SeekBarSetProgress((int) (1000 * pos / mSongDuration));
            } else {
                SeekBarSetProgress(1000);
            }
            // return the number of milliseconds until the next full second, so
            // the counter can be updated at just the right time
            return remaining;
        } catch (Exception ex) {
        	;
        }
        return 500;
    }


	private static final int DOWNLOAD_MP3FILE = 1;
	private static final int CONNECTING = 2;	     
    ProgressDialog   mProgressDialog, mProgressDownload;
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {

			// download progress
            case DOWNLOAD_MP3FILE: {
                mProgressDownload = new ProgressDialog(this);
                mProgressDownload.setMessage("Download mp3 file ...");
				mProgressDownload.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				mProgressDownload.setMax(1000);
				mProgressDownload.setButton("Cancel", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
						mProgressDownload.hide();
	                    /* User clicked Yes so do some stuff */
						
	                }
	            });				
                return mProgressDownload;
			}
            case CONNECTING: {
				mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setMessage("Please wait while connecting...");
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(true);
                return mProgressDialog;
            }
        }
        return null;
    }

	private void SeekBarSetEnalbe(final boolean enable) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				mSeekBar.setEnabled(enable);
			}
		});
	}

	private void DownloadSetProgress(final int progress) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				mProgressDownload.setProgress(progress);
			}
		});
	}	

	private void DownloadSetMax(final int progress) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				mProgressDownload.setMax(progress);
			}
		});
	}    
	private void DownloadMusic(String filename, String mp3url) {
		URL url = null;
		HttpURLConnection urlConn = null;

		String urlString;
        Log.v("Ringtone", "download: " + filename +" from " + mp3url);
		urlString = mp3url;
		DownloadSetProgress(0);
		mDownloading = true;
        try {
        	url = new URL(urlString);
        	urlConn = (HttpURLConnection)url.openConnection();
        	urlConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3 -Java");
        	
        	urlConn.connect();

			int downsize = urlConn.getContentLength();
			int downed = 0;
			
			DownloadSetMax(downsize);
			
			DataInputStream fileStream;
			String fullpathname = SavedPath + "/" + filename;
			//FileOutputStream filemp3 =   openFileOutput(filename, MODE_WORLD_READABLE);						
			FileOutputStream filemp3 = new FileOutputStream(fullpathname);

			byte[] buff = new byte[4096];
			int len ;
			fileStream = new DataInputStream(new BufferedInputStream(urlConn.getInputStream()));
			while ((len = fileStream.read(buff, 0, 4096)) > 0) {
				filemp3.write(buff, 0, len);
				downed +=  len;
				DownloadSetProgress(downed);
			}        	

			filemp3.close();

			//DownloadShowMessage(filename + " download finished");
			//updateDownloadList();

			//ScanMediafile(fullpathname);

				
        } catch (IOException e) {
        	e.printStackTrace();
		}
		//if (mProgressDialogIsOpen == true) {
			mProgressDownload.dismiss();
			mDownloading = false;

		//}
		//toggleProgressBarVisibility(false);
	}


    private void ScanMediafile(final String fullpathame) {
		
		mScanner = new MediaScannerConnection(this,
			new MediaScannerConnectionClient() {
				public void onMediaScannerConnected() {
					mScanner.scanFile(fullpathame, null /*mimeType*/);
				}

				public void onScanCompleted(String path, Uri uri) {
					if (path.equals(fullpathame)) {
						mCurrentFileUri = uri;	

						FinishedReturn();
			
						mScanner.disconnect();
					}
				}
			
		});
		mScanner.connect();

    }  


	private void ShowSetFinish() {
		this.runOnUiThread(new Runnable() {
			public void run() {
    			Toast.makeText(RDetail.this, "This ringtone has been set finish.", Toast.LENGTH_LONG).show();
			}
		});
	}	
    
	private void FinishedReturn() {
		Log.v("MusicSearch", "FinishedReturn: ");
        try {
			ContentResolver resolver = this.getContentResolver();
			Uri ringUri = mCurrentFileUri;
			
            ContentValues values = new ContentValues(2);
            values.put(MediaStore.Audio.Media.IS_RINGTONE, "1");
            values.put(MediaStore.Audio.Media.IS_ALARM, "1");
            resolver.update(ringUri, values, null, null);
			
            Settings.System.putString(resolver, Settings.System.RINGTONE, ringUri.toString());
            ShowSetFinish();
        }catch(Exception e) {
			e.printStackTrace();
		} 	
	}

    
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.setButton:   
                if(mRingtoneRealLink == null)
                    mRingtoneRealLink  = NetLoadString(mRingtoneLink);
                
				mMp3title = mRingtoneLink.replaceAll("http://ggapp.appspot.com/mobile/getlink/", "");
				showDialog(DOWNLOAD_MP3FILE); 

				(new Thread() {
					public void run() {
						try {
							stopMediaPlayer();
						}catch(Exception e) {
							e.printStackTrace();
						} 
						String filesuffix = ".mp3";
						String filename = mMp3title + filesuffix;
						

						
						filename = mMp3title + filesuffix;
						
						String fullpathname = SavedPath + "/" + filename;
												
						DownloadMusic(filename, mRingtoneRealLink);  
                        ScanMediafile(fullpathname);

                        //Toast.makeText(MusicSearch.this, "Ringtone has saved to filename.", Toast.LENGTH_LONG).show();
					}
				}).start();

                break;
                
            case R.id.previewButton:

                showDialog(CONNECTING); 		
                
                (new Thread() {
        			public void run() {				
						try {
                            mRingtoneRealLink  = NetLoadString(mRingtoneLink);
							Log.v("MusicSearch", "link: " + mRingtoneRealLink); 
							
							//mMediaPlayer.reset();
							stopMediaPlayer();
				            mMediaPlayer = new MediaPlayer();
				
							mMediaPlayer.setDataSource(mRingtoneRealLink);
							mMediaPlayer.setOnCompletionListener(RDetail.this);
							//mMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
							mMediaPlayer.prepare();
							mMediaPlayer.start();

							mProgressDialog.dismiss();
			
							SeekBarSetEnalbe(true);	

							
							mSongDuration = mMediaPlayer.getDuration(); 
					        long next = refreshSeekBarNow();
					        queueNextRefresh(next);
					        
						}catch(Exception e) {
							e.printStackTrace();
						} 
	    			}
        		}).start(); 

                break;

            case R.id.shareButton:
                Browser.sendString(this, mRequstUrl);
                break;
        }
    }   




    /**
     * Closes the specified stream.
     * 
     * @param stream The stream to close.
     */
    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                android.util.Log.e("Ringtone", "Could not close stream", e);
            }
        }
    }

    /**
     * Copy the content of the input stream into the output stream, using a
     * temporary byte array buffer whose size is defined by
     * {@link #IO_BUFFER_SIZE}.
     * 
     * @param in The input stream to copy from.
     * @param out The output stream to copy to.
     * @throws IOException If any error occurs during the copy.
     */
    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] b = new byte[IO_BUFFER_SIZE];
        int read;
        while ((read = in.read(b)) != -1) {
            out.write(b, 0, read);
        }
    }
    public String NetLoadString(String url) {
        String str = null;
        InputStream in = null;
        BufferedOutputStream out = null;

        try {
			//Log.e(TAG, "load Bitmap url: " + url);
            in = new BufferedInputStream(new URL(url).openStream(), IO_BUFFER_SIZE);

            final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
            out = new BufferedOutputStream(dataStream, IO_BUFFER_SIZE);
            copy(in, out);
            out.flush();

            final byte[] data = dataStream.toByteArray();
            str = new String(data);
			//Log.e(TAG, "load String : " + str);
        } catch (IOException e) {
            Log.e("Ringtone", "Could not load Bitmap from: " + url);
        } finally {
            closeStream(in);
            closeStream(out);
        }

        return str;
    }

}

