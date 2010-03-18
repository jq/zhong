package com.trans.music.search;

import java.io.IOException;
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
import android.widget.Toast;

public class SearchList extends BaseList {
  private final static String TAG = "SearchList";
  public final static int DEFAULT_RESULT = 30;

  @Override
  public ListAdapter getAdapter() {
    Intent i = this.getIntent();
    reloadUrl = getKeywordFromIntent(i);
    long expire = i.getLongExtra(Const.expire, 0);

    mAdapter = new SearchResultAdapter(this, R.layout.searchlist_row, expire);
    
    return mAdapter;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
    MP3Info mp3 = mAdapter.getItem(pos);
    Intent intent = new Intent(this,MusicPage.class);
    // intent.putExtra("MP3LOC", mMp3Local);
    String mp3Link = "";
	try {
	  mp3Link = MusicUtil.getLink(mp3.getLink());
	} catch (IOException e) {
	  // TODO Auto-generated catch block
	  Toast.makeText(this, R.string.no_result, Toast.LENGTH_SHORT).show();
	  return;
	}
	float rate = (float)((Double.parseDouble(mp3.rate)/6.0)*5.0);
	Log.e("rate_ori:", mp3.rate);
	intent.putExtra("MP3RATE", ((Float)rate).toString());
    intent.putExtra("MP3LOC", mp3Link);
    intent.putExtra("MP3TITLE", mp3.name);
    intent.putExtra("MP3SONGER", mp3.artist);
    startActivity(intent);
    startActivity(intent);
  }
   
  @Override
  public boolean onSearchRequested() {
    startSearch(null, true, null, false);
    return true;
  }    

  private String getKeywordFromIntent(final Intent intent) {
    String keyword = null;
    final String action = intent.getAction();
    if (Intent.ACTION_SEARCH.equals(action)) {
      keyword = intent.getStringExtra(SearchManager.QUERY);
      Const.dbAdapter.intsertHistory(keyword, DbAdapter.TYPE_SEARCH);
    } else if (Intent.ACTION_VIEW.equals(action)){
      keyword = intent.getDataString();
    } else {
      keyword = intent.getStringExtra(Const.Key);
    }
    if (keyword != null) {
      return MusicUtil.getSogouLinks(keyword);
    }
    return null;
  }
  
  // this should not be null
  private String reloadUrl;
  
  @Override
  public void onNewIntent(final Intent intent) {
    super.onNewIntent(intent);
    reloadUrl = getKeywordFromIntent(intent);
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
      if (reloadUrl != null && Util.inCache(reloadUrl, Const.OneWeek)) {
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
      int page = pos / DEFAULT_RESULT + 1;
      String url = MusicUtil.getSogouLinks(reloadUrl, page);
      return url;
    }
    @Override
    protected void finishLoading() {
      if (lastCnt + DEFAULT_RESULT > super.getCount()) {
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
    	return MusicUtil.getSogoMp3(url, -1);
    }
    @Override
    protected MP3Info getT(Object obj) {
      return (MP3Info)obj;
    }
  }

  private int lastCnt;
  private SearchResultAdapter mAdapter;
}
