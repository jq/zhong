package com.fatima.life2;

import com.droidcool.music.TrackBrowserActivity;
import com.fatima.life2.updater.AppUpdater;
import com.fatima.life2.updater.UpdateInfo;
import com.fatima.life2.R;
import com.limegroup.gnutella.RouterService;
import com.other.RingSelectActivity;
import com.ringdroid.RingdroidSelectActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class music extends Activity {
	@SuppressWarnings("unused")
	private SearchBar mSearch;
	
	private static final int DIALOG_INITIALIZING = 1;
	private static final int DIALOG_NETWORK_ERROR = 2;
	
	private ProgressDialog mProgressDialog;
	
	private Handler mHandler = new Handler();
	
	private static boolean sInitialized = false;
	private static Activity sActivity;
	private static boolean sFeedsAndUpdateChecked = false;
	
    @Override
    protected Dialog onCreateDialog(int id) {
        Utils.D("onCreateDialog() " + id);
        switch (id) {
        case DIALOG_INITIALIZING: {
            if (mProgressDialog == null) {
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setTitle(getString(R.string.initializing));
                mProgressDialog.setMessage("If it takes too long, push exit and restart.");
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
        		mProgressDialog.setButton(getString(R.string.exit), new DialogInterface.OnClickListener() {          
					@Override
        			public void onClick(DialogInterface dialog, int which) {
						finish();
						System.exit(0);
					}
        		});
            }
            return mProgressDialog;
            
        }
        
        case DIALOG_NETWORK_ERROR:
            return new AlertDialog.Builder(music.this)
                .setIcon(R.drawable.alert_dialog_icon)
                .setTitle(R.string.network_dialog_title)
                .setMessage(R.string.network_error_message)
                .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
				    	if (Utils.isNetworkAvailable(music.this)) {
                            if (!sInitialized) {
                                sInitialized = true;
                                checkFeedsAndUpdate();
                            }
				    	} else {
				    		mHandler.post(new Runnable() {
				    			public void run() {
						    		showDialog(DIALOG_NETWORK_ERROR);
				    			}
				    		});
				    	}
                    }
                })
                .setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	finish();
                    }
                })
                .create();
        
        case Feed.DOWNLOAD_APP_DIG: {
        	return Feed.createDownloadDialog(this); 
        }
        }
        return null;
    }
    
    private void initViews() {
        mSearch = new SearchBar(music.this, null);
        
        Button downloadsButton = (Button)findViewById(R.id.downloads_button);
        downloadsButton.setOnClickListener(null);
        downloadsButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(music.this, DownloadActivity.class);
				startActivity(intent);
			}
        	
        });
        
        Button advanceButton = (Button)findViewById(R.id.advance_search_button);
        advanceButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(music.this, SearchTab.class);
                startActivity(intent);
            }
            
        });
        
        Button ringButton = (Button)findViewById(R.id.ring_button);
        ringButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
	            Intent i = new Intent(music.this, RingSelectActivity.class);
	            startActivity(i);
			}
        });
        
        Button libraryButton = (Button)findViewById(R.id.music_library);
        libraryButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
	            Intent i = new Intent(music.this, TrackBrowserActivity.class);
	            startActivity(i);
			}
        });
        
        Button rate = (Button)findViewById(R.id.rate);
        rate.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				String url = "market://details?id=" + getPackageName();
    			
				try {
					Uri uri = Uri.parse(url);
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		    		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
        });
    }
    
    
    private class CheckUpdateTask extends AsyncTask<Void, Void, UpdateInfo> {
		@Override
		protected UpdateInfo doInBackground(Void... params) {
			return AppUpdater.checkUpdate(getApplication());
		}
		
		@Override
		protected void onPostExecute(final UpdateInfo update) {
			if (update == null)
				return;
			
			if (sActivity == null)
				return;
			
			new AlertDialog.Builder(sActivity)
			.setIcon(R.drawable.alert_dialog_icon)
			.setTitle(R.string.updater_dialog_title)
			.setMessage(update.getMessage()).setPositiveButton("Download",
					new DialogInterface.OnClickListener() {
        		@Override
        		public void onClick(DialogInterface dialog,
        				int whichButton) {
			        // Start the new activity
			        try {
			        	Uri uri = Uri.parse(update.getUrl());
			        	Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			        	startActivity(intent);
			        } catch (Exception ex) {
			        	ex.printStackTrace();
			        }
        		}
        	}).setNegativeButton("Cancel",
        			new DialogInterface.OnClickListener() {
        		@Override
        		public void onClick(DialogInterface dialog,
        				int whichButton) {
        		}
        	}).create().show();
		}
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    }
    
    
    @Override
    public void onStop() {
    	super.onStop();
    	//Debug.stopMethodTracing();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	if (Utils.isNetworkAvailable(this)) {
            if (!sInitialized) {
    			sInitialized = true;
                checkFeedsAndUpdate();
            }
    	} else {
    		showDialog(DIALOG_NETWORK_ERROR);
    	}
    }
   	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sActivity = this;
        
        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(this));
        
        setContentView(R.layout.main);
        /*        AdManager.setTestDevices( new String[] {                 
        	     AdManager.TEST_EMULATOR,             // Android emulator
        	     "E83D20734F72FB3108F104ABC0FFC738",  // My T-Mobile G1 Test Phone
        	     } ); */ 
        Utils.addMixedAds(this);
        
		if (!EulaActivity.checkEula(this)) {
			return;
		}
		
        initViews();
     }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	// TODO: Users are annoyed by this.
        // Bookmark.addBookmark(this, getContentResolver());
    	sActivity = null;
    }
    
    private void checkFeedsAndUpdate() {
    	if (sFeedsAndUpdateChecked)
    		return;
    	
    	sFeedsAndUpdateChecked = true;
        if (!Feed.runFeed(this, R.raw.feed)) {
        	checkUpdate();
        }
    }
    
    private void checkUpdate() {
    	Utils.D("Checking update");
    	new CheckUpdateTask().execute();
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.ringtone:
            Intent i = new Intent(this, RingSelectActivity.class);
            this.startActivity(i);
            break;
            
        /*
        case R.id.exit:
        	System.exit(0);
        	break;
       	*/
        
        case R.id.about:
        	new AlertDialog.Builder(this)
        	.setTitle(R.string.about)
            .setIcon(R.drawable.ic_dialog_info)
        	.setMessage(VersionUtils.getApplicationName(this) + " v" +
        			VersionUtils.getVersionNumber(this)).setPositiveButton("Update",
        			new DialogInterface.OnClickListener() {
        		@Override
        		public void onClick(DialogInterface dialog,
        				int whichButton) {
        			String url = AppUpdater.getNewUpdateUrl(getApplication());
        			
        			if (url != null) {
        				// Start the new activity
        				try {
        					Uri uri = Uri.parse(url);
        					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				    		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        					startActivity(intent);
        				} catch (Exception ex) {
        					ex.printStackTrace();
        				}
        			} else {
        				Utils.Info(music.this, "You app is update to date");
        			}
        		}
        	}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        		@Override
        		public void onClick(DialogInterface dialog, int which) {
        		}
        	}).create().show();
        	return true;
        }
        
        return false;
    }
}
