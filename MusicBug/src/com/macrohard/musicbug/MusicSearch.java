package com.macrohard.musicbug;


import com.macrohard.musicbug.R;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.ImageButton;

public class MusicSearch extends Activity {
	  private EditText mSearchTitle;
	  private ImageButton mSearchButton;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
        Ads.addAds(this);
        mSearchTitle = (EditText)findViewById(R.id.search_query_words);
        mSearchButton = (ImageButton)findViewById(R.id.search_button);
        
        mSearchTitle.setOnKeyListener(new OnKeyListener() {

    		@Override
    		public boolean onKey(View v, int keyCode, KeyEvent event) {
    			if(keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
    				search();
    				return true;
    			}
    			return false;
    		}
        });
        
        mSearchButton.setOnClickListener(new OnClickListener() {   
            @Override
            public void onClick(View v) {
              search();
            }
        });
    }
    
    private void search() {
	  	String title = mSearchTitle.getText().toString();

	  	Debug.D("title = " + title);
	  	
	    if (!TextUtils.isEmpty(title)) {
	    	Mp3ListActivity.handleMp3ListIntent(this, title);
	    }
    }
}