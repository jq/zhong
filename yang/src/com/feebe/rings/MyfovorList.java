package com.feebe.rings;

import java.util.Map;

import android.content.SharedPreferences;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;

import com.feebe.lib.StringListBase;
import com.feebe.lib.StringListBase.StringAdapter;
import com.feebe.rings.StringList.Item;

public class MyfovorList extends StringListBase {

  @Override
  public ListAdapter getAdapter() {
    mAdapter = new StringAdapter(this, android.R.layout.simple_list_item_1, 
        null, 
        0,
        "");
    // add artist
    SharedPreferences s = getSharedPreferences(Const.artist, 0);
    Map<String, ?> layers = s.getAll();

    if (layers.size() > 0) {
      for (String id : layers.keySet()) {
        //String artist = s.getString(id, null);
        mAdapter.add(id);
      }
    }

    return mAdapter;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
    String artist = mAdapter.getItem(pos);
    Search.getArtistRing(this, artist);

  }

}
