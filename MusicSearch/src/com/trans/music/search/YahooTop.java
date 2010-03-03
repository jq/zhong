

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
import android.content.DialogInterface;

import android.app.Dialog;
import android.app.ProgressDialog;

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

import java.net.MalformedURLException;


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
public class YahooTop extends Activity {
	
	ListView mTypesList;
	private String[] mCurTypes;
	private AdView mAd;

	ProgressDialog   mProgressDialog;
		
	JSONArray mFeedentries;
	
    ArrayAdapter<String> mAdapter; 
    ArrayList<String> mStrings = new ArrayList<String>();

	TopListAdapter mTopAdapter; 
	
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

		try{
			mFeedentries = new JSONArray();
        }catch(Exception e) {
			e.printStackTrace();
		} 

		mTopAdapter = new TopListAdapter(this);
        mTypesList.setAdapter(mTopAdapter);
		mTypesList.setItemsCanFocus(true);	
		
        //mTypesList.setTextFilterEnabled(true);


        mTypesList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {         
				try{
					JSONObject mp3 = mFeedentries.getJSONObject(position);			
					final String key = mp3.getString("keyword");

	                Intent intent = new Intent();
					intent.putExtra("query", key);
					intent.putExtra("search", true);
	            	intent.setClass(YahooTop.this, MusicSearch.class);
					startActivity(intent);	

					/*
					Intent intent = new Intent();
					intent.putExtra("keyword", key);
					intent.putExtra("artist", "");
					intent.putExtra("song", "");
	                setResult(RESULT_OK, intent);
	                finish();
	                */

				}catch(JSONException e) {
					e.printStackTrace();
				} 
            }
        });

		showDialog(CONNECTING); 

		//downloadFeeds();
		//updatFeedList();

		(new Thread() {
			public void run() {
				downloadFeeds();
				updatFeedList();
			}
		}).start();
		
		

		/*
		(new Thread() {
			public void run() {
				downloadFeeds();
				updatFeedList();

			}
		}).start();
		*/
    }

    public class TopListAdapter extends BaseAdapter {

        private ArrayList<String> mMp3Title = new ArrayList<String>();
        private LayoutInflater mInflater;
		
        public TopListAdapter(Context c) {
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
				
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);
				//holder.icon.setBackgroundColor(0x00000000);  
				//holder.icon.setVisibility(View.GONE);
				
	            holder.line1 = (TextView) convertView.findViewById(R.id.line1);
	            holder.line2 = (TextView) convertView.findViewById(R.id.line2);
				holder.duration = (TextView) convertView.findViewById(R.id.duration);
				
                convertView.setTag(holder);
            } else {
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                holder = (ViewHolder) convertView.getTag();
            }
			
			try{
	            // Bind the data efficiently with the holder.
	            JSONObject mp3 = mFeedentries.getJSONObject(position);
				String mp3tile = mp3.getString("Title");
				String pubDate = mp3.getString("pubDate");
				String img = mp3.getString("img");
			
	            holder.line1.setText(mp3tile);

				pubDate = pubDate + "\n";
				holder.line2.setText(pubDate);

				Drawable image = ImageOperations(img,"image.jpg");
				
				//holder.icon.loadUrl(img); 
				holder.icon.setImageDrawable(image);
				holder.icon.setPadding(1, 1, 1, 1);
				
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
			ImageView icon;
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
		
		private Drawable ImageOperations(String url, String saveFilename) {
			try {			
				InputStream is = (InputStream) this.fetch(url);
				Drawable d = Drawable.createFromStream(is, "src");						
				return d;
			} catch (MalformedURLException e) {			
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			} 
	}
		public Object fetch(String address) throws MalformedURLException,IOException {
			URL url = new URL(address);
			Object content = url.getContent();
			return content;
		}	

    }


    private String[] mType_Animals = {
            "Yahoo! Music Top Songs"
		};


	private static final String urlString = "http://www.heiguge.com/mp3/yahootop/";

	//private static final String urlString = "http://192.168.1.180/mp3/yahootop/";
	
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

		this.runOnUiThread(new Runnable() {
			public void run() {
				try{
					findViewById(R.id.center_text).setVisibility(View.GONE);
					
					JSONArray feedEntries2 = new JSONArray();
					JSONArray entries = mFeedentries;
					for(int i = 0; i < entries.length(); i++){
						if( entries.isNull(i) )
							break;
					    
						JSONObject mp3 = entries.getJSONObject(i);
					
						String name = mp3.getString("Title");
						
						//mAdapter.add(name);
						mTopAdapter.add(name);
						
						feedEntries2.put(mp3);
					}

					mProgressDialog.dismiss();
					
					//mFeedentries = feedEntries2;
				}catch(JSONException e) {
					e.printStackTrace();
				} 
			}
		});
		
	}

	static final int CONNECTING = 1;
	
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
