package com.feebe.RingTone;

import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.feebe.lib.BaseList;
import com.feebe.lib.UrlArrayAdapter;


import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class StringList extends BaseList {
  private static final String base_url = "http://ggapp.appspot.com/category/list/?json=1";
  private static final String TOPDOWNLOAD = "http://ggapp.appspot.com/ringtone/hot/topdl/";
  private static final String LATEST = "http://ggapp.appspot.com/ringtone/hot/newest/";
  private static final int CATE_TYPE = 0;
  private static final int ARTIST_TYPE = 1;
  private static final int TOP_TYPE = 2;
  private static final int Latest_TYPE = 3;
  
  @Override
  public ListAdapter getAdapter() {
    mAdapter = new StringAdapter(this, android.R.layout.simple_list_item_1);
    return mAdapter;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
  	Item cate = mAdapter.getItem(pos);
    Search s;
    if (cate.type == CATE_TYPE) {
    	Search.getCate(cate.name);
    } else if (cate.type == ARTIST_TYPE) {
    	Search.getArtistRing(cate.name);
    } else if (cate.type == Latest_TYPE){
      Search.startSearchList(LATEST, 0);
    } else {
    	Search.startSearchList(TOPDOWNLOAD, Const.OneDay);
    }
  }
  
  public static class Item {
  	Item(String n, int t) {
  		name = n;
  		type = t;
  	}
  	String name;
  	int type;
  };
    
  public class StringAdapter extends UrlArrayAdapter<Item, TextView> {

    public StringAdapter(Context context, int resource) {
      super(context, resource);
      add(new Item("Top download", TOP_TYPE));
      add(new Item("Latest Rings", Latest_TYPE));

      // add artist
      SharedPreferences s = getSharedPreferences(Const.artist, 0);
      Map<String, ?> layers = s.getAll();

      if (layers.size() > 0) {
	      for (String id : layers.keySet()) {
	        //String artist = s.getString(id, null);
	        this.add(new Item(id, ARTIST_TYPE));
	      }
      }
      runSyn(base_url, Const.OneWeek);
    }

    @Override
    public Item getT(Object o) {
      try {
        JSONObject obj = (JSONObject) o;
        String name = obj.getString("name");
        if (name != null && name.length() > 1) {
        	return new Item(name, CATE_TYPE);
        }
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return null;
    }

		@Override
    public TextView getWrapper(View v) {
		  TextView t = (TextView)v.findViewById(android.R.id.text1);
		  v.setTag(t);
	    return t;
    }

		@Override
    public void applyWrapper(Item item, TextView wp, boolean newView) {
      wp.setText(item.name);
    }
    @Override
    protected List getListFromUrl(String url, long expire) {
      return RingUtil.getJsonArrayFromUrl(url, expire);
    }
  }
  private StringAdapter mAdapter;
}