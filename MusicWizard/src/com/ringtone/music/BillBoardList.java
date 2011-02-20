package com.ringtone.music;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ringtone.music.download.DownloadJson;
import com.ringtone.music.download.DownloadActivity;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


public class BillBoardList extends ListActivity implements OnItemClickListener {

    private static final int DIALOG_WAITING_FOR_SERVER = 1;
    
    private ProgressDialog mProgressDialog;
    
	private static BillBoardList sActivity;
	private static FetchBillboardTask sTask;
	
	// TODO(zyu): Redefine a better container for this.
	private static JSONArray sJsonArray;
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	switch (id) {
    	case DIALOG_WAITING_FOR_SERVER:
            if (mProgressDialog == null) {
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setMessage(getString(R.string.wait));
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(true);
            }
            return mProgressDialog;
    	}
    	return null;
    }

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sActivity = this;
        
        String url = getIntent().getStringExtra("url");
        setContentView(R.layout.billboard_detail_list);
        Utils.addAds(this);
        
        if (sTask != null)
        	sTask.cancel(true);
        sTask = new FetchBillboardTask();
        sTask.execute(url);
        
        getListView().setOnItemClickListener(this);
    };
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	if (mProgressDialog != null && mProgressDialog.isShowing()) {
    		mProgressDialog.dismiss();
    	}
    	
    	if (sTask != null) {
    		// Fetch going on.
	        showDialog(DIALOG_WAITING_FOR_SERVER );
    	}
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	if (mProgressDialog != null && mProgressDialog.isShowing()) {
    		mProgressDialog.dismiss();
    	}
    	mProgressDialog = null;
    }
    
    private void handleBillboardUpdate(List<Map<String, Object>> feed) {
    	if (feed != null) {
	        SimpleAdapter adapter = new SimpleAdapter(
	                this, feed, R.layout.billboard_detail_item,
	                new String[] { "artist", "title" },
	                new int[]{ R.id.billboardListItem1, R.id.billboardListItem2 });
	        setListAdapter(adapter);
    	} else {
    		noDataError();
    	}
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
        	mProgressDialog.dismiss();
        }
    }
    
    private static class FetchBillboardTask extends AsyncTask<String, Void, List<Map<String, Object>>> {
		@Override
		protected List<Map<String, Object>> doInBackground(String... params) {
			String url = params[0];
			return getData(url);
		}
		
		@Override
		protected void onPostExecute(List<Map<String, Object>> result) {
			if (sTask != this) {
				// Probably another task is running?
				return;
			}
			sTask = null;
			if (sActivity != null) {
				sActivity.handleBillboardUpdate(result);
			}
		}
    }

    private static List<Map<String, Object>> getData(String url) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(); 
        Map<String, Object> map;
        
        JSONObject jObject;
		jObject = DownloadJson.getJsonFromUrl(url, DownloadJson.OneDay);
		if (jObject == null) {
			return null;
		}

        try {
            sJsonArray = jObject.getJSONArray("list");
            for (int i = 0; i < sJsonArray.length(); i++) {
                JSONArray item = sJsonArray.getJSONArray(i);
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

        return list;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
        try {
            String title = sJsonArray.getJSONArray(pos).getString(0);
            String artist = sJsonArray.getJSONArray(pos).getString(1);
            if (title.length() + artist.length() > 0) {
                String query = title; 
                HistoryAdapter adapter = HistoryAdapter.getInstance(getApplication());
                adapter.insertHistory(query, HistoryAdapter.TYPE_SEARCH);
                SearchResultActivity.startQuery(BillBoardList.this.getApplication(), query);
                SearchResultActivity.handleMp3ListIntent(BillBoardList.this, query);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void noDataError() {
        Toast.makeText(getApplicationContext(), "No data", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.billboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bill_dowloads:
                Intent intent = new Intent(BillBoardList.this, DownloadActivity.class);
                startActivity(intent);
                return true;
        }

        return false;
    }
}
