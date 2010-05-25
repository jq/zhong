package com.feebe.rings;

import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import com.feebe.lib.Util;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

public class Search {
  private final static String TAG = "Search";
  public final static String keyIDUrl = "http://ggapp.appspot.com/ringtone/show/";
  
  public static void getCate(String cate) {
    String url = Const.SearchBase + "category=" + URLEncoder.encode(cate);
    startSearchList(url, Const.OneWeek);
  }
  
  public static void getArtistRing(String artist) {
    String url = Const.SearchBase + "artist=" + URLEncoder.encode(artist);
    startSearchList(url, Const.OneWeek);
  }
   
  public static void getArtistAndTitle(String artist, String title) {
    String url = Const.SearchBase + "artist=" + URLEncoder.encode(artist) +"&q=" +URLEncoder.encode(title);
    startSearchList(url, Const.OneWeek);
  }
  
  public static void getTitleRing(String key) {
    String url = Const.SearchBase + "&q=" +URLEncoder.encode(key);
    startSearchList(url, Const.OneWeek);
  }

  public static void getAuthorRing(String key) {
    String url = Const.SearchBase + "&author=" +URLEncoder.encode(key);
    startSearchList(url, Const.OneWeek);
  }

  public static void startSearchList(String url, long expire) {
    Log.e("url", url);
    Intent intent = new Intent();
    intent.putExtra(Const.searchurl, url);
    intent.putExtra(Const.expire, expire);
    intent.setClass(Const.main, SearchList.class);
    Const.main.startActivity(intent);
  }
    
  public static String getRingUrl(String key) {
  	return keyIDUrl + key + "?json=1";
  }
  public static void startRing(String key) {
	  startRing(Const.main, key);
  }
  public static void startRing(Activity at, String key) {
    Intent intent = new Intent();
    intent.setClass(at, RingActivity.class);
    intent.putExtra(Const.searchurl, key);
    at.startActivity(intent);
  }
  
  public static JSONObject getRingJson(String src) {
    JSONObject r = null;

  	boolean fromNet = src.startsWith("http");
  	if (fromNet) {
  		r = Util.getJsonFromUrl(src, Const.OneMonth);
  		if (r!= null)
	      try {
	        r.put(Const.key, src);
        } catch (JSONException e) {
        }
  	} else {
  		String ring = Util.readFile(src);
      if (ring != null) {
        try {
          r = new JSONObject(ring);
        }catch (JSONException e) {
  		  }
      }
  	}
  	// no key in http://ggapp.appspot.com/ringtone/show/agVnZ2FwcHIYCxIRcmluZ3RvbmVfcmluZ3RvbmUY9RMM?json=1
    return r;
  }
  
  public static String getSearchKeyUrl(String key) {
    return Const.SearchQuery + URLEncoder.encode(key);
  }
  
/*  
  private static void searchKeyword(String key) {
  	searchUrl(getSearchKeyUrl(key));
  }

  @Override
  public void run() {
    //// Log.e(TAG, "enter run");

    String data = popup.download(url, Const.OneDay);
    if (data != null) {
      try {
        JSONArray entries;
        entries = new JSONArray(data);
        int len = entries.length();
        if (len == 1) {
          String key = entries.getJSONObject(0).getString(Const.key);
          SearchKey(key);
        } else if (len > 1){
          //// Log.e(TAG, data);
          Intent intent = new Intent();
          intent.putExtra(Const.searchurl, url);
          intent.setClass(Ring.main, SearchList.class);
          Ring.obj = entries;
          Ring.main.startActivity(intent);
        } else {
          Toast.makeText(Ring.main, R.string.no_result,Toast.LENGTH_SHORT).show();
        }
      } catch (JSONException e) {
        // Log.e(TAG, e.getMessage());
      }
      
    } else {
      // Log.e(TAG, url);
    }
    
  }
*/  
}
