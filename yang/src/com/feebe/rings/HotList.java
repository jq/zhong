package com.feebe.rings;

import org.json.JSONException;
import org.json.JSONObject;

import com.feebe.lib.BaseList;
import com.feebe.lib.ImgThread;
import com.feebe.lib.UrlArrayAdapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

public class HotList extends BaseList {
  //private final static String TAG = "HotList";
  private static final String base_url = "http://ggapp.appspot.com/ringtone/hot/bb100/";

	@Override
	public ListAdapter getAdapter() {
		new ImgThread(getListView());
		mAdapter = new HotAdapter(this, R.layout.searchlist_row);
		return mAdapter;
  }

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
	  HotSong h = mAdapter.getItem(pos);
	  Search.getArtistAndTitle(h.artist, h.title);
	}
	
	public static class HotSong {
		String title;
		String artist;
		String image;
		
		//String rating;
		public HotSong(String title, String artist) {
			super();
			this.title = title;
			this.artist = artist;
		}
	}
	
	public class HotAdapter extends UrlArrayAdapter<HotSong, SearchViewWp> {
		public HotAdapter(Context context, int resource) {
			super(context, resource);
			runAsyn(base_url, Const.OneWeek);
		}
				
    @Override
    public HotSong getT(JSONObject obj) {
      try {
        String song = obj.getString(Const.song);
        String artist = obj.getString(Const.artist);
        if (song != null && artist != null) {
          HotSong hotsong = new HotSong(song, artist);
          try{
          	hotsong.image = obj.getString(Const.image);
          } catch (Exception e) {
          	hotsong.image = null;
					}         
         // hotsong.rating = obj.getString(Const.rating);
          return hotsong;
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
    public void applyWrapper(HotSong item, SearchViewWp wp, boolean newView) {
      if (item.image != null && item.image.length() > 0) {
      	wp.setUrl(item.image);
      	if (newView) {
      		wp.download();
      	}
      } else {
      	wp.setUrl(null);
      }
      wp.name.setText(item.title);
      wp.artist.setText(item.artist);
    }
	}
	
	private HotAdapter mAdapter;

}
