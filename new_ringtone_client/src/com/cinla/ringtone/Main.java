package com.cinla.ringtone;

import com.ringdroid.RingdroidSelectActivity;
import com.ringdroidlib.RingSelectActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SimpleAdapter.ViewBinder;

public class Main extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        //init. should use a background thread and splash screen...
        Constant.init(this);

        Button searchButton = (Button) findViewById(R.id.home_search_button);
        searchButton.setOnClickListener(new SearchButtonClickListener());
        
        Button libButton = (Button) findViewById(R.id.home_lib_button);
        libButton.setOnClickListener(new LibButtonClickListener());
        
        Button topDownlaodButton = (Button) findViewById(R.id.home_topdownload_button);
        topDownlaodButton.setOnClickListener(new TopDownloadButtonClickListener());
    }

    private class SearchButtonClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(Main.this, SearchListActivity.class);
			intent.putExtra(Constant.SEARCH_TYPE, Constant.TYPE_KEY);
			Main.this.startActivity(intent);
		}
    }
    
    private class LibButtonClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(Main.this, RingSelectActivity.class);
			startActivity(intent);
		}
    }
    
    private class TopDownloadButtonClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			SearchListActivity.startQueryByDownloadcount(Main.this);
		}
    	
    }
}