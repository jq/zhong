package com.trans.music.search;

import android.app.Activity;
import android.os.Bundle;

import java.util.ArrayList;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;
import android.widget.ImageButton;
import android.view.View;
import android.view.View.OnClickListener;
import android.graphics.Bitmap;
import android.content.Intent;

import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 *歌手详细信息  
 */
public class Artist extends Activity {

	final static String LASTFM_KEY = "c309476a7d0c6c29d4ed6632678c5054";
	ListView mTopTracksList;
	TextView mName;
	ImageView mImage;
   
	String mRequestUrl;
	Bitmap mArtistImage;
	String mArtistName, mSummary, mContent;

    ArrayAdapter<String> mTopTracksAdapter; 
    ArrayList<String> mTopTracksStrings = new ArrayList<String>();

	ArrayList<String> mTopTracks = new ArrayList<String>();
	
	//Button removeButton;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.artist);
		
		Bundle extras = getIntent().getExtras();
		mArtistName = extras.getString("name");
		Log.e("OnlineMusic ", "getString name : " + mArtistName);	

		mTopTracksList = (ListView) findViewById(R.id.toptracks);
		mTopTracksList.setSelectionAfterHeaderView();
		mTopTracksList.setVisibility(View.GONE);
		LayoutInflater mInflater;
		mInflater = LayoutInflater.from(this);
						
		View convertView = mInflater.inflate(R.layout.artist_title, null);	
		mName = (TextView)convertView.findViewById(R.id.name);
		mName.setText(mArtistName);
		mImage = (ImageView)convertView.findViewById(R.id.image);		
		mTopTracksList.addHeaderView(convertView);



		View separator = mInflater.inflate(R.layout.separator, null);
		TextView text = (TextView)separator.findViewById(R.id.text);
		text.setText(mArtistName + "'s Top Songs"); 
		mTopTracksList.addHeaderView((View)separator);
		
        mTopTracksAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mTopTracksStrings);    
        mTopTracksList.setAdapter(mTopTracksAdapter);
        mTopTracksList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {    
				if(position > 1){
	                Intent intent = new Intent();
					intent.putExtra("query", mTopTracks.get(position - 2));
					intent.putExtra("search", true);
	            	intent.setClass(Artist.this, MusicSearch.class);
					startActivity(intent);	
				}
            }
        });

					
		// get artist info
		(new Thread() {
			public void run() {
				String reqUrl = "";
				try{ 
					reqUrl = "http://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=" + URLEncoder.encode(mArtistName, "UTF-8") + "&api_key=" + LASTFM_KEY;
		        }catch (java.io.UnsupportedEncodingException neverHappen) {
		        }			
				getArtistInfo( reqUrl);
				updatArtistInfo();

			}
		}).start();
		// get artist top tracks 
		(new Thread() {
			public void run() {
				String reqUrl = "";
				try{ 
					reqUrl = "http://ws.audioscrobbler.com/2.0/?method=artist.gettoptracks&artist=" + URLEncoder.encode(mArtistName, "UTF-8") + "&api_key=" + LASTFM_KEY;
		        }catch (java.io.UnsupportedEncodingException neverHappen) {
		        }					
				getArtistTopTracks(reqUrl);
				updatArtistTopTracks();

			}
		}).start();

    }

	
	private void getArtistInfo(String rurl) {
		URL url = null;
		HttpURLConnection urlConn = null;
		InputStream stream = null;
		InputStreamReader is = null;
        try {

			Log.e("OnlineMusic", "searchFromNetwork mRequestUrl: " + rurl);
			
        	url = new URL(rurl);
        	urlConn = (HttpURLConnection)url.openConnection();
        	urlConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3 -Java");
        	urlConn.connect();
        	
        	stream = urlConn.getInputStream();
			
        	StringBuilder builder = new StringBuilder(8*1024);
			
        	char[] buff = new char[4096];
			is = new InputStreamReader(stream);
			
			int len;
			while ((len = is.read(buff, 0, 4096)) > 0) {
				builder.append(buff, 0, len);
			}
			urlConn.disconnect();
			
			String httpresponse = builder.toString();

			//Log.e("OnlineMusic", "httpresponse : " + httpresponse);
			
			Pattern pattern = Pattern.compile("<image size=\"extralarge\">([\\s\\S]*?)</image>");
			Matcher matcher = pattern.matcher(httpresponse);
			
			if(matcher.find()) {	
				Log.e("OnlineMusic", "pattern : " + matcher.group(1));
				mArtistImage = NetUtils.loadBitmap(matcher.group(1));
			}
			
			pattern = Pattern.compile("<summary>([\\s\\S]*?)</summary>");
			matcher = pattern.matcher(httpresponse);	
			String str_nohtml;
			if(matcher.find()) {	
				str_nohtml = matcher.group(1).replaceAll("<!\\[CDATA\\[", "").replaceAll("\\]\\]>", "");
				str_nohtml = str_nohtml.replaceAll("\\&[a-zA-Z]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "");
				Log.e("OnlineMusic", "summary 1: " + matcher.group(1));
				Log.e("OnlineMusic", "summary 2: " + str_nohtml);
				if(str_nohtml.length() > 108)
					mSummary  = str_nohtml.substring(0, 108);  
				else 
					mSummary = str_nohtml;
				mSummary += "...(more)";  
				mContent = new String(str_nohtml.getBytes(), "UTF-8");
			}			
			/*
			pattern = Pattern.compile("<content>([\\s\\S]*?)</content>");
			matcher = pattern.matcher(httpresponse);			
			if(matcher.find()) {	
				String str = matcher.group(1).replaceAll("<!\\[CDATA\\[", "").replaceAll("\\]\\]>", "");
				str = str.replaceAll("\\&[a-zA-Z]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "");
				Log.e("OnlineMusic", "content 1: " + matcher.group(1));
				Log.e("OnlineMusic", "content 2: " + str);
				mContent = str;
			}				
			*/
			
        } catch (IOException e) {
	        ShowToastMessage("Network can not connect, please try again.");
        	e.printStackTrace();
		}

	}

    public static String splitAndFilterString(String input, int length) {  
        if (input == null || input.trim().equals("")) {  
            return "";  
        }  
        // 去掉所有html元素,  
        String str = input.replaceAll("\\&[a-zA-Z]{1,10};", "").replaceAll(  
                "<[^>]*>", "");  
        str = str.replaceAll("[(/>)<]", "");  
        int len = str.length();  
        if (len <= length) {  
            return str;  
        } else {  
            str = str.substring(0, length);  
            str += "......";  
        }  
        return str;  
    }  
	
	private void ShowToastMessage(final String message) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(Artist.this, message, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	private void updatArtistInfo() {
		this.runOnUiThread(new Runnable() {
			public void run() {			
				try{
					
					findViewById(R.id.center_processbar).setVisibility(View.GONE);
					mImage.setImageBitmap(mArtistImage);
					mTopTracksList.setVisibility(View.VISIBLE);
				}catch(Exception e) {
					e.printStackTrace();
				} 
			}
		});
			
	}

	private void getArtistTopTracks(String rurl) {
		String httpresponse  = NetUtils.loadString(rurl);
		if(httpresponse == null || httpresponse.length() == 0)
			return;
		
		Pattern pattern = Pattern.compile("<name>([\\s\\S]*?)</name>");
		Matcher matcher = pattern.matcher(httpresponse);
		int rank = 1;
		while(matcher.find()) {		 
			if(!matcher.group(1).toUpperCase().equals(mArtistName.toUpperCase())){
				mTopTracksStrings.add(rank + ". " + matcher.group(1));
				mTopTracks.add( matcher.group(1));
				rank++;
			}
		}		
	}
	private void updatArtistTopTracks() {
		this.runOnUiThread(new Runnable() {
			public void run() {			
				try{
					mTopTracksAdapter.notifyDataSetChanged();
					
				}catch(Exception e) {
					e.printStackTrace();
				} 
			}
		});
			
	}



	
}
