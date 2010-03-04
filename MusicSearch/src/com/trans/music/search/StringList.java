package com.trans.music.search;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.feebe.lib.BaseList;

public class StringList extends BaseList{
  private String[] mType_Animals = {
      "Yahoo! Music Top Songs",
      "Artist Library ",
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

  @Override
  public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
    if(position == 0){
      Intent intent = new Intent();
      intent.setClass(this, YahooTop.class);
      startActivityForResult(intent, 1);
        //startActivity(intent);
      }else if(position == 1){
            Intent intent = new Intent();
        intent.setClass(this, SingerLibrary.class);
      startActivityForResult(intent, 1);
        //startActivity(intent);
      }else if(position == 2){
            Intent intent = new Intent();
      intent.putExtra("type", "bhot100");
        intent.setClass(this, BbHotChart.class);
      startActivityForResult(intent, 1);
        //startActivity(intent);
      }else if(position == 3){
            Intent intent = new Intent();
      intent.putExtra("type", "bhiphop");
        intent.setClass(this, BbHotChart.class);
      startActivityForResult(intent, 1);
        //startActivity(intent);
      }else if(position == 4){
            Intent intent = new Intent();
      intent.putExtra("type", "bcountry");
        intent.setClass(this, BbHotChart.class);
      startActivityForResult(intent, 1);
        //startActivity(intent);
      }else if(position == 5){
            Intent intent = new Intent();
      intent.putExtra("type", "bmodernrock");
        intent.setClass(this, BbHotChart.class);
      startActivityForResult(intent, 1);
        //startActivity(intent);
      }else if(position == 6){
            Intent intent = new Intent();
      intent.putExtra("type", "bdanceclub");
        intent.setClass(this, BbHotChart.class);
      startActivityForResult(intent, 1);
        //startActivity(intent);
      }else if(position == 7){
            Intent intent = new Intent();
      intent.putExtra("type", "brap");
        intent.setClass(this, BbHotChart.class);
      startActivityForResult(intent, 1);
        //startActivity(intent);
      }else if(position == 8){
            Intent intent = new Intent();
      intent.putExtra("type", "bpop");
        intent.setClass(this, BbHotChart.class);
      startActivityForResult(intent, 1);
        //startActivity(intent);
      }else if(position == 9){
            Intent intent = new Intent();
      intent.putExtra("type", "bmainrock");
        intent.setClass(this, BbHotChart.class);
      startActivityForResult(intent, 1);
        //startActivity(intent);
      }
    
  }
  //private ArrayAdapter mAdapter;
}
