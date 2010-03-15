package com.trans.music.search;

import java.util.List;

import com.feebe.lib.BaseList;
import com.feebe.lib.DbAdapter;
import com.feebe.lib.EndlessUrlArrayAdapter;
import com.feebe.lib.Util;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;

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
    startSearch(null, true, null, false);
    return true;
  }    

  private String getUrlFromIntent(final Intent intent) {
    String url = null;
    final String action = intent.getAction();
    if (Intent.ACTION_SEARCH.equals(action)) {
      url = intent.getStringExtra(SearchManager.QUERY);
      Log.e("in search list", url);
      Const.dbAdapter.intsertHistory(url, DbAdapter.TYPE_SEARCH);
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
