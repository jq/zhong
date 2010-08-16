package com.feebe.rings;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.feebe.lib.AdListener;
import com.feebe.lib.BaseList;
import com.feebe.lib.DbAdapter;
import com.feebe.lib.EndlessUrlArrayAdapter;
import com.feebe.lib.ImgThread;
import com.feebe.lib.UrlArrayAdapter;
import com.feebe.lib.Util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.SearchRecentSuggestions;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class SearchList extends ListActivity implements OnItemClickListener {
  private final static String TAG = "SearchList";
	private static final int PROGRESS_DIALOG = 0;
  private int currentPage = 0;
  private ProgressDialog waitDialog;
  final Handler uiHandler = new Handler();
  @Override
  public void onCreate(Bundle savedInstanceState) {
	  Const.init(this);
	  //android.util.// Log.e("init", "" + Const.main != null);
	  
	  super.onCreate(savedInstanceState);
	  
	  requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.list); 
    AdListener.createAds(this);
    final ListView list = getListView();
    SearchListFooterView footer = new SearchListFooterView(this);
    list.addFooterView(footer);
    setListAdapter(getAdapter());
    list.setDividerHeight(1);
    list.setFocusable(true);
    // list.setOnCreateContextMenuListener(this);
    list.setTextFilterEnabled(true);
    list.setOnItemClickListener(this);
    
    ImageButton btnPre = footer.getBtnPre();
    ImageButton btnNext = footer.getBtnNext();
    btnPre.setOnClickListener(new OnClickListener() {	
			@Override
			public void onClick(View v) {
				if(currentPage <= 0)
					Toast.makeText(getApplicationContext(), "No pre page", Toast.LENGTH_LONG).show();
				else {
					currentPage--;
					mAdapter.clear();
					mAdapter.reset();
					mAdapter.notifyDataSetChanged();
				}
			}
		});
    btnNext.setOnClickListener(new OnClickListener() {		
			@Override
			public void onClick(View v) {
				currentPage++;
				try {
				  showDialog(PROGRESS_DIALOG);
				} catch (Exception e) {
					
				}
				new Thread(
					new Runnable() {
						public void run() {
							final List list = mAdapter.getListFromUrl(mAdapter.getUrl(currentPage), Const.OneWeek);
							if(list == null) {
								currentPage--;
								uiHandler.post(new Runnable() {
									@Override
									public void run() {
										Toast.makeText(getApplicationContext(), "No next page", Toast.LENGTH_LONG).show();
									}
								});
								
							} else {
								uiHandler.post(new Runnable() {							
									@Override
									public void run() {
										mAdapter.clear();
										mAdapter.runList(list);
										mAdapter.notifyDataSetChanged();
									}
								});
							}				
							waitDialog.dismiss();

						}
					}
				).start();
				
				
			}
		});
	 
  }  

public ListAdapter getAdapter() {
  	new ImgThread(getListView());
    Intent i = this.getIntent();
    reloadUrl = getUrlFromIntent(i);
    long expire = i.getLongExtra(Const.expire, 0);
    mAdapter = new SearchResultAdapter(this, R.layout.searchlist_row);
    return mAdapter;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
    SearchResult ring;
    try {
      ring = mAdapter.getItem(pos);
    } catch (Exception e) {
      return;
    }
    Search.startRing(this, Search.getRingUrl(ring.key));
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
    String url = null;
    final String action = intent.getAction();
    if (Intent.ACTION_SEARCH.equals(action)) {
      String key = intent.getStringExtra(SearchManager.QUERY);
      url = Search.getSearchKeyUrl(key);
      try {
    	  Const.dbAdapter.intsertHistory(key, DbAdapter.TYPE_SEARCH);
      } catch (Exception e) {
    	  Const.dbAdapter = new DbAdapter(this);
    	  Const.dbAdapter.intsertHistory(key, DbAdapter.TYPE_SEARCH);
      }
    } else if (Intent.ACTION_VIEW.equals(action)){
      // Get from suggestions
      url = Search.getSearchKeyUrl(intent.getDataString());
    } else {
      url = intent.getStringExtra(Const.searchurl);
    }
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
  
  public class SearchResultAdapter extends UrlArrayAdapter<SearchResult, SearchViewWp> {
    public SearchResultAdapter(Context context, int resource) {
      super(context, resource);
      reset();
    }
    public void reset() {      
    	runAsyn(getUrl(currentPage), Const.OneWeek);
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
    
    protected String getUrl(int page) {
      if (page == 0) {
        return reloadUrl;
      }
      String url;
      if (reloadUrl.indexOf('?') != -1) {
        url = reloadUrl + "&start=" +page*15;
      } else {
        url = reloadUrl + "?start=" +page*15;
      }
      return url;
    }

    @Override
    protected List getListFromUrl(String url, long expire) {
      return RingUtil.getJsonArrayFromUrl(url, expire);
    }

  }
  
  protected Dialog onCreateDialog(int id) { 
  	switch(id) {
  		case PROGRESS_DIALOG:
  			waitDialog = new ProgressDialog(SearchList.this);
  			waitDialog.setTitle("Please wait");
  			waitDialog.setMessage("Getting ringtone");
  			waitDialog.setIndeterminate(true);
  			return waitDialog;
  		default:
  			return null;
  	}
  }
 private SearchResultAdapter mAdapter;
}
