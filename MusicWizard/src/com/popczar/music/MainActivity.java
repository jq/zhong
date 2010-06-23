package com.popczar.music;

import com.admob.android.ads.AdManager;
import com.admob.android.ads.AdView;
import com.popczar.music.R;
import com.popczar.music.download.DownloadActivity;
import com.qwapi.adclient.android.view.QWAdView;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
	private SearchBar mSearch;
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(this));
        
        setContentView(R.layout.main); 
        AdView admob = (AdView)findViewById(R.id.adMob);
        if (admob != null){
            admob.setGoneWithoutAd(true);
        }      
        QWAdView qwAdView = (QWAdView)findViewById(R.id.QWAd);
        AdListener adListener = new AdListener(this);
        qwAdView.setAdEventsListener(adListener,
            false);
        //Utils.createQWAd(this);
		if (!EulaActivity.checkEula(this)) {
			return;
		}
        
        mSearch = new SearchBar(this);
        
        TextView downloads = (TextView)findViewById(R.id.downloads);
        downloads.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, DownloadActivity.class);
				startActivity(intent);
			}
        });
        
        TextView viewMusic = (TextView)findViewById(R.id.music_library);
        viewMusic.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				ViewDownloadedActivity.listFiles();
				Intent intent = new Intent(MainActivity.this, ViewDownloadedActivity.class);
				startActivity(intent);
			}
        });
        
        Feed.runFeed(this, R.raw.feed);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    }
    
    @Override
    protected void onDestroy() {
    	Bookmark.addBookmark(this, getContentResolver());
    	super.onDestroy();
    }
   
    
    @Override
    protected Dialog onCreateDialog(int id) {
      if (id == Feed.DOWNLOAD_APP_DIG) {
        return Feed.createDownloadDialog(this); 
      }
      return null;
    }    
    
}