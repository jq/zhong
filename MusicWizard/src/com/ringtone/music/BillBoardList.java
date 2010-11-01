package com.ringtone.music;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ringtone.music.download.DownloadJson;

import android.app.ListActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


public class BillBoardList extends ListActivity implements OnItemClickListener {
  
  private String url = "";
  private JSONArray jArray;
  
  @Override
  public void onCreate(android.os.Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    url = getIntent().getStringExtra("url");
    
    SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.billboard_list_item, new String[]{"artist","title"}, new int[]{R.id.billboardListItem1,R.id.billboardListItem2});
    setListAdapter(adapter);
    getListView().setOnItemClickListener(this);
  };


  
  private List<Map<String, Object>> getData() {
    
    List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(); 
    Map<String, Object> map;
    url = getIntent().getStringExtra("url");
    //Log.e("URL: ", url);
    
    JSONObject jObject = DownloadJson.getJsonFromUrl(url, DownloadJson.OneMonth);
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
        map.put("artist", artist);
        map.put("title", title);
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
    Log.e("onItemClick", "pos: " +pos);
    try {
      String title = jArray.getJSONArray(pos).getString(0);
      String artist = jArray.getJSONArray(pos).getString(1);
      if(title.length() + artist.length() > 0) {
        String query = title; 
        Log.e("query : ", query);
        SearchResultActivity.startQuery(BillBoardList.this.getApplication(), query);
        SearchResultActivity.handleMp3ListIntent(BillBoardList.this, query);
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
    

  }
  
  private void NoDataError() {
    Toast.makeText(getApplicationContext(), "No data", Toast.LENGTH_SHORT).show();
    BillBoardList.this.finish();
  }

}
