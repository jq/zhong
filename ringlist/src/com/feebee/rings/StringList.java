package com.feebee.rings;

import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.feebe.lib.BaseList;
import com.feebe.lib.UrlArrayAdapter;

import android.content.Context;
import android.content.Intent;
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
  private static final int ARTIST_LIB_TYPE = 4;
  private static final int BBHOTCHART_TYPE = 5;
  
  @Override
  public ListAdapter getAdapter() {
    mAdapter = new StringAdapter(this, android.R.layout.simple_list_item_1);
    return mAdapter;
  }
  
  private static final String[] BbHotChartStr = {
    "yahootop", "bhot100","bhiphop", "bcountry", "bmodernrock", "bdanceclub","brap","bpop","bmainrock"
  };

  @Override
  public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
  	Item cate = mAdapter.getItem(pos);
    Search s;
    if (cate.type == ARTIST_LIB_TYPE) {
        Intent intent = new Intent();
        intent.setClass(this, SingerLibrary.class);
        startActivityForResult(intent, 1);
    } else if (cate.type == BBHOTCHART_TYPE) {
        Intent intent = new Intent();
        intent.putExtra("type", BbHotChartStr[pos - 1]);
        intent.setClass(this, BbHotChart.class);
        startActivityForResult(intent, 1);
    } else if (cate.type == CATE_TYPE) {
    	Search.getCate(this, cate.name);
    } else if (cate.type == ARTIST_TYPE) {
    	Search.getArtistRing(this, cate.name);
    } else if (cate.type == Latest_TYPE){
        Search.startSearchList(this, LATEST, 0);
    } else {
    	Search.startSearchList(this, TOPDOWNLOAD, Const.OneDay);
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
      add(new Item("Artist Library", ARTIST_LIB_TYPE));
      add(new Item("Yahoo! Music Top Songs", BBHOTCHART_TYPE));
      add(new Item("Billboard Top 100", BBHOTCHART_TYPE));
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
      runAsyn(base_url, Const.OneWeek);
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
