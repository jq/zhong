package com.feebe.rings;

import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import com.feebe.lib.Util;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class Search {
  private final static String TAG = "Search";
  public final static String keyIDUrl = "http://ggapp.appspot.com/ringtone/show/";
  
  public static void getCate(Activity act, String cate) {
    String url = Const.SearchBase + "category=" + URLEncoder.encode(cate);
    startSearchList(act, url, Const.OneWeek);
  }
  
  public static void getCateByOrder(Activity act, String cate, String order) {
    String url = Const.SearchBase + "category=" + URLEncoder.encode(cate) +
      "&order=" + order;
    Log.e("order", url);

    startSearchList(act, url, Const.OneWeek, false);
  }
 
  public static void getArtistRing(Activity act, String artist) {
    String url = Const.SearchBase + "artist=" + URLEncoder.encode(artist);
    startSearchList(act, url, Const.OneWeek);
  }
   
  public static void getArtistAndTitle(Activity act, String artist, String title) {
    String url = Const.SearchBase + "artist=" + URLEncoder.encode(artist) +"&q=" +URLEncoder.encode(title);
    startSearchList(act, url, Const.OneWeek);
  }
  
  public static void getTitleRing(Activity act, String key) {
    String url = Const.SearchBase + "&q=" +URLEncoder.encode(key);
    startSearchList(act, url, Const.OneWeek);
  }

  public static void getAuthorRing(Activity act, String key) {
    String url = Const.SearchBase + "&author=" +URLEncoder.encode(key);
    startSearchList(act, url, Const.OneWeek);
  }

  public static void startSearchList(Activity act, String url, long expire) {
    Intent intent = new Intent();
    intent.putExtra(Const.searchurl, url);
    intent.putExtra(Const.expire, expire);
    intent.setClass(act, SearchList.class);
    act.startActivity(intent);
  }
  
  public static void startSearchList(Activity act, String url, long expire, boolean dedup) {
    // Log.e("url", url);
    Intent intent = new Intent();
    intent.putExtra(Const.searchurl, url);
    intent.putExtra(Const.expire, expire);
    intent.putExtra(Const.USEDEDUP, dedup);
    intent.setClass(act, SearchList.class);
    act.startActivity(intent);
   
  }
    
  public static String getRingUrl(String key) {
  	return keyIDUrl + key + "?json=1";
  }
  public static void startRing(Activity at, String key) {
	if (key == null) {
		Toast.makeText(at, R.string.no_result,Toast.LENGTH_SHORT).show();
		return;
	}
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
          // Log.e("getRingJson", "put key error");
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
    //// // // Log.e(TAG, "enter run");

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
          //// // // Log.e(TAG, data);
          Intent intent = new Intent();
          intent.putExtra(Const.searchurl, url);
          intent.setClass(Ring.main, SearchList.class);
          Ring.obj = entries;
          Ring.main.startActivity(intent);
        } else {
          Toast.makeText(Ring.main, R.string.no_result,Toast.LENGTH_SHORT).show();
        }
      } catch (JSONException e) {
        // // // Log.e(TAG, e.getMessage());
      }
      
    } else {
      // // // Log.e(TAG, url);
    }
    
  }
*/  
}
