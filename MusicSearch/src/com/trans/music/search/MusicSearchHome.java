package com.trans.music.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Random;

import java.text.SimpleDateFormat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.Activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Environment;
import android.os.IBinder;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.webkit.WebView;
import android.content.Context;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.net.Uri;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.view.View.OnClickListener;
import android.media.MediaScannerConnection;

import com.feebe.lib.AdsView;
import com.trans.music.search.help;

	
public class MusicSearchHome extends Activity 
	implements SeekBar.OnSeekBarChangeListener
	{
	LinearLayout mContentView;
	WebView mWebview;
	String mCurrentLink;
	boolean mStartSearchFlag = false;

	ImageButton mStartSearch, mPopular;
	EditText mQueryWords;
	ListView mSearchResults;
	ProgressDialog   mProgressDialog, mProgressDialogSearch, mProgressDialogPrepare, mProgressDownload;
	AlertDialog mAPPDownload;
	
	boolean mProgressDialogIsOpen = false, mDownloading = false, mTrackAdapterCreated = false;
	SeekBar  mSeekBar, mSeekBarOnline;
	//JSONArray mMp3entries;
    ArrayAdapter<String> mAdapter; 
    ArrayList<String> mStrings = new ArrayList<String>();

	
    int mSearchResultMp3index = -1, mMp3Lyricindex = -1, mFeedindex = -1, mLocalMp3index;
    String mCurSongArtist, mCurSongTitle;
	
	int  mSongProgress;
	long  mSongDuration;
	long  mSongPosition;

	private byte mSearchTimes = 0, mSearchExceed = 0;
	static public byte mDonateFlag = 0;
	
    private static final int REFRESH = 1;
    private boolean mPaused = false;
    
    private String SavedPath = "/sdcard/MusicSearch";
	//private boolean mHasSDCard = false;

	static final int SEARCHING = 1;
	static final int CONNECTING = 2;
	static final int DOWNLOAD_MP3FILE = 3;
	static final int DOWNLOAD_PRO = 4;
	static final int DOWNLOAD_APP = 5;
	static final int DONATE_APP = 6;
	
	private Menu mMenu;
    private Intent mServiceIntent = null;
	private IMediaPlaybackService mService = null;
	private ImageView mPlayStop;

	JSONArray mFeedentries;

	MediaScannerConnection mScanner;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
		setContentView(R.layout.home);
		mContentView = (LinearLayout)findViewById(R.id.mainview);
        
        //new AdsView(this);
        //mAdsenseView = (WebView) findViewById(R.id.adsWV);		
        AdsView.createQWAd(this);

       
        mStartSearch = (ImageButton) findViewById(R.id.search_button);
		mPopular = (ImageButton) findViewById(R.id.popular_button);
        mQueryWords = (EditText) findViewById(R.id.search_query_words);
        mSearchResults = (ListView) findViewById(R.id.search_mp3list);
        mSearchResults.setDividerHeight(1);
        
		mPlayStop = (ImageView)findViewById(R.id.play_stop);;
        mPlayStop.setOnClickListener(mPlayStopListener);
        mPlayStop.setEnabled(false);

		mCurSongArtist = new String();
		mCurSongTitle = new String();
		
		//mAd = (AdView) findViewById(R.id.ad);			
		try{
			mFeedentries = new JSONArray();
        }catch(Exception e) {
			e.printStackTrace();
		} 
				
        mSeekBar = (SeekBar)findViewById(R.id.play_seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setMax(1000);
		SeekBarSetSecondaryProgress(1000);
        mSeekBar.setEnabled(false);
        
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mStrings);    
        mSearchResults.setAdapter(mAdapter);
	
		//mTrackAdapter = new TrackListAdapter(this);
        //mSearchResults.setAdapter(mTrackAdapter);
		
        mSearchResults.setItemsCanFocus(true);	
		

		mServiceIntent = new Intent(this, MediaPlaybackService.class);
		startService(mServiceIntent);
        bindService((new Intent()).setClass(this,
                MediaPlaybackService.class), osc, 0);

        try{
			String status = Environment.getExternalStorageState();
			if(!status.equals(Environment.MEDIA_REMOVED)){

				File saveddir = new File(SavedPath);
				if(!saveddir.exists())
					saveddir.mkdirs();
							
			}

			//ScanMediaPath(SavedPath);
			
        }catch(Exception e) {
			e.printStackTrace();
		} 

		
        mStartSearch.setOnClickListener(
            new OnClickListener() {
                public void onClick(View v) {
					int maxcount;
					mCurSongArtist = "";
					mCurSongTitle = "";
					mMp3Lyricindex = -1;
					
					if(mSearchExceed == 1)
						maxcount = 1;
					else 
						maxcount = 50000;

					Log.e("MusicSearch ", "mStartSearch: " + mSearchTimes + ":" + maxcount + ":" + mDonateFlag);
					if(mSearchTimes > maxcount && mDonateFlag == 0){
						mSearchExceed = 1;
				        showDialog(DONATE_APP);
					
				    	try{
							byte[] b = new byte[1];
							FileOutputStream fcontrol =   openFileOutput("exceed.list", 0);
							b[0] = 1;
							fcontrol.write(b);
							fcontrol.close();
				    	}catch(IOException e) {
							e.printStackTrace();
						}

					}else{
    	                ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(mQueryWords.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS); 

                        final String queryWords = mQueryWords.getText().toString();
                        try{                       
                            final String queryWords2 = URLEncoder.encode(queryWords, "gb2312");

                            Intent intent = new Intent();
                			intent.putExtra("query", queryWords2);
                			intent.putExtra("search", true);
                        	intent.setClass(MusicSearchHome.this, MusicSearch.class);
                			startActivity(intent);	
                        }catch (java.io.UnsupportedEncodingException neverHappen) {
                        }
	                    //onSearchRequested();
					}
                }
            }
        );

     	
        mPopular.setOnClickListener(
            new OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent();
	            	intent.setClass(MusicSearchHome.this, StringList.class);
					startActivityForResult(intent, 1);
	            	//startActivity(intent);
                }
            }
        );

		
        mSearchResults.setOnItemClickListener(mFeedsItemListener);
		getFeeds();
		updatFeedList();  

		Random generator = new Random();
		int t = generator.nextInt();
		if (t % 2 != 0) {
			return;
		}

		(new Thread() {
			public void run() {
				downloadFeeds();

			}
		}).start();
		

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

	private OnItemClickListener mFeedsItemListener = new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {

				if(position == 0){
	                Intent intent = new Intent(MusicSearchHome.this, SingerLibrary.class);
	                startActivityForResult(intent, 1);
					return;
				}else if(position == 2){
	                Intent intent = new Intent(MusicSearchHome.this, ArtistList.class);
	                startActivityForResult(intent, 1);
					return;
				}
				
                mFeedindex = position - 3;


                //showDialog(DOWNLOAD_APP); 
                
				try {
                	JSONObject feed = mFeedentries.getJSONObject(mFeedindex);
                	final String uriString = feed.getString("uri");	
					//final String descript = feed.getString("descript");	
					//final String name = feed.getString("name");	
					
                    Intent intent = new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(uriString));
                    startActivity(intent);		
					
				}catch (Exception e){
				    Log.e("Download app", "error: " + e.getMessage(), e);
        	    }				
				
            }
        
	};
	
	
	protected void onPause() {
		super.onPause();
		Log.e("pause","pause");
	}
	
	protected void onResume() {
		updateTrackInfo();


		super.onResume();
		Log.e("resume","resume");
	}
	@Override
	protected void onDestroy() {
        try {
			if(mService.isPlaying() == true && mStartSearchFlag == false){
				Log.e("OnlineMusic ", "stop service");	
				mService.stop();
			}
        } catch (Exception ex) {
        	;
        }
						
		unbindService(osc);
    	super.onDestroy();
	}
	
    
	private void toggleProgressBarVisibility(final boolean visible) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				setProgressBarIndeterminateVisibility(visible);
			}
		});
	}

	private void SeekBarSetProgress(final int progress) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				mSeekBar.setProgress(progress);
			}
		});
	}
	private void SeekBarInit() {
		this.runOnUiThread(new Runnable() {
			public void run() {
				mSeekBar.setMax(1000);
				mSeekBar.setProgress(0);
			}
		});
	}

	private void ProgressDiagClose() {
		this.runOnUiThread(new Runnable() {
			public void run() {
			    removeDialog(CONNECTING);	
				mProgressDialogIsOpen = false;
				
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
	private void SeekBarSetEnalbe(final boolean enable) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				mSeekBar.setEnabled(enable);
			}
		});
	}


	
	private void ButtonsSetEnalbe(final boolean enable) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				/*
				mButtonPlayStop.setEnabled(enable);
				mButtonAddOnline.setEnabled(enable);
				if(enable == true){
					if(mHasSDCard == true)
						mButtonAddLocal.setEnabled(true);
				}else
					mButtonAddLocal.setEnabled(enable);
				*/
				mPlayStop.setEnabled(true);
				mPlayStop.setImageResource(R.drawable.stop);
				
			}
		});
	}
	
	private void MenuSetEnable(final boolean enable) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				MenuItem item = mMenu.findItem(R.id.menu_download);
				item.setEnabled(enable);  

			}
		});
	}	


	
	private void updatFeedList() {
		try{
			JSONObject popular = new JSONObject();
			mAdapter.add("Artist Library");
            mAdapter.add("Mobile Ringtones");
			//mTrackAdapter.add("The Week's Most Popular Songs");		
			//mTrackAdapter.add("Artist Library");
			
			JSONArray feedEntries2 = new JSONArray();
			JSONArray entries = mFeedentries;
				
			for(int i = 0; i < entries.length(); i++){
				if( entries.isNull(i) )
					break;
			    
				JSONObject mp3 = entries.getJSONObject(i);
			
				String name = mp3.getString("name");
				String uri = mp3.getString("uri");
				String type = mp3.getString("type");
				if(hasAppName(uri) == true && !type.equals("update"))
					continue;

				String sdkver = android.os.Build.VERSION.SDK;
				try{
					String ver = mp3.getString("v");
					if(!ver.equals(sdkver))
						continue;
						
				}catch(JSONException e) {
					e.printStackTrace();
				} 	
				
				if(type.length() > 0)
					mAdapter.add(name + " [" + type + "]");
					//mTrackAdapter.add(name + " [" + type + "]");
				else
					mAdapter.add(name);
					//mTrackAdapter.add(name);
				feedEntries2.put(mp3);
			}
			mFeedentries = feedEntries2;
		}catch(JSONException e) {
			e.printStackTrace();
		} 
	}
	


	private static final String urlString = "http://www.heiguge.com/mp3/getfeed/";
	private static final String feedsFile = "feeds";
	//urlString = "http://192.168.1.180/mp3/getfeed/";
	private void downloadFeeds() {
		URL url = null;
		HttpURLConnection urlConn = null;
		InputStream stream = null;
		InputStreamReader is = null;
        try {
        	url = new URL(urlString);
        	urlConn = (HttpURLConnection)url.openConnection();
        	urlConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3 -Java");
        	urlConn.setConnectTimeout(10000);
        	urlConn.connect();
        	
        	stream = urlConn.getInputStream();
			
        	StringBuilder builder = new StringBuilder(4096);
			
        	char[] buff = new char[4096];
			is = new InputStreamReader(stream);
			int len;
			while ((len = is.read(buff)) > 0) {
				builder.append(buff, 0, len);
			}
			String httpresponse = builder.toString();
			DataOutputStream feeds =  new DataOutputStream(openFileOutput(feedsFile, 0));
		    feeds.writeBytes(httpresponse);
			urlConn.disconnect();
        } catch (IOException e) {
	        //ShowToastMessage("get feeds error: network");
        	e.printStackTrace();
		}
	}

	private void getFeeds() {
		// if we have feedsFile then read it, otherwise read from resource
		InputStream feeds;
		StringBuilder builder;
		try {
			feeds = openFileInput(feedsFile);
		} catch (FileNotFoundException e) {
			feeds = getResources().openRawResource(R.raw.feed);
		}
		try {
			InputStreamReader r = new InputStreamReader(feeds);
			char[] buf = new char[4096];
			int len;
			builder = new StringBuilder(4096);
			while ((len = r.read(buf)) > 0) {
				builder.append(buf,0, len);
			}
		} catch (Exception e) {
			return;
		}
		try {
			String json = builder.toString();
			mFeedentries = new JSONArray(json);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			return;
		}
	}
	

	private void ShowToastMessage(final String message) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(MusicSearchHome.this, message, Toast.LENGTH_SHORT).show();
			}
		});
	}



	
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case SEARCHING: {
                mProgressDialogSearch = new ProgressDialog(this);
                mProgressDialogSearch.setMessage("Please wait while searching...");
                mProgressDialogSearch.setIndeterminate(true);
                mProgressDialogSearch.setCancelable(true);
				mProgressDialogIsOpen = true;
                return mProgressDialogSearch;
            }
            case CONNECTING: {
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setMessage("Please wait while connect...");
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(true);
				mProgressDialogIsOpen = true;
                return mProgressDialog;
            }
			// download progress
            case DOWNLOAD_MP3FILE: {
                mProgressDownload = new ProgressDialog(this);
                //mProgressDownload.setMessage("Download mp3 file : \n" + mMp3Local.substring(0, 64));
                mProgressDownload.setMessage("Download mp3 file ...");
				mProgressDownload.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				mProgressDownload.setMax(1000);
				mProgressDownload.setButton("Hide", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
						mProgressDownload.hide();
	                    /* User clicked Yes so do some stuff */
	                }
	            });				
				mProgressDialogIsOpen = true;
                return mProgressDownload;
			}

            case DONATE_APP: {
				try {
					
		            mAPPDownload =  new AlertDialog.Builder(this)
	                    .setIcon(R.drawable.icon)
	                    .setTitle("Donate MusicSearch")
	                    .setMessage("If you want continue to use this app, you should donate it cost $7.99 or buy MusicSearch Pro cost $9.99 to get unlimited using and search more results! Or Ignore it to search music once a day .")
	                    .setPositiveButton("PayPal",
	                            new DialogInterface.OnClickListener() {
	                                public void onClick(DialogInterface dialog,
	                                        int whichButton) {

						                Intent intent = new Intent(MusicSearchHome.this, paypal.class);
						                startActivity(intent);
	                                    /*
	                                    String token = PaypalDonate(); 
										//String uriString = "https://mobile.paypal.com/wc?t=" + token;
	                                    String uriString = "https://www.sandbox.paypal.com/wc?t=" + token;
	                                    Intent intent = new Intent(
	                                            Intent.ACTION_VIEW,
	                                            Uri.parse(uriString));
	                                    startActivity(intent);
	                                    */
	                                }
	                            }).  setNeutralButton("Buy Pro",
	                            new DialogInterface.OnClickListener() {
	                                public void onClick(DialogInterface dialog,
	                                        int whichButton) {
	                                    Intent intent = new Intent(
	                                            Intent.ACTION_VIEW,
	                                            Uri.parse("market://search?q=pname:com.trans.music.searchpro"));
	                                    startActivity(intent);
	                                }
	                            }).setNegativeButton("Ignore",
	                            new DialogInterface.OnClickListener() {
	                                public void onClick(DialogInterface dialog,
	                                        int whichButton) {
										mAPPDownload.dismiss();
	                                }
	                            }).create();	
	                   
		   
				
					return mAPPDownload;
				}catch (Exception e){
				    Log.e("Download app", "error: " + e.getMessage(), e);
        	    }	
			}
        }
        return null;
    }


    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
    	mSongProgress = progress;
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        ;
    }

    public void onStopTrackingTouch(SeekBar seekBar) { 
 	
        try {
        	mSongPosition = mService.position();
        	long position = (mSongDuration * mSongProgress)/1000;
            mService.seek(position);
        } catch (Exception ex) {
        	;
        }
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
        } catch (Exception ex) {
        	;
        }
        return 500;
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

    private boolean hasBrowser() {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final PackageManager manager = getPackageManager();
        final List<ResolveInfo> apps = manager.queryIntentActivities(
                mainIntent, 0);
        for (int i = 0; i < apps.size(); i++) {
            ResolveInfo info = apps.get(i);
            if (info.activityInfo.applicationInfo.packageName.equals("com.jie.browser") ||
                info.activityInfo.applicationInfo.packageName.equals("com.feebe.b1")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAppName(String appname) {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final PackageManager manager = getPackageManager();
        final List<ResolveInfo> apps = manager.queryIntentActivities(
                mainIntent, 0);
        for (int i = 0; i < apps.size(); i++) {
            ResolveInfo info = apps.get(i);
            if (appname.endsWith(info.activityInfo.applicationInfo.packageName) ) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Hold on to this
        mMenu = menu;
        
        // Inflate the currently selected menu XML resource.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);      
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_search:
				mQueryWords.requestFocus();
            	break;
  
            case R.id.menu_local:
                intent = new Intent(MusicSearchHome.this, local.class);
                startActivity(intent);
                return true;
            case R.id.menu_help:
                intent = new Intent(MusicSearchHome.this, help.class);
                startActivity(intent);
                return true;
            default:
                break;
        }
        
        return false;
    }

    private ServiceConnection osc = new ServiceConnection() {
            public void onServiceConnected(ComponentName classname, IBinder obj) {
                mService = IMediaPlaybackService.Stub.asInterface(obj);
				
                updateTrackInfo();	  
                long next = refreshSeekBarNow();
                queueNextRefresh(next);
            }
			
            public void onServiceDisconnected(ComponentName classname) {
		        try {
					mService.stop();
		        } catch (Exception ex) {
		        	;
		        }

            }
    };

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

	
}





