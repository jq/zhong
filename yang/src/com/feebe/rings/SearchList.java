package com.feebe.rings;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.feebe.lib.BaseList;
import com.feebe.lib.EndlessUrlArrayAdapter;
import com.feebe.lib.ImgThread;
import com.feebe.lib.UrlArrayAdapter;
import com.feebe.lib.Util;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.provider.SearchRecentSuggestions;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;

public class SearchList extends BaseList {
  private final static String TAG = "SearchList";

  @Override
  public ListAdapter getAdapter() {
  	new ImgThread(getListView());
    Intent i = this.getIntent();
    reloadUrl = getUrlFromIntent(i);
    long expire = i.getLongExtra(Const.expire, 0);
    mAdapter = new SearchResultAdapter(this, R.layout.searchlist_row, expire);
    return mAdapter;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
    SearchResult ring = mAdapter.getItem(pos);
    Search.startRing(Search.getRingUrl(ring.key));
  }
  
  public static class SearchResult{
    String title;
    String artist;
    String key;
    String image;
    String rating;
    
    public SearchResult(String title, String artist, String key, String image,
  			String rating) {
  		super();
  		this.title = title;
  		this.artist = artist;
  		this.key = key;
  		this.image = image;
  		this.rating = rating;
    }
  };
  
  @Override
  public boolean onSearchRequested() {
    startSearch(this.getString(R.string.search_hint), true, null, false);
    return true;
  }    

  private String getUrlFromIntent(final Intent intent) {
    String keyword = null;
    final String action = intent.getAction();
    if (Intent.ACTION_SEARCH.equals(action)) {
      keyword = Search.getSearchKeyUrl(intent.getStringExtra(SearchManager.QUERY));
      // TODO: save keyword to db
    } else if (Intent.ACTION_VIEW.equals(action)){
      // Get from suggestions
      keyword = Search.getSearchKeyUrl(intent.getDataString());
    } else {
    	keyword = intent.getStringExtra(Const.searchurl);
    }
    return keyword;
  }
  
  // this should not be null
  private String reloadUrl;
  
  @Override
  public void onNewIntent(final Intent intent) {
    super.onNewIntent(intent);
    reloadUrl = getUrlFromIntent(intent);
    if(reloadUrl == null)
      return;
    else {
      mAdapter.clear();
      mAdapter.reset();
    }
  }
  
  public class SearchResultAdapter extends EndlessUrlArrayAdapter<SearchResult, SearchViewWp> {
    public SearchResultAdapter(Context context, int resource, long expire) {
      super(context, resource, expire);
      reset();
    }
    public void reset() {
      lastCnt = 0;
      if (Util.inCache(reloadUrl, expire_)) {
        runSyn(reloadUrl, expire_);
        finishLoading();
      }
    }
    @Override
    public SearchResult getT(Object o) {
      try {
        JSONObject obj = (JSONObject) o;
        String title = obj.getString(Const.title);
        String artist = obj.getString(Const.artist);
        String key = obj.getString(Const.key);
        String image = obj.getString(Const.image);
        String rating = obj.getString(Const.rating);

        if (key != null && (title != null || artist != null )) {
          return new SearchResult(title, artist, key, image, rating);
        } 
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return null;
    }
		@Override
    public SearchViewWp getWrapper(View v) {
	    return new SearchViewWp(v);
    }
		@Override
  public void applyWrapper(SearchResult item, SearchViewWp w, boolean newView) {
      if (item.title != null) {
        w.name.setText(item.title);
    }
    if (item.artist != null) {
      w.artist.setText(item.artist);
    }
    if (item.rating != null) {
  	  int ratingNum = Integer.parseInt(item.rating);
  	  if (ratingNum < 60){
  	    w.rating.setRating(1);
  	  }else if (ratingNum < 70){
  	    w.rating.setRating(2);
  	  }else if (ratingNum < 80){
  	    w.rating.setRating(3);
  	  }else if (ratingNum < 90){
  	    w.rating.setRating(4);
  	  }else
  	    w.rating.setRating(5);   	  
    }
    if (item.image != null && item.image.length() > 0) {
    	w.setUrl("http://s.heiguge.com/" + item.image);
    	if (newView) {
        w.download();
    	}
    }
	    
    }
    
    @Override
    protected String getUrl(int pos) {
      if (pos == 0) {
        return reloadUrl;
      }
      lastCnt = pos;
      String url;
      if (reloadUrl.indexOf('?') != -1) {
        url = reloadUrl + "&start=" +lastCnt;
      } else {
        url = reloadUrl + "?start=" +lastCnt;
      }
      return url;
    }
    @Override
    protected void finishLoading() {
      Log.e("finishLoading", "cont " + super.getCount() + " last " + lastCnt);
      if(super.getCount() == 0) {
    	  new AlertDialog.Builder(SearchList.this)
    	  .setTitle("NotFound")
    	  .setPositiveButton("OK", new DialogInterface.OnClickListener() {
    		  public void onClick(DialogInterface dialog, int whichButton) {
    			  setResult(RESULT_OK);
    			  finish();
    			  SearchList.this.finish();
    		  }})
    	  .show();
    	  
    	  
      }
      
      if (lastCnt + Const.DEFAULT_RESULT > super.getCount()) {
        keepOnAppending = false;
      } else {
        fetchMoreResult();
      }
    }
    @Override
    protected List getListFromUrl(String url, long expire) {
      return RingUtil.getJsonArrayFromUrl(url, expire);
    }

  }

  private int lastCnt;
  private SearchResultAdapter mAdapter;
}
