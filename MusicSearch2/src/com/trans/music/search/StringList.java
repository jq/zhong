package com.trans.music.search;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;


public class StringList extends BaseList{
  private static final String[] mType_Animals = {
	  "My favourite",
      "Artist Library ",
      "Mobile Ringtones",
      "Yahoo! Music Top Songs",
      "Hot Top Songs",
      "Hip Hop / R&B Songs",
      "Country Songs",
      "Rock Songs",
      "Dance/Club Play Songs",
      "Rap Tracks",
      "Pop Songs",
      "Mainstream Rock Tracks"
  };

  @Override
  public ListAdapter getAdapter() {
    return new ArrayAdapter<String>(this,  android.R.layout.simple_list_item_1, mType_Animals);
   }

  private static final String[] BbHotChartStr = {
    "yahootop", "bhot100","bhiphop", "bcountry", "bmodernrock", "bdanceclub","brap","bpop","bmainrock"
  };
  @Override
  public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
	  	if (position == 0)  {
	  		Intent intent = new Intent();
	  		intent.setClass(StringList.this, Myfavourite.class);
	  		startActivityForResult(intent, 1);
	  	} else if (position == 1) {
			Intent intent = new Intent();
			intent.setClass(this, SingerLibrary.class);
			startActivityForResult(intent, 1);
			// startActivity(intent);
		} else if (position == 2) {
			Intent intent = new Intent(this, ArtistList.class);
			startActivityForResult(intent, 1);

		} else {
			Intent intent = new Intent();
			intent.putExtra("type", BbHotChartStr[position - 3]);
			intent.setClass(this, BbHotChart.class);
			startActivityForResult(intent, 1);
			// startActivity(intent);
		}
    
  }
  //private ArrayAdapter mAdapter;
}
