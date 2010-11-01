package com.ringtone.music;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
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
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      requestWindowFeature(Window.FEATURE_NO_TITLE);
      setContentView(R.layout.list); 
      final ListView list = getListView();
      setListAdapter(getAdapter());
      list.setDividerHeight(1);
      list.setFocusable(true);
      // list.setOnCreateContextMenuListener(this);
      list.setTextFilterEnabled(true);
      list.setOnItemClickListener(this);
  }
  
  public ListAdapter getAdapter() {
      ArrayList<String> items = new ArrayList<String>();
      for(int i = 0; i < 10; i++) {
          items.add(billboardString[i]);
      }
      mAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,items);
      return mAdapter;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
    Intent intent = new Intent(BillBoardCate.this, BillBoardList.class);
    intent.putExtra("url", billboardUrl[pos]);
    startActivity(intent);

  }

}
