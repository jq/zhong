package com.feebe.rings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.feebe.lib.BaseList;
import com.feebe.lib.Util;

public class BillBoardList extends BaseList {
  
  private String url = "";
  private JSONArray jArray;
  
  @Override
  public void onCreate(android.os.Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    url = getIntent().getStringExtra("url");
  };

  @Override
  public ListAdapter getAdapter() {
    SimpleAdapter mAdapter = new SimpleAdapter(this, getData(), R.layout.comment_list_item, new String[]{"user","comment"}, new int[]{R.id.commentListItem1,R.id.commentListItem2});
    
    return mAdapter;
  }
  
  private List<Map<String, Object>> getData() {
    
    List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(); 
    Map<String, Object> map;
    url = getIntent().getStringExtra("url");
    //Log.e("URL: ", url);
    
    JSONObject jObject = Util.getJsonFromUrl(url, Const.OneMonth);
    if(jObject == null) {
      NoDataError();
    }
    //Log.e("json: ", jObject.toString());

    try {
      jArray = jObject.getJSONArray("list");
      for(int i = 0; i < jArray.length(); i++) {
        JSONArray item = jArray.getJSONArray(i);
        String title = item.getString(0);
        String artist = item.getString(1);    
        map = new HashMap<String, Object>();
        map.put("user", artist);
        map.put("comment", title);
        list.add(map);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    if(list.size() == 0) {
      NoDataError();
    }
    
    return list;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
    try {
      String title = jArray.getJSONArray(pos).getString(0);
      String artist = jArray.getJSONArray(pos).getString(0);
      if(title.length() + artist.length() > 0)
        Search.getArtistAndTitle(BillBoardList.this, artist, title);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    

  }
  
  private void NoDataError() {
    Toast.makeText(getApplicationContext(), "No data", Toast.LENGTH_SHORT).show();
    BillBoardList.this.finish();
  }

}
