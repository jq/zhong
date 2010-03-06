package com.trans.music.search;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  // pn= start number
  public final static int DEFAULT_RESULT = 15;
  //private final static String Search_Url = "http://221.195.40.183/m?f=ms&tn=baidump3&ct=134217728&rn=15&lm=0&word=";
  private final static String Search_Url = "http://mp3.sogou.com/music.so?pf=&as=&st=&ac=1&w=02009900&query=";

  @Override
  public ListAdapter getAdapter() {
    Intent i = this.getIntent();
    reloadUrl = getUrlFromIntent(i);
    long expire = i.getLongExtra(Const.expire, 0);

    mAdapter = new SearchResultAdapter(this, R.layout.searchlist_row, expire);
    
    return mAdapter;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
    MP3Info ring = mAdapter.getItem(pos);
    
  }
   
  @Override
  public boolean onSearchRequested() {
    startSearch(this.getString(R.string.search_hint), true, null, false);
    return true;
  }    

  private String getUrlFromIntent(final Intent intent) {
    String url = null;
    final String action = intent.getAction();
    if (Intent.ACTION_SEARCH.equals(action)) {
      url = intent.getStringExtra(SearchManager.QUERY);
      // TODO: save keyword to db
    } else if (Intent.ACTION_VIEW.equals(action)){
      url = intent.getDataString();
    } else {
    	url = intent.getStringExtra(Const.Key);
    }
    url = Search_Url + url;
    return url;
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
  
  public class SearchResultAdapter extends EndlessUrlArrayAdapter<MP3Info, SearchViewWp> {
    public SearchResultAdapter(Context context, int resource, long expire) {
      super(context, resource, expire);
      reset();
    }
    public void reset() {
      lastCnt = 0;
      if (Util.inCache(reloadUrl, Const.OneWeek)) {
                keepOnAppending = false;

        runSyn(reloadUrl, Const.OneWeek);
        finishLoading();
      }
    }
		@Override
    public SearchViewWp getWrapper(View v) {
	    return new SearchViewWp(v);
    }
		@Override
  public void applyWrapper(MP3Info item, SearchViewWp w, boolean newView) {
      if (item.getName() != null) {
        w.name.setText(item.getName());
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
    @Override
    protected List getListFromUrl(String url, long expire) {
      //return MusicUtil.getBiduMp3(url);
    	// for test sogo
    	return MusicUtil.getSogoMp3(url);
    }
    @Override
    protected MP3Info getT(Object obj) {
      return (MP3Info)obj;
    }
  }

  private int lastCnt;
  private SearchResultAdapter mAdapter;
}
