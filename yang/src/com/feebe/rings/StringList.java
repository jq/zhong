package com.feebe.rings;

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
  private static final int CATE_TYPE = 0;
  private static final int ARTIST_TYPE = 1;
  private static final int TOP_TYPE = 2;
  
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
    } else {
    	Search.startSearchList(Const.TOPDOWNLOAD);
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
      // add artist
      SharedPreferences s = getSharedPreferences(Const.artist, 0);
      Map<String, ?> layers = s.getAll();

      if (layers.size() > 0) {
	      for (String id : layers.keySet()) {
	        //String artist = s.getString(id, null);
	        this.add(new Item(id, ARTIST_TYPE));
	      }
      }
      runSyn(base_url, Const.OneMonth);
    }

    @Override
    public Item getT(JSONObject obj) {
      try {
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
  }
  private StringAdapter mAdapter;
}
