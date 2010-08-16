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

import com.ringtone.search1.R;

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
	
	boolean hasInfo = true;
	boolean hasTracks = true;
	
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
					intent.putExtra(Const.Key, mTopTracks.get(position - 2));
	        intent.setClass(Artist.this, SearchList.class);
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
    try {
			Log.e("OnlineMusic", "searchFromNetwork mRequestUrl: " + rurl);
			
			String httpresponse = null;
			httpresponse = Util.downloadAndCache(rurl, Const.OneWeek);
			if (httpresponse==null || httpresponse.length()==0) {
				hasInfo = false;
				return;
			}

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
    } catch (IOException e) {
    		hasInfo = false;
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
					Log.e("mImage==null? ", ""+(mImage==null));
					mTopTracksList.setVisibility(View.VISIBLE);
					if (!hasInfo && !hasTracks) {
						Toast.makeText(Artist.this, R.string.no_result, Toast.LENGTH_SHORT).show();
						finish();
					}
				}catch(Exception e) {
					e.printStackTrace();
				} 
			}
		});
			
	}

	private void getArtistTopTracks(String rurl) {
		String httpresponse  = null;
    boolean inCache = Util.inCache(rurl, Const.OneWeek);

		if (inCache) {
			httpresponse = Util.readFile(Const.cachedir+Util.getHashcode(rurl));
		} else {
			httpresponse = NetUtils.loadString(rurl);
		}
		
		if(httpresponse == null || httpresponse.length() == 0) {
			hasTracks = false;
			return;
		}
		
		Pattern pattern = Pattern.compile("<name>([\\s\\S]*?)</name>");
		Matcher matcher = pattern.matcher(httpresponse);
		int rank = 1;
		while(matcher.find()) {		 
			if(!matcher.group(1).toUpperCase().equals(mArtistName.toUpperCase())){
				final String trackItem = rank + ". " + matcher.group(1);
				this.runOnUiThread(new Runnable() {
					public void run() {			
						try{
							mTopTracksStrings.add(trackItem);
							mTopTracksAdapter.notifyDataSetChanged();
						}catch(Exception e) {
							
						} 
					}
				});
				mTopTracks.add( matcher.group(1));
				rank++;
			}
		}
		if (rank > 1 && !inCache) {
      Util.saveFileInThread(httpresponse, Const.cachedir+Util.getHashcode(rurl));
		}
	}
	private void updatArtistTopTracks() {
		this.runOnUiThread(new Runnable() {
			public void run() {			
				try{
					findViewById(R.id.center_processbar).setVisibility(View.GONE);
					mTopTracksList.setVisibility(View.VISIBLE);
					mTopTracksAdapter.notifyDataSetChanged();
					if (!hasInfo && !hasTracks) {
						Toast.makeText(Artist.this, R.string.no_result, Toast.LENGTH_SHORT).show();
						finish();
					}
				}catch(Exception e) {
					
				} 
			}
		});
			
	}



	
}

