package com.popczar.music;

import com.popczar.music.R;
import com.popczar.music.download.DownloadActivity;

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
        Feed.runFeed(8, this, R.raw.feed);
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
      if (id == Feed.DOWNLOAD_APP_DIG) {
        return Feed.createDownloadDialog(this); 
      }
      return null;
    }    
    
}