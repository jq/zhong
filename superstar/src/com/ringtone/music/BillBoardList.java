package com.ringtone.music;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ringtone.music.download.DownloadActivity;
import com.ringtone.music.download.DownloadJson;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


public class BillBoardList extends ListActivity implements OnItemClickListener {
  
  private String url = "";
  private JSONArray jArray;
  
  private static final int DIALOG_MUSIC_OPTIONS = 1;
  
  private static final int SERACH_THE_SONG = 0;
  private static final int SERACH_THE_SINGER = 1;
  
  
  private String mCurrentTitle;
  private String mCurrentArtist;
  
  @Override
  public void onCreate(android.os.Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    url = getIntent().getStringExtra("url");
    setContentView(R.layout.billboard_detail_list);
    Utils.addAds(this);
    
    SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.billboard_detail_item, new String[]{"artist","title"}, new int[]{R.id.billboardListItem1,R.id.billboardListItem2});
    setListAdapter(adapter);
    getListView().setOnItemClickListener(this);
  };


  
  private List<Map<String, Object>> getData() {
    
    List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(); 
    Map<String, Object> map;
    url = getIntent().getStringExtra("url");
    //Log.e("URL: ", url);
    
    JSONObject jObject = DownloadJson.getJsonFromUrl(url, DownloadJson.ThreeAndAHalfDays);
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
	  try {
		  mCurrentTitle = jArray.getJSONArray(pos).getString(0);
		  mCurrentArtist = jArray.getJSONArray(pos).getString(1);
		  showDialog(DIALOG_MUSIC_OPTIONS);
	  } catch (JSONException e) {
		  e.printStackTrace();
	  }
//    Log.e("onItemClick", "pos: " +pos);
//    try {
//      String title = jArray.getJSONArray(pos).getString(0);
//      String artist = jArray.getJSONArray(pos).getString(1);
//      if(title.length() + artist.length() > 0) {
//        String query = title; 
//        Log.e("query : ", query);
//        SearchResultActivity.startQuery(BillBoardList.this.getApplication(), query);
//        SearchResultActivity.handleMp3ListIntent(BillBoardList.this, query);
//      }
//    } catch (JSONException e) {
//      e.printStackTrace();
//    }
  }
  
  @Override
  protected void onPrepareDialog(int id, Dialog dialog) {
	  switch (id) {
	  case DIALOG_MUSIC_OPTIONS: {
		  if (mCurrentTitle != null){
			  dialog.setTitle("Options for \"" + mCurrentTitle + "\"");
		  }
		  return;
	  }
	  }
  }
  
  @Override
  protected Dialog onCreateDialog(int id) {
	  switch (id) {
	  case DIALOG_MUSIC_OPTIONS:
		  return new AlertDialog.Builder(BillBoardList.this)
		  .setTitle(R.string.options)
          .setItems(R.array.search_song_singer, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case SERACH_THE_SONG:
							SearchResultActivity.handleMp3ListIntent(BillBoardList.this, mCurrentTitle);
							SearchResultActivity.startQuery(BillBoardList.this, mCurrentTitle);
							break;
						case SERACH_THE_SINGER:
							Intent intent = new Intent(BillBoardList.this, SingerList.class);
							intent.putExtra("keyword", mCurrentArtist);
							startActivity(intent);
//							SingerList.startQuery(BillBoardList.this, "gaga");
							break;
						}
					}
          })
          .create();
	  }
	return null;
  }
  
  private void NoDataError() {
    Toast.makeText(getApplicationContext(), "No data", Toast.LENGTH_SHORT).show();
    BillBoardList.this.finish();
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
            Intent intent = new Intent(BillBoardList.this, DownloadActivity.class);
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
			
			Intent intent1 = new Intent(BillBoardList.this, SingerList.class);
			intent1.putExtra("type", "allsingers");
			startActivity(intent1);

			return true;
		}

		return false;
	}

}