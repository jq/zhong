package com.feebe.musicsearch;

import java.io.File;
import java.util.ArrayList;
import org.json.JSONArray;

import com.feebe.musicsearch.IMediaPlaybackService;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.ImageView;

import android.os.Environment;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.Context;

import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.net.Uri;
import android.content.ContentResolver;
import android.provider.Settings;

import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.content.ContentUris;
import android.database.Cursor;
import android.content.ContentValues;

public class local extends Activity
		implements SeekBar.OnSeekBarChangeListener
{
    // Local Playlist
	ListView mLocalList;
	JSONArray mLocalMp3s = new JSONArray();
    ArrayAdapter<String> mLocalAdapter; 
    ArrayList<String> mLocalStrings = new ArrayList<String>();

    SeekBar  mSeekBar;
    
	private boolean mPaused = false;
	
	int mLocalMp3index = -1;
	
	Uri mCurrentFileUri ;
	private boolean mChooseItem = false;
	
	private IMediaPlaybackService mService = null;
	long  mSongDuration;
	private static final int REFRESH = 1;
	
	ImageView mPlayStop;

	MediaScannerConnection mScanner;
	
	
    @Override
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.local);

		
        // Local Playlist UI
        mLocalList = (ListView) findViewById(R.id.local_playlist);
        mLocalAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mLocalStrings);    
        mLocalList.setAdapter(mLocalAdapter);   

        mSeekBar = (SeekBar)findViewById(R.id.play_seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setEnabled(false);
        SeekBarSetSecondaryProgress(1000);
        
        
        mPlayStop = (ImageView) findViewById(R.id.play_stop);;
        mPlayStop.setOnClickListener(mPlayStopListener);
        mPlayStop.setEnabled(false);
        
        try{
    			String status = Environment.getExternalStorageState();
    			File saveddir = new File(Const.homedir);
    			if(!saveddir.exists())
    				saveddir.mkdirs();
    						
    			File[] file=(new File(Const.homedir)).listFiles();
    			for(int i = 0; i < file.length; i++){
    				if(file[i].isFile()){
    					String fname = file[i].getName();
    					if(fname.endsWith(".mp3"))
    						mLocalAdapter.add(fname);
    				}
    			}
			
        }catch(Exception e) {
    			e.printStackTrace();
    		} 

		startService(new Intent(this, MediaPlaybackService.class));
        bindService((new Intent()).setClass(this,
                MediaPlaybackService.class), osc, 0);
        
        mLocalList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {         
                final int mp3index = position;
				mLocalMp3index = mp3index;
				String fileLocal = Const.homedir + "/" + mLocalStrings.get(mLocalMp3index);

				ScanMediafile(fileLocal);
					
				Toast.makeText(local.this, "Playing:  " + fileLocal, Toast.LENGTH_SHORT).show();
				try {
					mService.stop();
					mService.openfile(fileLocal);
					mService.play();

					mChooseItem = true;
					
					mSeekBar.setEnabled(true);	
					mPlayStop.setEnabled(true);
					mPlayStop.setImageResource(R.drawable.stop);
					
					updateTrackInfo();	  
			        long next = refreshSeekBarNow();
			        queueNextRefresh(next);
			        
				}catch(Exception e) {
					e.printStackTrace();
				} 

            }
        });

    }

	protected void onDestroy() {

		unbindService(osc);		

		//mScanner.disconnect();
		
    	super.onDestroy();
	}
	
    private ServiceConnection osc = new ServiceConnection() {
            public void onServiceConnected(ComponentName classname, IBinder obj) {
                mService = IMediaPlaybackService.Stub.asInterface(obj);
        		
                updateTrackInfo();	  
                long next = refreshSeekBarNow();
                queueNextRefresh(next);
                
            }
			
            public void onServiceDisconnected(ComponentName classname) {
            }
    };    


    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
    	;
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        ;
    }

    public void onStopTrackingTouch(SeekBar seekBar) { 
    	;
    }

    private long refreshSeekBarNow() {
        try {
            long pos = mService.position();
            long remaining = 1000 - (pos % 1000);
            if ((pos >= 0) && (mSongDuration > 0)) {

            	SeekBarSetProgress((int) (1000 * pos / mSongDuration));
            } else {
                SeekBarSetProgress(1000);
            }
            // return the number of milliseconds until the next full second, so
            // the counter can be updated at just the right time
            return remaining;
            //return 5000;
        } catch (Exception ex) {
        }
        return 500;
    }

    private void updateTrackInfo() {
        if (mService == null) {
            return;
        }
        try {
            if (mService.getPath() == null) {
                return;
            }

			if (mPaused == false){
				mPlayStop.setImageResource(R.drawable.stop);
			}else{
				mPlayStop.setImageResource(R.drawable.play);
			}
			   
            mSongDuration = mService.duration();
            mSeekBar.setEnabled(true);
            mPlayStop.setEnabled(true);
       } catch (Exception ex) {
    	   return;
        }
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
    
	private void SeekBarSetProgress(final int progress) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				mSeekBar.setProgress(progress);
			}
		});
	}
	private void SeekBarSetSecondaryProgress(final int progress) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				mSeekBar.setSecondaryProgress(progress);
			}
		});
	}	
	
	private OnClickListener mPlayStopListener = new OnClickListener() {
	    public void onClick(View v) {
		    try{	
			   if (mPaused == false){
				    mService.pause();
				    mPlayStop.setImageResource(R.drawable.play);
		    	    mPaused = true;
			   }else{
				   	mService.play();
				    mPlayStop.setImageResource(R.drawable.stop);
		    	    mPaused = false;
	            	long next = refreshSeekBarNow();
	            	queueNextRefresh(next);
			   }
		    }catch(Exception e) {
				e.printStackTrace();
			} 
	    }
	};


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the currently selected menu XML resource.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.local, menu);      
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_search:
				finish();
            	break;
            case R.id.menu_ringtone:
				if(mLocalMp3index >= 0 && mChooseItem == true){
			        try {
						ContentResolver resolver = this.getContentResolver();
						Uri ringUri = mCurrentFileUri;
						
			            ContentValues values = new ContentValues(2);
			            values.put(MediaStore.Audio.Media.IS_RINGTONE, "1");
			            values.put(MediaStore.Audio.Media.IS_ALARM, "1");
			            resolver.update(ringUri, values, null, null);
						
		                Settings.System.putString(resolver, Settings.System.RINGTONE, ringUri.toString());
						Toast.makeText(this, "This playing song has set as phone ringtone.", Toast.LENGTH_SHORT).show();

			        }catch(Exception e) {
						e.printStackTrace();
					} 
				}else{
					Toast.makeText(this, "Please select one music to play.", Toast.LENGTH_SHORT).show();
				}
            	break;
			case R.id.menu_delete:
            	try{
					String fileLocal = Const.homedir + "/" + mLocalStrings.get(mLocalMp3index);
					File mp3 = new File(fileLocal);
					if(mp3.exists()){
						mp3.delete();
						updateDownloadList();
						mChooseItem = false;
					}
                }catch(Exception e) {
					e.printStackTrace();
				} 
                return true;
            case R.id.menu_help:
                intent = new Intent(local.this, help.class);
                startActivity(intent);
                return true;
            default:
                break;
        }
        
        return false;
    }
	
	private void updateDownloadList() {
		this.runOnUiThread(new Runnable() {
			public void run() {
				try{
					File[] file=(new File(Const.homedir)).listFiles();
					mLocalAdapter.clear();
					for(int i = 0; i < file.length; i++){
						if(file[i].isFile()){
							String fname = file[i].getName();
							if(fname.endsWith(".mp3"))
								mLocalAdapter.add(fname);
						}
					}
				}catch(Exception e) {
					e.printStackTrace();
				} 
			}
		});
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
						mScanner.disconnect();
					}
				}
			
		});
		mScanner.connect();

    }   

}
