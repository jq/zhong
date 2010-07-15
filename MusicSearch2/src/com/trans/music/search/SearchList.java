package com.trans.music.search;

import java.io.IOException;
import java.util.List;

import com.trans.music.search.R;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.Toast;

public class SearchList extends BaseList {
  private final static String TAG = "SearchList";
  public final static int DEFAULT_RESULT = 10;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
	  Const.init(this);
	  super.onCreate(savedInstanceState);
  }  
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
    MP3Info mp3;
    try {
      mp3 = mAdapter.getItem(pos);
    } catch (Exception e) {
      return;
    }
    new GetLinkTask().execute(mp3);
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
        // Log.e("incache", "cache?");
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
      if (item.getFSize() != null) {
      	w.size.setText(item.getFSize());
      }
      if (item.getAlbum() != null) {
      	w.album.setText(item.getAlbum());
      }
	    
    }
    
    @Override
    protected String getUrl(int pos) {
      if (pos == 0) {
        return reloadUrl;
      }
      lastCnt = pos;
      //int page = pos / DEFAULT_RESULT + 1;
      String url = MusicUtil.getSogouLinks(reloadUrl, ++currentPage);
      return url;
    }
    @Override
    protected void finishLoading() {
      // Log.e("finishLoading", "cont " + super.getCount() + " last " + lastCnt);
      if (super.getCount() == 0) {
    	  this.onNoResult();
    	  SearchList.this.finish();
      }
      
      if (lastCnt + DEFAULT_RESULT > super.getCount()) {
        keepOnAppending = false;
        notifyDataSetChanged();
      } else {
        fetchMoreResult();
      }
      lastCnt = super.getCount();
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
  
  private class GetLinkTask extends AsyncTask<MP3Info, Integer, Integer> {

	MP3Info mp3; 
	
	@Override
	protected Integer doInBackground(MP3Info... params) {
		MP3Info mp3 = params[0];
	    
	    String mp3Link = null;
	    if (!mp3.getLink().startsWith("http://")) {
	  	try {
	  	  mp3Link = MusicUtil.getLink(mp3.getLink());
	  	  mp3.link = mp3Link;
	  	  Log.e("mp3Link", mp3Link);
	  	} catch (IOException e) {
	  		Log.e("error", e.getMessage());
	  		return 0;
	  	}
	  	
	  	if (mp3Link == null) {
	      return 0;
	  	}
	  	this.mp3 = mp3;
		return 1;
	    } else {
	    	this.mp3 = mp3;
	    	return 1;
	    }
	}

	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
		getLinkProgressDialog.cancel();
		if (result == 0) {
			 Toast.makeText(SearchList.this, R.string.no_result, Toast.LENGTH_SHORT).show();
			 return;
		} else {
			Intent intent = new Intent(SearchList.this, MusicPage.class);
		    intent.putExtra(Const.MP3LOC, mp3.link);
		    intent.putExtra(Const.MP3TITLE, mp3.name);
		    intent.putExtra(Const.MP3SONGER, mp3.artist);
		    intent.putExtra(Const.MP3ALBM, mp3.album);
		    startActivity(intent);
		}
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		getLinkProgressDialog = new ProgressDialog(SearchList.this);
		getLinkProgressDialog.setMessage(SearchList.this.getString(R.string.get_download_link));
		getLinkProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		getLinkProgressDialog.setCancelable(true);
		getLinkProgressDialog.show();
	}
	  
  }

  private int lastCnt;
  private int currentPage = 1;
  private SearchResultAdapter mAdapter;
  private ProgressDialog getLinkProgressDialog;
}
