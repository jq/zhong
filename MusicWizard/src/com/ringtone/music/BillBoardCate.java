package com.ringtone.music;

import java.util.ArrayList;

import com.ringtone.music.download.DownloadActivity;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class BillBoardCate extends ListActivity implements OnItemClickListener {
  
  String[] billboardString = {"Hot 100 Singles","200 Albums","Hot RnB HipHop Songs Singles",
      "Country Songs Singles","Modern Rock Tracks Singles","Dance Club Play Singles",
      "Hot Rap Tracks Singles","Pop 100 Singles","Hot Mainstream Rock Tracks Singles",
      "Hot Adult Top40 Tracks Singles", "UK Billboard", "Japan Billboard",
      "China Top New Songs", "China Top Songs", "Japan and Korea Top Songs"};
  
  String[] billboardUrl = {"http://music-chart.appspot.com/chart/billboard_hot_100_singles",
      "http://music-chart.appspot.com/chart/billboard_200_albums",
      "http://music-chart.appspot.com/chart/billboard_hot_rnb_hip_hop_songs_singles",
      "http://music-chart.appspot.com/chart/billboard_country_songs_singles",
      "http://music-chart.appspot.com/chart/billboard_modern_rock_tracks_singles",
      "http://music-chart.appspot.com/chart/billboard_dance_club_play_singles",
      "http://music-chart.appspot.com/chart/billboard_hot_rap_tracks_singles",
      "http://music-chart.appspot.com/chart/billboard_pop_100_singles",
      "http://music-chart.appspot.com/chart/billboard_hot_mainstream_rock_tracks_singles",
      "http://music-chart.appspot.com/chart/billboard_hot_adult_top_40_tracks_singles",
      "http://music-chart.appspot.com/chart/billboard_uk",
      "http://music-chart.appspot.com/chart/billboard_japan",
      "http://music-chart.appspot.com/chart/billboard_china_new",
      "http://music-chart.appspot.com/chart/billboard_china_top",
      "http://music-chart.appspot.com/chart/billboard_j_and_k"
      };
  
  private ArrayList<String> mItems = new ArrayList<String>();
  private CatAdapter mCatAdapter;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.billboard_cat_list);
      Utils.addAds(this);
      
      int numBoards = Math.min(billboardString.length, billboardUrl.length);
      
      for (int i = 0; i < numBoards; i++) {
    	  mItems.add(billboardString[i]);
      }
      
      mCatAdapter = new CatAdapter(BillBoardCate.this,R.layout.billboard_cat_item);
      
      final ListView list = getListView();
      setListAdapter(mCatAdapter);
      list.setDividerHeight(1);
      list.setFocusable(true);
      list.setTextFilterEnabled(true);
      list.setOnItemClickListener(this);
  }
  
  @Override
  public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
    Intent intent = new Intent(BillBoardCate.this, BillBoardList.class);
    intent.putExtra("url", billboardUrl[pos]);
    startActivity(intent);
  }
  
private final class CatAdapter extends BaseAdapter {
	private int mResource;
	private LayoutInflater mInflater;

	public CatAdapter(Context context, int resource) {
		mResource = resource;
        mInflater = (LayoutInflater)context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mItems.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return mItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		View v;
		String item = mItems.get(position);
		
		if (convertView == null) {
			v = mInflater.inflate(mResource, parent, false);
		} else {
			v = convertView;
		}

		((TextView) v.findViewById(R.id.cat_name)).setText(item);

		return v;
	}
	  
  }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.billboard_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.bill_dowloads:
	        Intent intent = new Intent(BillBoardCate.this, DownloadActivity.class);
			startActivity(intent);
			return true;

		}
	
		return false;
	}
}