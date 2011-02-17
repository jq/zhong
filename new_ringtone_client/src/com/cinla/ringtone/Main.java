package com.cinla.ringtone;

import java.util.ArrayList;

import com.latest.ringtone.R;
import com.ringdroid.RingdroidSelectActivity;
import com.ringdroidlib.RingSelectActivity;
import android.R.integer;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.SimpleAdapter.ViewBinder;

public class Main extends Activity {
	
	private SearchBar mSearchBar;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        AdListener.createAds(this);
        
        //init. should use a background thread and splash screen...
        Constant.init(this);
        
        mSearchBar = new SearchBar(this);
        
        ((TextView) findViewById(R.id.home_lastfm_top_charts)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Main.this, LastFmChartListActivity.class);
				Main.this.startActivity(intent);
			}
		});

        ((TextView) findViewById(R.id.home_top_download)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SearchListActivity.startQueryByDownloadcount(Main.this);
			}
		});
        ((TextView) findViewById(R.id.home_newest)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SearchListActivity.startQueryByDate(Main.this);
			}
		});
        ((TextView) findViewById(R.id.home_all_category)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Main.this, CategoriesListActivity.class);
				startActivity(intent);
			}
		});
        ((TextView) findViewById(R.id.home_library)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				Intent intent2 = new Intent(Main.this, RingSelectActivity.class);
				startActivity(intent2);
			}
		});
        
        ((TextView) findViewById(R.id.home_rate_promote)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String url = "market://details?id=" + getPackageName();
				try {
					Uri uri = Uri.parse(url);
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		    		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
        mSearchBar.setHint();
    }

}