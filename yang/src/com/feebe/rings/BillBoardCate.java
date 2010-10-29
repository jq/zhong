package com.feebe.rings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;

import com.feebe.lib.StringListBase;
import com.feebe.lib.Util;

public class BillBoardCate extends StringListBase {
  
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

  @Override
  public ListAdapter getAdapter() {
    mAdapter = new StringAdapter(this, android.R.layout.simple_list_item_1, 
        null, 
        0,
        "");
    for(int i = 0; i < 10; i++) {
      mAdapter.add(billboardString[i]);
    }

    return mAdapter;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
    Intent intent = new Intent(BillBoardCate.this, BillBoardList.class);
    intent.putExtra("url", billboardUrl[pos]);
    startActivity(intent);

  }

}
