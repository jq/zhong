package com.trans.music.search;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.feebe.lib.BaseList;
import com.feebe.lib.EndlessUrlArrayAdapter;
import com.feebe.lib.ImgThread;
import com.feebe.lib.UrlArrayAdapter;
import com.feebe.lib.Util;

import android.app.SearchManager;
import android.content.Context;
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
    Intent i = this.getIntent();
    reloadUrl = getUrlFromIntent(i);
    mAdapter = new SearchResultAdapter(this, R.layout.searchlist_row);
    return mAdapter;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
    SearchResult ring = mAdapter.getItem(pos);
  }
  
  public static class SearchResult{
    String title;
    String artist;
    String size;
    String album;
    
    public SearchResult(String title, String artist, String size, String album) {
  		this.title = title;
  		this.artist = artist;
  		this.size = size;
  		this.album = album;
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
      //keyword = Search.getSearchKeyUrl(intent.getStringExtra(SearchManager.QUERY));
      // TODO: save keyword to db
    } else if (Intent.ACTION_VIEW.equals(action)){
      //keyword = Search.getSearchKeyUrl(intent.getDataString());
    } else {
    	//keyword = intent.getStringExtra(Const.searchurl);
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
    public SearchResultAdapter(Context context, int resource) {
      super(context, resource, Const.OneWeek);
      reset();
    }
    public void reset() {
      lastCnt = 0;
      /*
      if (Util.inCache(reloadUrl, Const.OneWeek)) {
                keepOnAppending = false;

        runSyn(reloadUrl, Const.OneWeek);
      }
      */
    }
    @Override
    public SearchResult getT(JSONObject obj) {
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
      if (lastCnt + 15 > super.getCount()) {
        //Log.e("finishLoading", "cont " + super.getCount() + " last " + lastCnt);
        keepOnAppending = false;
      } else {
        fetchMoreResult();
      }
    }
  }

  private int lastCnt;
  private SearchResultAdapter mAdapter;
}
