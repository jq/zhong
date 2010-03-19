
package com.trans.music.search;


import com.admob.android.ads.*;

import android.app.Activity;
import android.os.Bundle;

import java.util.ArrayList;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import android.view.View;
import android.view.View.OnClickListener;

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

import android.util.Log;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
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
		String hottype = extras.getString("type");

		mRequestUrl = urlString + hottype + "/";
		Log.e("BbHotChart", mRequestUrl);
		
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


		downloadFeeds();
		updatFeedList();

		/*
		(new Thread() {
			public void run() {
				downloadFeeds();
				updatFeedList();

			}
		}).start();
		*/
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
	            JSONObject mp3 = mFeedentries.getJSONObject(position);
				String name = mp3.getString("Title");
				String songer = mp3.getString("Artist");

			
			    String songinfo =  new String("");
				
	            holder.line1.setText(name);
				
				songinfo = songinfo + new String("\n");

				if(songer.length() > 1)
					songinfo = songinfo + songer;
				else
					songinfo = songinfo + new String("Unknown artist");

				
				holder.line2.setText(songinfo);


					
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
		URL url = null;
		HttpURLConnection urlConn = null;
		InputStream stream = null;
		InputStreamReader is = null;
        try {

			Log.e("MusicSearch BbHotChart ", "downloadFeeds mRequestUrl: " + mRequestUrl);
			
        	url = new URL(mRequestUrl);
        	urlConn = (HttpURLConnection)url.openConnection();
        	urlConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3 -Java");
        	urlConn.setConnectTimeout(10000);
        	urlConn.connect();
        	
        	stream = urlConn.getInputStream();
			
        	StringBuilder builder = new StringBuilder(64*1024);
			
        	char[] buff = new char[4096];
			is = new InputStreamReader(stream);
			int len;
			while ((len = is.read(buff)) > 0) {
				builder.append(buff, 0, len);
			}
			String httpresponse = builder.toString();

			try {
				String json = builder.toString();
				mFeedentries = new JSONArray(json);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				return;
			}
		
			urlConn.disconnect();
        } catch (IOException e) {
	        //ShowToastMessage("get feeds error: network");
        	e.printStackTrace();
		}
	}

	
	private void updatFeedList() {
		try{
			findViewById(R.id.center_text).setVisibility(View.GONE);
			
			JSONArray feedEntries2 = new JSONArray();
			JSONArray entries = mFeedentries;
			for(int i = 0; i < entries.length(); i++){
				if( entries.isNull(i) )
					break;
			    
				JSONObject mp3 = entries.getJSONObject(i);
			
				String name = mp3.getString("Title");
				String songer = mp3.getString("Artist");

				mTrackAdapter.add(name + " [" + songer + "]");
				
				feedEntries2.put(mp3);
			}
			//mFeedentries = feedEntries2;
		}catch(JSONException e) {
			e.printStackTrace();
		} 
	}

	
}

