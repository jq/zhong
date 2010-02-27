package com.feebe.rings;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.feebe.lib.BaseList;
import com.feebe.lib.ImgThread;
import com.feebe.lib.UrlArrayAdapter;
import com.feebe.lib.Util;
import com.feebe.rings.HotList.HotSong;

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
  	new ImgThread(getListView());
    Intent i = this.getIntent();
    reloadUrl = getUrlFromIntent(i);
    
    mAdapter = new SearchResultAdapter(this, R.layout.searchlist_row);
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
      keyword = Search.getSearchKeyUrl(intent.getDataString());
    } else {
    	keyword = intent.getStringExtra(Const.searchurl);
    }
    return keyword;
  }
  
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
      mAdapter.notifyDataSetChanged();
    }
  }
  
  public class SearchResultAdapter extends UrlArrayAdapter<SearchResult, SearchViewWp> {
    private int mStatus;
    private final static int INIT = 0;
    private final static int LOADING = 1;
    private final static int NOMORE = 2;
    
    public SearchResultAdapter(Context context, int resource) {
      super(context, resource);
      reset();
    }
    public void reset() {
      lastCnt = 0;
      if(reloadUrl != null) {
        runAsyn(reloadUrl, Const.OneWeek);
      }
    }
    /*
    private View footview;
    private View getFooterView() {
    	if (footview == null) {
    		footview = mInflater.inflate(
	          R.layout.list_footer, null);
	      TextView text = (TextView)footview.findViewById(R.id.footer_text);
	      text.setText(R.string.footer);
	      footview.setVisibility(View.VISIBLE);
	      //view.bind(ListFooterView.Status.LOADING, this);
    	}
      return footview;    	
    }
*/
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (shouldLoadMore(position)) {
        // // Log.e("load", "more");
        fetchDataFromServer();
        mStatus = LOADING;
      }
      
      return super.getView(position, convertView, parent);
    }

    private boolean shouldLoadMore(int position) {
      return reloadUrl != null &&
          position == this.getCount() - 10 &&  // It is the last one.
          mStatus == INIT;
    }

    private void fetchDataFromServer() {
      lastCnt = super.getCount();
      String url;
      if (reloadUrl.indexOf('?') != -1) {
        url = reloadUrl + "&start=" +lastCnt;
      } else {
        url = reloadUrl + "?start=" +lastCnt;
      }
      
      // // Log.e("url", url);
      runAsyn(url, Const.OneWeek);
    }

    @Override
    public void notifyDataSetChanged() {
      // // Log.e("lastcnt", "" + lastCnt);
      if (lastCnt + Const.DEFAULT_RESULT > super.getCount()) {
      	onNoResult();
      } else {
        //SearchList.this.getListView().setSelection(lastCnt-1);
        mStatus = INIT;
      }
      super.notifyDataSetChanged();
    }
    
    @Override
    public SearchResult getT(JSONObject obj) {
      try {
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
    protected void onNoResult() {
    	/*
    	ListView list = getListView();
    	if (list.getFooterViewsCount() > 0) {
    		try {
    	    list.removeFooterView(getFooterView());
    		} catch (Exception e) {
    			
    		}
    	}*/
      mStatus = NOMORE;
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
    
  }

  private int lastCnt;
  private SearchResultAdapter mAdapter;
}
