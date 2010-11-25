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
      "Hot Adult Top40 Tracks Singles"};
  
  String[] billboardUrl = {"http://music-chart.appspot.com/chart/billboard_hot_100_singles",
      "http://music-chart.appspot.com/chart/billboard_200_albums",
      "http://music-chart.appspot.com/chart/billboard_hot_rnb_hip_hop_songs_singles",
      "http://music-chart.appspot.com/chart/billboard_country_songs_singles",
      "http://music-chart.appspot.com/chart/billboard_modern_rock_tracks_singles",
      "http://music-chart.appspot.com/chart/billboard_dance_club_play_singles",
      "http://music-chart.appspot.com/chart/billboard_hot_rap_tracks_singles",
      "http://music-chart.appspot.com/chart/billboard_pop_100_singles",
      "http://music-chart.appspot.com/chart/billboard_hot_mainstream_rock_tracks_singles",
      "http://music-chart.appspot.com/chart/billboard_hot_adult_top_40_tracks_singles",};
  
  private ArrayAdapter<String> mAdapter;
  private ArrayList<String> mItems = new ArrayList<String>();
  private CatAdapter mCatAdapter;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
//      requestWindowFeature(Window.FEATURE_NO_TITLE);
      setContentView(R.layout.billboard_cat_list);
      Utils.addAds(this);
      
      for(int i = 0; i < 10; i++) {
    	  mItems.add(billboardString[i]);
      }
      
      mCatAdapter = new CatAdapter(BillBoardCate.this,R.layout.billboard_cat_item);
      
      final ListView list = getListView();
      setListAdapter(mCatAdapter);
      list.setDividerHeight(1);
      list.setFocusable(true);
      // list.setOnCreateContextMenuListener(this);
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
		inflater.inflate(R.menu.getmore_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.bill_dowloads:
	        Intent intent = new Intent(BillBoardCate.this, DownloadActivity.class);
			startActivity(intent);
			return true;
		
		case R.id.bill_getmore:
//			String url1 = "market://search?q=pub:\"Social Games\"";	
//			try {
//				Uri uri = Uri.parse(url1);
//				Intent intent1 = new Intent(Intent.ACTION_VIEW, uri);
//	    		intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				startActivity(intent1);
//			} catch (Exception ex) {
//				ex.printStackTrace();
//			}
			
			Intent intent1 = new Intent(BillBoardCate.this, SingerList.class);
			intent1.putExtra("type", "allsingers");
			startActivity(intent1);
			
			return true;
		}
	
		return false;
	}
}