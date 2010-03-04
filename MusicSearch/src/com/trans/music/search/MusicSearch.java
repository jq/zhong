package com.trans.music.search;

import com.admob.android.ads.*;
import com.feebe.lib.AdsView;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.CharBuffer;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.UnsupportedEncodingException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.apache.commons.lang.StringEscapeUtils;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Environment;
import android.os.Build.VERSION;
import android.os.IBinder;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;

import android.widget.BaseAdapter;
import android.view.LayoutInflater;
import android.content.Context;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.ServiceConnection;
import android.content.ComponentName;


import android.net.Uri;
import java.net.URLEncoder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;

import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;

import com.trans.music.search.help;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;


import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
	
public class MusicSearch extends Activity 
	implements SeekBar.OnSeekBarChangeListener
	{
	private UserTask<?, ?, ?> mTask;
    private DownloadService mDlService;
    
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

	
	TrackListAdapter mTrackAdapter; 
	
    int mSearchResultMp3index = -1, mMp3Lyricindex = -1, mFeedindex = -1, mLocalMp3index;
    String mCurSongArtist, mCurSongTitle;
	
	int  mSongProgress;
	long  mSongDuration;
	long  mSongPosition;

	private byte mSearchTimes = 0, mSearchExceed = 0;
	static public byte mDonateFlag = 0;
	
    private String mMp3Local = null, mMp3title, mMp3songer, m_CurDownloadFile = "";
    
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
    static final int CONNECT_ERROR = 7;
	
	private Menu mMenu;
	private Intent mServiceIntent = null;
	private IMediaPlaybackService mService = null;
	private ImageView mPlayStop;

	JSONArray mFeedentries;

	MediaScannerConnection mScanner;

	// Paypal donate
	private static String mToken;
	
	//private AdView mAd;
    //private WebView mAdsenseView = null;

    private final String mSearch_Url = "http://221.195.40.183/m?f=ms&tn=baidump3&ct=134217728&lf=&rn=&lm=0&word=";
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		mContentView = (LinearLayout)findViewById(R.id.mainview);
        
        //new AdsView(this);
        //mAdsenseView = (WebView) findViewById(R.id.adsWV);		
        AdsView.createQWAd(this);

        startService(new Intent(this,DownloadService.class));
        bindService(new Intent(this,DownloadService.class), mConnection, Context.BIND_AUTO_CREATE);

        
		mWebview = (WebView) findViewById(R.id.webview);;
        WebSettings s = mWebview.getSettings();
		s.setJavaScriptEnabled(true);
		s.setLoadsImagesAutomatically(true);
		mWebview.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");
		mWebview.setVisibility(View.GONE);
		mWebview.setWebViewClient(mWebClient);

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
		
        //mButtonPlayStop = (Button) findViewById(R.id.play_button_stop);
		//mButtonPlayStop.setOnClickListener(mPlayStopListener);
		
		/*
        mButtonClearOnline = (Button) findViewById(R.id.clear_button_online);
		mButtonDelLocal = (Button) findViewById(R.id.del_button_local);
		*/
        //ButtonsSetEnalbe(false);
	

		mServiceIntent = new Intent(this, MediaPlaybackService.class);
		startService(mServiceIntent);
        bindService((new Intent()).setClass(this,
                MediaPlaybackService.class), osc, 0);

				
        // Online Playlist UI
        /*
        mOnlineList = (ListView) findViewById(R.id.online_playlist);
        mOnlineAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mOnlineStrings);    
        mOnlineList.setAdapter(mOnlineAdapter);   
		*/

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

	                    onSearchRequested();
					}
                }
            }
        );

		Bundle extras = getIntent().getExtras();
		if(extras != null){
			if(extras.getString("query") != null)
				mQueryWords.setText(extras.getString("query"));

			mStartSearchFlag = extras.getBoolean("search", false);

			if(mStartSearchFlag == true){
				mPopular.setVisibility(View.GONE);
				onSearchRequested();
				return;
			}	
		}
		        	
        mPopular.setOnClickListener(
            new OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent();
	            	intent.setClass(MusicSearch.this, StringList.class);
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
		if (t % 10 != 0) {
			return;
		}

		(new Thread() {
			public void run() {
				downloadFeeds();

			}
		}).start();
		
        //mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }


    public class TrackListAdapter extends BaseAdapter {

        private ArrayList<String> mMp3Title = new ArrayList<String>();
        private LayoutInflater mInflater;
		
        public TrackListAdapter(Context c) {
            mContext = c;
			mInflater = LayoutInflater.from(c);
        }

        public int getCount() {
            //return mPhotos.size();
            //return mMp3entries.length();
            return mSongs.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.track_list_item, null);
				
                // Creates a ViewHolder and store references to the two children views
                // we want to bind data to.
                holder = new ViewHolder();
				
				ImageView iv = (ImageView) convertView.findViewById(R.id.icon);
				iv.setVisibility(View.GONE);
				
	            holder.line1 = (TextView) convertView.findViewById(R.id.line1);
	            holder.line2 = (TextView) convertView.findViewById(R.id.line2);
				holder.duration = (TextView) convertView.findViewById(R.id.duration);
				holder.play_indicator = (ImageView) convertView.findViewById(R.id.play_indicator);
				
                convertView.setTag(holder);
            } else {
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                holder = (ViewHolder) convertView.getTag();
            }
			
			try{
	            // Bind the data efficiently with the holder.
	            MP3Info mp3 = mSongs.get(position);

    			if(mp3.bNull){
    				holder.line1.setText("# Upgrade to \"MusicSearch Pro\" #");
                    holder.line2.setText(" Pro can get more results\n and search faster");
                    holder.duration.setText("");
    			}else{        
                    
    				String mp3tile = mp3.name;
    				String songer = mp3.artist;
    				String album = mp3.album;
    				String size = mp3.fsize;

    				holder.duration.setText(size);
    				
    			    String songinfo =  new String("");
    				
    	            holder.line1.setText(mp3tile);

    				if(album.length() > 1)
    					songinfo = songinfo + album;
    				else
    					songinfo = songinfo + new String("Unknown album");
    				
    				songinfo = songinfo + new String("\n");

    				if(songer.length() > 1)
    					songinfo = songinfo + songer;
    				else
    					songinfo = songinfo + new String("Unknown artist");

    				
    				holder.line2.setText(songinfo);

    				ImageView iv = holder.play_indicator;
    				try{
    					Log.e("MusicSearch", "play_indicator: " + mSearchResultMp3index + " - " + position + " playing:  " + mService.isPlaying() );
    					if((mSearchResultMp3index == position) && (mService.isPlaying() == true)){
    		                iv.setImageResource(R.drawable.indicator_ic_mp_playing_list);
    		                iv.setVisibility(View.VISIBLE);
    						Log.e("MusicSearch", "play_indicator: true");
    					}else{
    		                //iv.setImageResource(R.drawable.indicator_ic_mp_playing_list);
    		                //iv.setVisibility(View.VISIBLE);
    						iv.setVisibility(View.GONE);
    						Log.e("MusicSearch", "play_indicator: false");
    					}
    		        } catch (Exception ex) {
    		        	;
    		        }
                }
			}catch(Exception e) {
				e.printStackTrace();
			} 
			
            return convertView;

			
            // Make an ImageView to show a photo
            //ImageView i = new ImageView(mContext);

            //i.setImageResource(mPhotos.get(position));
            //i.setAdjustViewBounds(true);
            //i.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.WRAP_CONTENT,
            //        LayoutParams.WRAP_CONTENT));
            // Give it a nice background
            //i.setBackgroundResource(R.drawable.picture_frame);
            //return i;
        }


        class ViewHolder {
            TextView line1;
            TextView line2;
            TextView duration;
            TextView size;
            ImageView play_indicator;

        }

        private Context mContext;

        public void clear() {
            mMp3Title.clear();
            notifyDataSetChanged();
        }
        
        public void add(String name) {

            mMp3Title.add(name);
            notifyDataSetChanged();
        }

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

	private OnItemClickListener mResultsItemListener = new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                final int mp3index = position;
                mSearchResultMp3index = mp3index;
				String urlString, urlString2;
				InputStream stream = null;
				InputStreamReader is = null;
				URL url = null;
				HttpURLConnection urlConn = null;
				urlString = new String("");
				try {
                	MP3Info mp3 = mSongs.get(mp3index);
                	if(mp3.bNull)
                	{
                		showDialog(DOWNLOAD_PRO);
                		return;
                	}
                	urlString = mp3.link;
                	//Toast.makeText(MusicSearch.this, "Link: " + urlString, Toast.LENGTH_SHORT).show();
				}catch (Exception e) {
				    Log.e("MusicSearch", "error: " + e.getMessage(), e);
        	    }	
				
				if(!urlString.endsWith(".mp3")){
					showDialog(CONNECTING); 
					try {
	                	MP3Info mp3 = mSongs.get(mp3index);
				        String urlinput = mp3.link;

						Log.e("MusicSearch ", "mResultsItemListener url: " + urlinput);

						mWebview.loadUrl(urlinput); 				
						
				
						/*
						if(mDonateFlag == 1)
							urlString = "http://feebe.heiguge.com:8891/mp3/getlink/?url=" + urlinput;
						else{
							//urlString = "http://192.168.1.180/mp3/getlink/?url=" + urlinput;
							urlString = "http://www.heiguge.com/mp3/getlink/?url=" + urlinput;
						}

						Log.e("MusicSearch ", "mResultsItemListener urlString: " + urlString);

			        	url = new URL(urlString);
			        	urlConn = (HttpURLConnection)url.openConnection();
			        	urlConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3 -Java");
			        	
			        	urlConn.connect();
			        	
			        	stream = urlConn.getInputStream();
						
        				StringBuilder builder = new StringBuilder(4096);
				        	
			        	char[] buff = new char[4096];
						is = new InputStreamReader(stream);
						int len;
						while ((len = is.read(buff)) > 0) {
							builder.append(buff, 0, len);
						}
						urlString2 = builder.toString();
						
						Log.e("MusicSearch ", "mCurrentLink : " + mCurrentLink);
						mp3.put("local", mCurrentLink);
						mMp3entries.put(mp3index, mp3);
						*/						
	                	//Toast.makeText(MusicSearch.this, urlString2, Toast.LENGTH_SHORT).show();
					}catch (Exception e) {
					    Log.e("MusicSearch", "error: " + e.getMessage(), e);
	        	    }		
				}else{
					showActionChoice();
				}

            }
        
	};



	private OnItemClickListener mFeedsItemListener = new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {

				if(position == 0){
	                Intent intent = new Intent(MusicSearch.this, SingerLibrary.class);
	                startActivityForResult(intent, 1);
					return;
				}else if(position == 1){
	                Intent intent = new Intent(MusicSearch.this, ArtistList.class);
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
		Log.e("musicsearch","pause");
	}
	
	protected void onResume() {
		super.onResume();
		Log.e("musicsearch","resume");
	}
	@Override
	protected void onDestroy() {

		mWebview.clearCache(true);
		
        try {
			if(mService.isPlaying() == true && mStartSearchFlag == false){
				Log.e("OnlineMusic ", "stop service");	
				mService.stop();
			}
        } catch (Exception ex) {
        	;
        }
				
		unbindService(osc);
		//stopService(mServiceIntent);

    	if((mTask!=null)&&(mTask.getStatus()!=UserTask.TStatus.FINISHED))
    	{
    		//mTask.cancel(true);
    	}
        unbindService(mConnection);
    	super.onDestroy();
	}
	
    
    @Override
    public boolean onSearchRequested() {
		
        final String queryWords = mQueryWords.getText().toString();
        //mQueryWords.setText(null);

        //mAdsenseView.setVisibility(View.GONE);
        showDialog(SEARCHING);      

		//mAd.requestFreshAd();
		
        //final String queryWords2 = queryWords.replaceAll("[ ]", "%2B");
        mSearchTimes++;
		
		int maxcount;

		if(mTrackAdapterCreated == false){
			mTrackAdapter = new TrackListAdapter(this);
	        mSearchResults.setAdapter(mTrackAdapter);
			mTrackAdapterCreated = true;
		}

		mSearchResults.setOnItemClickListener(mResultsItemListener);



        try{
            final String queryWords2 = URLEncoder.encode(queryWords, "gb2312");

    		(new Thread() {
    			public void run() {
    				searchMusicFromNetwork2(queryWords2);    

    			}
    		}).start();            
        }catch (java.io.UnsupportedEncodingException neverHappen) {
        }

		
        return true;
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

	private void SetPlayIndicator() {
		this.runOnUiThread(new Runnable() {
			public void run() {
				mTrackAdapter.notifyDataSetChanged();
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
	private void clearSearchResultList() {
		this.runOnUiThread(new Runnable() {
			public void run() {
				try{
					//mAdapter.clear();
					mTrackAdapter.clear();
				}catch(Exception e) {
					e.printStackTrace();
				} 
			}
		});
	}
	
	private void updatSearchResulteList() {
		this.runOnUiThread(new Runnable() {
			public void run() {
				try{
					//mAd.setVisibility(View.VISIBLE);
					ArrayList<MP3Info> entries = mSongs;
					for(int i = 0; i < entries.size(); i++){

						
						MP3Info mp3 = entries.get(i);
					
						String mp3tile = mp3.name;
						String songer = mp3.artist;
						//String local = mp3.getString("local");
						//mAdapter.add(mp3tile + " [" + songer + "]" + local);
						//mAdapter.add(mp3tile + " [" + songer + "]");
						mTrackAdapter.add(mp3tile + " [" + songer + "]");
					}
                    removeDialog(SEARCHING);
				}catch(Exception e) {
					e.printStackTrace();
				} 
			}
		});
	}
	
	private void updatFeedList() {
		try{
			JSONObject popular = new JSONObject();
			mAdapter.add("The Week's Most Popular Songs");			
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
	
	private void searchMusicFromNetwork(String keyword) {
		URL url = null;
		HttpURLConnection urlConn = null;
		InputStream stream = null;
		InputStreamReader is = null;
		JSONArray entries;		
		String urlString;
        
		//urlString = "http://192.168.1.180/mp3/?json=1&word=" + "beat%2Bit";
        //urlString = "http://www.google.com/";
		//urlString = "http://192.168.1.180/mp3/?ver=1.2.0&json=1&word=" + keyword;
		//urlString = "http://192.168.1.180/mp3/?ver=1.5&hash=RFV200412SPACEFUCK&json=1&word=" + keyword;
		//urlString = "http://www.heiguge.com/mp3/?json=1&word=" + keyword;

		if(mDonateFlag == 1)
			urlString = "http://feebe.heiguge.com:8891/mp3/?ver=1.6&json=1&word=" + keyword;
		else
			urlString = "http://www.heiguge.com/mp3/?ver=1.6&json=1&word=" + keyword;	
			//urlString = "http://192.168.1.180/mp3/?ver=1.6&json=1&word=" + keyword;
		
        try {
        	url = new URL(urlString);
        	urlConn = (HttpURLConnection)url.openConnection();
        	urlConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3 -Java");
        	
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
			urlConn.disconnect();
			try{
				entries = new JSONArray(httpresponse);
				//mMp3entries = entries;
				clearSearchResultList();
				updatSearchResulteList();
				
			}catch(JSONException e) {
				ShowToastMessage("search error: parse results");
				e.printStackTrace();
			} 
			
			
        } catch (IOException e) {
	        ShowToastMessage("search error: network");
        	e.printStackTrace();
		}
		removeDialog(SEARCHING);
		mProgressDialogIsOpen = false;

	}

	
	private void searchMusicFromNetwork2(String keyword) {

		String reqURL = "";
        reqURL = mSearch_Url + keyword;

        if(mSongs != null)
            mSongs.clear();
        
        doDownload(reqURL);
		clearSearchResultList();
    	updatSearchResulteList();
	}

    public ArrayList<MP3Info> mSongs = new ArrayList<MP3Info>();
    
	public class MP3Info{
		public boolean bNull = false;
		public String name="";	
		public String artist="";	
		public String album="";
		public String fsize="";	
		public String rate="";		
		public String link="";	
		public boolean bisLinkValid = false;
		void setName(String n)
		{
			try {
				name = URLDecoder.decode(n, "gb2312");
			} catch (UnsupportedEncodingException e) {
			}
			name = StringEscapeUtils.unescapeHtml(name);
		}
		public String getName(){return name;} 
		void setArtist(String a)
		{
			//try {
			//	artist = URLDecoder.decode(a, "gb2312");
			//} catch (UnsupportedEncodingException e) {
			//}
			artist = a.replaceAll("\\<.*?>","");
			artist = StringEscapeUtils.unescapeHtml(artist);
		}
		public String getArtist(){
			if(artist.length()>0)
				return artist;
			return "Unknown Artist";
		}
		void setAlbum(String a)
		{
			album = a.replaceAll("\\<.*?>","");
			album = StringEscapeUtils.unescapeHtml(album);
		}
		public String getAlbum(){
			if(album.length()>0)
				return album;
			return "Unknown Album"; 
		}
		void setFSize(String f)
		{
			fsize = f;
		}
		public String getFSize(){
			if(fsize.length()>0)
				return fsize+"M";
			else
				return "0M";
		}
		void setRate(String r)
		{
			rate = r;
		}
		public String getRate(){return rate;}
		void setLink(String l)
		{
			link = l;
		}
		public String getLink(){return link;}
   }
	
	private Integer doDownload(String urlStr){
		//初始化歌曲列表
		mSongs = new ArrayList<MP3Info>();
        try {
            Log.e("MusicSearch ", "onSearchRequested: " + urlStr);
            
        	URL url = new URL(urlStr);
        	HttpURLConnection urlConn = (HttpURLConnection)url.openConnection();
        	urlConn.setRequestProperty("User-Agent", "Apache-HttpClient/UNAVAILABLE (java 1.4)");
        	urlConn.setConnectTimeout(12000);
        	urlConn.connect();
        	
        	InputStream stream = urlConn.getInputStream();
			
        	StringBuilder builder = new StringBuilder(8*1024);
			
        	char[] buff = new char[4096];
        	//必须在此指定编码，否则后面toString会导致乱码
        	InputStreamReader is = new InputStreamReader(stream,"gb2312");
			
			int len;
			while ((len = is.read(buff, 0, 4096)) > 0) {
				builder.append(buff, 0, len);
			}
			urlConn.disconnect();
			String httpresponse = builder.toString();
		
			Pattern pattern = Pattern.compile("<td class=d><a href=\"([\\s\\S]*?)\" title=\"");
			Matcher matcher = pattern.matcher(httpresponse);

			Pattern pattern_title = Pattern.compile("&si=(.*?);;.*?;;");
			Pattern pattern_title_2 = Pattern.compile("&tn=baidusg,(.*?)&si=");

			//Pattern pattern_artist = Pattern.compile("&si=.*?;;(.*?);;");
			int count = 0;
			while(matcher.find()) {
				
				MP3Info mp3 = new MP3Info();
				int pos2 = httpresponse.indexOf("</tr>",matcher.start());
				
				//获取歌手名
				int artistStartPos = httpresponse.indexOf("<td>",matcher.start());
				int artistEndPos = httpresponse.indexOf("</td>",artistStartPos);
				if((artistStartPos>0)&&(artistStartPos<artistEndPos))
				{
					artistStartPos = httpresponse.indexOf(">",artistStartPos+12);
					int artistEndPos2 = httpresponse.indexOf("</a>",artistStartPos);
					if((artistEndPos>0)&&(artistEndPos2<artistEndPos))
						mp3.setArtist(httpresponse.substring(artistStartPos+1,artistEndPos2));
				}
				//获取连接速度
				int gifpos = httpresponse.lastIndexOf(".gif",pos2);
				if((gifpos>0)&&(gifpos<pos2))
				{
					mp3.setRate(httpresponse.substring(gifpos-1, gifpos));
				}
				//获取文件尺寸
				int sizePos = httpresponse.lastIndexOf(" M</td>",gifpos);
				if((sizePos>0)&&(sizePos<pos2))
				{
					int sizePos2 = httpresponse.indexOf(">",sizePos-6);
					mp3.setFSize(httpresponse.substring(sizePos2+1,sizePos));
				}
				//获取专辑名称
				int albumPos = httpresponse.indexOf("<td class=al><a",matcher.start());
				if((albumPos>0)&&(albumPos<pos2))
				{
					albumPos = httpresponse.indexOf(">",albumPos+16);
					int albumPos2 = httpresponse.indexOf("</a",albumPos);
					if((albumPos2>0)&&(albumPos2<pos2))
						mp3.setAlbum(httpresponse.substring(albumPos+1,albumPos2));
				}
				
				String link = matcher.group(1);
				Matcher matcher_title = pattern_title.matcher(link);
				matcher_title.find();
				
				if(matcher_title.group(1).length() == 0){
					matcher_title = pattern_title_2.matcher(link);
					matcher_title.find();
				}
				
				//Matcher matcher_artist = pattern_artist.matcher(link);
				//matcher_artist.find();
				
			
				mp3.setName(matcher_title.group(1));
			//	mp3.setArtist(matcher_artist.group(1));
				mp3.setLink(link);
				mSongs.add(mp3);

                count++;
                if(count >= 25)
                    break;
			}
			
			if((mSongs!=null)&&(!mSongs.isEmpty())){
				//免费版添加提示信息，Tao版会添加下一页的link
				MP3Info mp3Tip = new MP3Info();
				mp3Tip.bNull = true;
				mSongs.add(mp3Tip);
			}
        } catch (Exception e) {
	        //ShowToastMessage("Network can not connect, please try again.");
        	return null;
		}
        return 1;
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
	
	private void DownloadSetMax(final int progress) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				mProgressDownload.setMax(progress);
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
	private void DownloadShowMessage(final String message) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(MusicSearch.this, message, Toast.LENGTH_LONG).show();
			}
		});
	}	

	private void ShowToastMessage(final String message) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(MusicSearch.this, message, Toast.LENGTH_SHORT).show();
			}
		});
	}
	/*
	private void updateDownloadList() {
		this.runOnUiThread(new Runnable() {
			public void run() {
				try{
					File[] file=(new File(SavedPath)).listFiles();
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
	*/

	public class DownloadTask2 extends UserTask<String, String, Integer>{
		public Integer doInBackground(String... urls) {
            Log.e("Download", "doInBackground");    
            mDownloading = true;
            publishProgress(urls[0]);
			return 1;

		}
        public void onProgressUpdate(String... response) {
            removeDialog(CONNECTING);   
            Log.e("Download", "onProgressUpdate");
            if(mDlService != null){
                boolean started = mDlService.Download(response[0], m_CurDownloadFile);
                if(started)
                    Toast.makeText(MusicSearch.this, "Starting download...", Toast.LENGTH_LONG).show();	

                
            }           
        }		
		public void onPostExecute(Integer result) {
            removeDialog(CONNECTING);
            
		}
		
	}

    private NotificationManager mNM;
	public void showDownloadOKNotification(String title)
	{
	    String fullpathname = SavedPath + "/" + m_CurDownloadFile;
        
		Notification n = new Notification();
        n.icon = android.R.drawable.stat_sys_download_done;
		n.flags |= Notification.FLAG_AUTO_CANCEL;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + fullpathname), "audio/mp3");

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,intent, 0);
        n.setLatestEventInfo(
        		this, 
        		title,
        		"Download completed",
        		contentIntent);
        mNM.notify(R.string.local_service_started, n);
	}
    
    private class DownloadTask extends UserTask<String, Integer, Integer>{
        public Integer doInBackground(String... urls) {
            
    		URL url = null;
    		HttpURLConnection urlConn = null;

    		String urlString;
            
    		urlString = urls[0];
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
    			String fullpathname = SavedPath + "/" + m_CurDownloadFile;
    			//FileOutputStream filemp3 =   openFileOutput(filename, MODE_WORLD_READABLE);						
    			FileOutputStream filemp3 = new FileOutputStream(fullpathname);

    			byte[] buff = new byte[64 * 1024];
    			int len ;
    			fileStream = new DataInputStream(new BufferedInputStream(urlConn.getInputStream()));
    			while ((len = fileStream.read(buff)) > 0) {
    				filemp3.write(buff, 0, len);
    				downed +=  len;
    				publishProgress((int) downed);
    			}        	

    			filemp3.close();
                return 1;
            } catch (IOException e) {
            	e.printStackTrace();
                return 0;
    		}

        }

        public void onProgressUpdate(Integer... progress) {
            mProgressDownload.setProgress(progress[0]);
        }

        public void onPostExecute(Integer result) {

            if(result == 1){
                Toast.makeText(MusicSearch.this, m_CurDownloadFile + " download finished", Toast.LENGTH_LONG).show();
    			//DownloadShowMessage(m_CurDownloadFile + " download finished");
    			//updateDownloadList();
                String fullpathname = SavedPath + "/" + m_CurDownloadFile;
    			ScanMediafile(fullpathname);
                //showDownloadOKNotification(m_CurDownloadFile);

            }   

    	    removeDialog(DOWNLOAD_MP3FILE);
    		mDownloading = false;
    		mProgressDialogIsOpen = false;

        }
    }
        
	private void DownloadMusic(String filename, String mp3url) {
		URL url = null;
		HttpURLConnection urlConn = null;

		String urlString;
        
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

			DownloadShowMessage(filename + " download finished");
			//updateDownloadList();

			ScanMediafile(fullpathname);
			
        } catch (IOException e) {
        	e.printStackTrace();
		}
	    removeDialog(DOWNLOAD_MP3FILE);
		//mProgressDownload.dismiss();
		mDownloading = false;
		mProgressDialogIsOpen = false;
	}

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
			
	        case DOWNLOAD_PRO:{
	        	String ver = android.os.Build.VERSION.SDK;
	        	if(ver.compareTo(new String("3")) == 0){
		            mAPPDownload = new AlertDialog.Builder(this)
	                    .setIcon(R.drawable.icon)
	                    .setTitle("Download MusicSearchPro")
	                    .setMessage(
	                            "If you want continue to use this app, you should download MusicSearchPro! Or Ignore it to search music once a day .")
	                    .setPositiveButton("Download",
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
										/*
	                                    Intent intent = new Intent(
	                                            Intent.ACTION_VIEW,
	                                            Uri.parse("market://search?q=pname:com.transcoder.music.searchpro"));
	                                    startActivity(intent);
	                                    */

	                                }
	                            }).create();
	        	}else{
		            mAPPDownload =  new AlertDialog.Builder(this)
	                    .setIcon(R.drawable.icon)
	                    .setTitle("Download MusicSearchPro")
	                    .setMessage(
	                            "If you want continue to use this app, you should download MusicSearchPro! Or Ignore it to search music once a day .")
	                    .setPositiveButton("Download",
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
										/*
	                                    Intent intent = new Intent(
	                                            Intent.ACTION_VIEW,
	                                            Uri.parse("market://search?q=pname:com.transcoder.music.searchpro"));
	                                    startActivity(intent);
	                                    */

	                                }
	                            }).create();
	        	}
				return mAPPDownload;
        	}				
            case CONNECT_ERROR: {
                return  new AlertDialog.Builder(this)
					.setIcon(R.drawable.icon)
					.setTitle("Connect error ! ")
	                .setMessage("This music link is invalid, please try anothor.")
	                .setCancelable(true)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		                public void onClick(DialogInterface dialog, int whichButton) {
		                                
		                }
		            }).create();

            }
			// download app
            case DOWNLOAD_APP: {
				try {
                	JSONObject feed = mFeedentries.getJSONObject(mFeedindex);
                	final String uriString = feed.getString("uri");	
					final String descript = feed.getString("descript");	
					final String name = feed.getString("name");	
					
		            mAPPDownload =  new AlertDialog.Builder(this)
	                    .setIcon(R.drawable.icon)
	                    .setTitle("Download " + name)
	                    .setMessage(descript)
	                    .setPositiveButton("Download",
	                            new DialogInterface.OnClickListener() {
	                                public void onClick(DialogInterface dialog,
	                                        int whichButton) {

	                                    Intent intent = new Intent(
	                                            Intent.ACTION_VIEW,
	                                            Uri.parse(uriString));
	                                    startActivity(intent);
	                                }
	                            }).setNegativeButton("Ignore",
	                            new DialogInterface.OnClickListener() {
	                                public void onClick(DialogInterface dialog,
	                                        int whichButton) {
										mAPPDownload.dismiss();
	                                }
	                            }).create();	
	                   
					/*
	                mAPPDownload = (AlertDialog)new AlertDialog.Builder(this);
					mAPPDownload.setIcon(R.drawable.icon);
					mAPPDownload.setTitle("Download " + name);
	                mAPPDownload.setMessage(descript);
	                mAPPDownload.setCancelable(true);
					mAPPDownload.setButton("Download", new DialogInterface.OnClickListener() {
		                public void onClick(DialogInterface dialog, int whichButton) {
		                                Intent intent = new Intent(
		                                        Intent.ACTION_VIEW,
		                                        Uri.parse(uriString));
		                                startActivity(intent);
		                }
		            }); */        
				
					return mAPPDownload;
				}catch (Exception e){
				    Log.e("Download app", "error: " + e.getMessage(), e);
        	    }	
            	}	
			// donate app
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

						                Intent intent = new Intent(MusicSearch.this, paypal.class);
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
            case R.id.menu_download:
			    if(mDownloading == false){
                	try{
						if(mSongs.size() > 0){
							if(mSearchResultMp3index >= 0){
		                		MP3Info mp3 = mSongs.get(mSearchResultMp3index);
								String url = mp3.link;       	
		                    	mMp3Local = url.replaceAll("[ ]", "%20");
								mMp3title = mp3.name;
								mMp3songer = mp3.artist;
								try{
									if(mService.isPlaying() == true){
									    mService.pause();
									    mPlayStop.setImageResource(R.drawable.play);
							    	    mPaused = true;
									}
						        } catch (Exception ex) {
						        	;
						        }
								showDialog(DOWNLOAD_MP3FILE); 

								(new Thread() {
									public void run() {
										
										DownloadMusic(mMp3title + "[" + mMp3songer + "]" + ".mp3", mMp3Local);    
									}
								}).start();
							}else
								Toast.makeText(MusicSearch.this, "Please select link in search results first", Toast.LENGTH_SHORT).show();
						}else
							Toast.makeText(MusicSearch.this, "Please search music first", Toast.LENGTH_SHORT).show();
						
	                }catch(Exception e) {
						e.printStackTrace();
					} 
			    }else{
					mProgressDownload.show();
				}
                return true;
            case R.id.menu_local:
                intent = new Intent(MusicSearch.this, local.class);
                startActivity(intent);
                return true;
            case R.id.menu_help:
                intent = new Intent(MusicSearch.this, help.class);
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


	// paypal donate
    private String PaypalDonate(){	
		String urlString;
        
		urlString = "https://api-3t.sandbox.paypal.com/nvp";
		//urlString = "https://api-3t.paypal.com/nvp";
		
        Map map = new HashMap();
		map.put("METHOD", "SetMobileCheckout");
		map.put("VERSION", "51.0");
        map.put("USER", "waf.ya_1243923059_biz_api1.gmail.com");
        map.put("PWD", "1243923076");
		map.put("SIGNATURE", "AKLmkqPFFE7vwEEbGPrfwjOAZSf-A2GE2onAxvbQd69o8ao.h1oGoxAS");
		
		map.put("AMT", "7.99");
		map.put("CURRENCYCODE", "USD");
		map.put("DESC", "MusicSearch Donate");
		map.put("RETURNURL", "http://127.0.0.1/test/");
		
        String temp = doPost(urlString, map, null);
        Log.e("MusicSearch Paypal", "response: " + temp);
        return temp;
		
    }


    public static String doPost(String reqUrl, Map parameters,
            String recvEncoding)
    {
        HttpURLConnection url_con = null;
        //URLConnection url_con = null;
        String responseContent = null;
		InputStream stream = null;
		InputStreamReader is = null;
        try
        {
            StringBuffer params = new StringBuffer();
            for (Iterator iter = parameters.entrySet().iterator(); iter
                    .hasNext();)
            {
                Entry element = (Entry) iter.next();
                params.append(element.getKey().toString());
                params.append("=");
				if(recvEncoding == null)
	                params.append(URLEncoder.encode(element.getValue().toString()));
				else
					params.append(URLEncoder.encode(element.getValue().toString(), recvEncoding));
                params.append("&");
            }

            if (params.length() > 0)
            {
                params = params.deleteCharAt(params.length() - 1);
            }

	        HttpsURLConnection.setDefaultHostnameVerifier(hv);
			
            URL url = new URL(reqUrl);
            url_con = (HttpURLConnection) url.openConnection();
            //url_con = (URLConnection) url.openConnection();
            url_con.setRequestMethod("POST");
        	url_con.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3 -Java");
			

            url_con.setDoOutput(true);
            byte[] b = params.toString().getBytes();
            url_con.getOutputStream().write(b, 0, b.length);
            url_con.getOutputStream().flush();
            url_con.getOutputStream().close();			

			url_con.connect();
			
        	stream = url_con.getInputStream();
			
        	StringBuilder builder = new StringBuilder(4096);
			
        	char[] buff = new char[4096];
			is = new InputStreamReader(stream);
			int len;
			while ((len = is.read(buff)) > 0) {
				builder.append(buff, 0, len);
			}
			String httpresponse = builder.toString();
			String token = httpresponse.substring(httpresponse.indexOf("&TOKEN=") + new String("&TOKEN=").length());
			mToken  = new String(token);
			
        }
        catch (IOException e)
        {
			Log.e("MusicSearch", "error: " + e.getMessage(), e);
        }
        finally
        {
            if (url_con != null)
            {
                url_con.disconnect();
            }
        }
        return mToken;
    }

    static HostnameVerifier hv = new HostnameVerifier() {
        public boolean verify(String urlHostName, SSLSession session) {
            System.out.println("Warning: URL Host: " + urlHostName + " vs. "
                               + session.getPeerHost());
            return true;
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
			Bundle extras = data.getExtras();
			String key = extras.getString("keyword");
			mCurSongArtist = extras.getString("artist");
			mCurSongTitle = extras.getString("song");

			Log.e("MusicSearch :MusicSearch ", "artist: " + mCurSongArtist + "  song: " + mCurSongTitle);
			
			mQueryWords.setText(key);
			
			onSearchRequested();
        } 
    }

	void showActionChoice(){
		new AlertDialog.Builder(MusicSearch.this)
            .setTitle("Actions")
            .setItems(R.array.select_dialog_items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
					if(which == 0){			
		                (new Thread() {
		        			public void run() {				
								try {
									showConnectDiaglog(true);
									MP3Info mp3 = mSongs.get(mSearchResultMp3index);
				                    String mp3url = mp3.link;    	
				                    mMp3Local = mp3url.replaceAll("[ ]", "%20");
									mService.stop();
									mService.openfile(mMp3Local);
									mService.play();

									SetPlayIndicator();
									//mProgressDialog.dismiss();
									mProgressDialogIsOpen = false;	
									
									SeekBarSetEnalbe(true);	
									ButtonsSetEnalbe(true);
									
									mSongDuration = mService.duration(); 
							        long next = refreshSeekBarNow();
							        queueNextRefresh(next);
									showConnectDiaglog(false);
									
							        
								}catch(Exception e) {
								    showConnectDiaglog(false);
								    showConnectErrorDiaglog(true);
									e.printStackTrace();
								} 
			    			}
		        		}).start();

					}else if(which == 1){
					    removeDialog(CONNECTING);
						//mProgressDialog.dismiss();
						mProgressDialogIsOpen = false;	
                        //if(mDlService.mDownloading == false){
					    if(mDownloading == false){
		                	try{
								if(mSongs.size() > 0){
									if(mSearchResultMp3index >= 0){
				                		MP3Info mp3 = mSongs.get(mSearchResultMp3index);
										String url = mp3.link;
				                    	mMp3Local = url.replaceAll("[ ]", "%20");
										mMp3title = mp3.name;
										mMp3songer = mp3.artist;
										try{
											if(mService.isPlaying() == true){
											    mService.pause();
											    mPlayStop.setImageResource(R.drawable.play);
									    	    mPaused = true;
											}
								        } catch (Exception ex) {
								        	;
								        }
										showDialog(DOWNLOAD_MP3FILE); 
										m_CurDownloadFile = mMp3title + "[" + mMp3songer + "]" + ".mp3";
                                        mTask = new DownloadTask().execute(mMp3Local);
                                        /*
										(new Thread() {
											public void run() {
												m_CurDownloadFile = mMp3title + "[" + mMp3songer + "]" + ".mp3";
                                                new DownloadTask().execute(mMp3Local);
												//DownloadMusic(m_CurDownloadFile, mMp3Local);    
											}
										}).start();
										*/
									}else
										Toast.makeText(MusicSearch.this, "Please select link in search results first", Toast.LENGTH_SHORT).show();
								}else
									Toast.makeText(MusicSearch.this, "Please search music first", Toast.LENGTH_SHORT).show();
								
			                }catch(Exception e) {
								e.printStackTrace();
							} 
					    }else{
					        mProgressDownload.show();
							//Toast.makeText(MusicSearch.this, "Current download not finish, please try later.", Toast.LENGTH_SHORT).show();
						}

						
					}
				}
            })
            .create().show();	
	}
	
	void showConnectDiaglog(final boolean show){
		this.runOnUiThread(new Runnable() {
			public void run() {			
					if(show == true)
						showDialog(CONNECTING);
					else
						removeDialog(CONNECTING);				
			}
		});
	}	

	void showConnectErrorDiaglog(final boolean show){
		this.runOnUiThread(new Runnable() {
			public void run() {			
					if(show == true)
						showDialog(CONNECT_ERROR);
					else
						removeDialog(CONNECT_ERROR);				
			}
		});
	}	
    
	void updateMp3Link(){
		this.runOnUiThread(new Runnable() {
			public void run() {			
					removeDialog(CONNECTING);
					showActionChoice();
		
					mContentView.removeView(mWebview);
					mContentView.requestFocus();
				
			}
		});
	}
	class MyJavaScriptInterface  
	{  
	    @SuppressWarnings("unused")  
	    public void showHTML(String html)  
	    {  
	    	Log.e("MusicSearch ", "get link : " + html);
			mCurrentLink = html;
			try{
				MP3Info mp3 = mSongs.get(mSearchResultMp3index);              	
				mp3.link = mCurrentLink;
			}catch(Exception e) {
				e.printStackTrace();
			} 			

			updateMp3Link();
				
			
	    }  
	}  

	WebViewClient mWebClient = new WebViewClient(){
		
		public void  onPageFinished(WebView view, String url){
			//mWebview.requestFocus();
			Log.e("MusicSearch ", "finished url: " + url);

			mWebview.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementById('urla').href);");
			
		}
		
		public boolean  shouldOverrideUrlLoading(WebView view, String url){
			Log.e("MusicSearch ", "shouldOverrideUrlLoading url: " + url);
			return false;
			
		}

	};



    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mDlService = ((DownloadService.LocalBinder)service).getService();
            Log.i("ServiceConnection", "Connected! Name: " + className.getClassName());

        }

        public void onServiceDisconnected(ComponentName className) {
            Log.i("ServiceConnection", "Disconnected!");
            mDlService = null;
        }
    };

	
}





