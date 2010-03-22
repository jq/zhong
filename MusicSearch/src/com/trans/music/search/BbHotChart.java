
package com.trans.music.search;


import com.admob.android.ads.*;
import com.feebe.lib.Util;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;

import java.util.ArrayList;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import android.content.DialogInterface;
import android.content.Intent;

import android.widget.BaseAdapter;
import android.view.LayoutInflater;
import android.content.Context;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.webkit.WebView;
import android.graphics.drawable.Drawable;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A list view example where the 
 * data for the list comes from an array of strings.
 */
public class BbHotChart extends Activity {
	
	ListView mTypesList;
	private String[] mCurTypes;
	private AdView mAd;

	JSONArray mFeedentries;
	
    ArrayAdapter<String> mAdapter; 
    ArrayList<String> mStrings = new ArrayList<String>();

    TrackListAdapter mTrackAdapter; 
    
	String mRequestUrl;
	
	String hottype;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.popular);
        
        mTypesList = (ListView) findViewById(R.id.popular);

        
        mAd = (AdView) findViewById(R.id.ad);	
        mAd.setVisibility(View.VISIBLE);
        
        //new AdsView(this);
        
        //mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mStrings);    
        //mTypesList.setAdapter(mAdapter);
		
		Bundle extras = getIntent().getExtras();
		hottype = extras.getString("type");

		mRequestUrl = urlString + hottype + "/";
		
		try{
			mFeedentries = new JSONArray();
        }catch(Exception e) {
			e.printStackTrace();
		} 

		mTrackAdapter = new TrackListAdapter(this);
		mTypesList.setAdapter(mTrackAdapter);
		
        //mTypesList.setTextFilterEnabled(true);


        mTypesList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {         
				try{
					JSONObject mp3 = mFeedentries.getJSONObject(position);			
					final String key = mp3.getString("keyword");
					
					Intent intent = new Intent();
	        Log.e("key", key);
					intent.putExtra(Const.Key, key);
					intent.setClass(BbHotChart.this, SearchList.class);
					startActivity(intent);	
					
					/*
					final String name = mp3.getString("Title");
					final String songer = mp3.getString("Artist");
										
					Intent intent = new Intent();
					intent.putExtra("keyword", key);
					intent.putExtra("artist", songer);
					intent.putExtra("song", name);
					
	                setResult(RESULT_OK, intent);
	                finish();
	                */

				}catch(JSONException e) {
					e.printStackTrace();
				} 
            }
        });
        
		if (hottype.equals("yahootop")) {
			findViewById(R.id.center_text).setVisibility(View.GONE);
			showDialog(CONNECTING);
			(new Thread() {
				public void run() {
					downloadFeeds();
					updatFeedList();

				}
			}).start();
		} else {
			downloadFeeds();
			updatFeedList();
		}
		
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
            return mFeedentries.length();
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
                convertView = mInflater.inflate(R.layout.yahootop_list_item, null);
				
                // Creates a ViewHolder and store references to the two children views
                // we want to bind data to.
                holder = new ViewHolder();
                
	            holder.line1 = (TextView) convertView.findViewById(R.id.line1);
	            holder.line2 = (TextView) convertView.findViewById(R.id.line2);
				holder.duration = (TextView) convertView.findViewById(R.id.duration);
				holder.play_indicator = (ImageView) convertView.findViewById(R.id.play_indicator);
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);
				
                convertView.setTag(holder);
            } else {
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                holder = (ViewHolder) convertView.getTag();
            }
			
			try{
	            // Bind the data efficiently with the holder.
	            JSONObject mp3 = mFeedentries.getJSONObject(position);
	            //title
				String name = mp3.getString("Title");
				holder.line1.setText(name);
				
				if (!hottype.equals("yahootop")) {
					//for none yahootop stuff, set artist for line2
					String songer = mp3.getString("Artist");
					if(songer != null && songer.length() > 1)
						holder.line2.setText(songer);
					else
						holder.line2.setText("Unknown Artist");
				} else {
					//for yahootop stuff, set pubDate for line2
					String pubDate = mp3.getString("pubDate");
					pubDate = pubDate + "\n";
					holder.line2.setText(pubDate);
					//and set the relative image
					String img = mp3.getString("img");
					Drawable image = ImageOperations(img,"image.jpg");
					holder.icon.setImageDrawable(image);
					holder.icon.setPadding(1, 1, 1, 1);
				}
			}catch(JSONException e) {
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
            ImageView icon;
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


    private String[] mType_Animals = {
            "Yahoo! Music Top Songs"
		};


	private String urlString = "http://www.heiguge.com/mp3/";

	//private String urlString = "http://192.168.1.180/mp3/";
	
	private void downloadFeeds() {
        try {
			Log.e("MusicSearch BbHotChart ", "downloadFeeds mRequestUrl: " + mRequestUrl);
			String httpresponse = null;
			if (Util.inCache(mRequestUrl, Const.OneWeek)) {
				httpresponse = Util.readFile(Const.cachedir+Util.getHashcode(mRequestUrl));
			} else {
				httpresponse = Util.download(mRequestUrl);
				Util.saveFileInThread(httpresponse, Const.cachedir+Util.getHashcode(mRequestUrl));
			}
			try {
				String json = httpresponse;
				mFeedentries = new JSONArray(json);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				return;
			}
		
			//urlConn.disconnect();
			

        } catch (Exception e) {
	        //ShowToastMessage("get feeds error: network");
        	e.printStackTrace();
		}
	}

	
	private void updatFeedList() {
		if (hottype.equals("yahootop")) {
			this.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					updateData();
					mProgressDialog.dismiss();
				}

			});
		} else {
			findViewById(R.id.center_text).setVisibility(View.GONE);
			updateData();
		}
	}
	
	private void updateData() {
		try{
			JSONArray feedEntries2 = new JSONArray();
			JSONArray entries = mFeedentries;
			Log.e("test", "length is " + entries.length());
			for(int i = 0; i < entries.length(); i++){
				if( entries.isNull(i) ){
					break;
				} 
				JSONObject mp3 = entries.getJSONObject(i);
			
				String name = mp3.getString("Title");
				if (!hottype.equals("yahootop")) {// we don't need Artist for yahoo top issue
					String songer = mp3.getString("Artist");
					mTrackAdapter.add(name + " [" + songer + "]");
				} else {
					mTrackAdapter.add(name);
				}
				
				feedEntries2.put(mp3);
			}
			//mFeedentries = feedEntries2;
		}catch(JSONException e) {
			e.printStackTrace();
		} 
	}

	private Drawable ImageOperations(String url, String saveFilename) {
		try {
			InputStream is = (InputStream) this.fetch(url);
			Drawable d = Drawable.createFromStream(is, saveFilename);
			return d;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Object fetch(String address) throws MalformedURLException,
			IOException {
		URL url = new URL(address);
		Object content = url.getContent();
		return content;
	}
	
	private static final int CONNECTING = 1;
	
	ProgressDialog   mProgressDialog;
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {

            case CONNECTING: {
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setMessage("Please wait while connect...");
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(true);
				mProgressDialog.setButton("Close", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {

	                    /* User clicked Yes so do some stuff */
	                }
	            });
                return mProgressDialog;
            }


        }
        return null;
    }
}

