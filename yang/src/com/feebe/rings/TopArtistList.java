package com.feebe.rings;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;

import com.feebe.lib.StringListBase;

public class TopArtistList extends StringListBase {

  @Override
  public ListAdapter getAdapter() {
    mAdapter = new StringAdapter(this, android.R.layout.simple_list_item_1, 
        "http://ggapp.appspot.com/ringtone/topartist/", 
        0,//Const.OneMonth, 
        "artist"
        );
    return mAdapter;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
    String name = mAdapter.getItem(pos);
    Search.getArtistRing(this, name);
  }

}
