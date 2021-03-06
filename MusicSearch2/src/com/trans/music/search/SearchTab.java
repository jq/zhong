package com.trans.music.search;

import com.jokes.search.R;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class SearchTab  extends Activity {
  private EditText searchTitle;
  private ImageButton searchButton;
  @Override
  public void onNewIntent(Intent intent) {
    Intent i = this.getIntent();
    String key = i.getStringExtra("key");
    if (key != null && key.length() > 0)
      searchTitle.setText(key);
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    setContentView(R.layout.search_tab);
    Intent i = this.getIntent();
    String key = i.getStringExtra("key");
    
    searchTitle = (EditText) findViewById(R.id.search_query_words);
    if (key != null && key.length() > 0)
      searchTitle.setText(key);
    searchButton = (ImageButton) findViewById(R.id.search_button);
    
    AdListener.createAds(this);
    
    searchTitle.setOnKeyListener(new OnKeyListener() {

		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if(keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
				search();
				return true;
			}
			return false;
		}
    	
    });
    
    searchButton.setOnClickListener(new OnClickListener() {   
      @Override
      public void onClick(View v) {
        search();
      }
    });
    
    TextView downloads = (TextView)findViewById(R.id.downloads);
    downloads.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(SearchTab.this, local.class);
            startActivity(intent);
        }
    });
    
    TextView viewMusic = (TextView)findViewById(R.id.hot);
    viewMusic.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            Intent intent = new Intent(SearchTab.this, StringList.class);
            startActivity(intent);
        }
    });
    
    TextView ringtone = (TextView)findViewById(R.id.ringtone);
    ringtone.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            Intent intent = new Intent(SearchTab.this, com.other.RingSelectActivity.class);
            startActivity(intent);
        }
    });
    
    TextView play = (TextView)findViewById(R.id.play);
    play.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            loadDefaultMusicApp();
        }
    });
    
    TextView rate = (TextView)findViewById(R.id.rate);
    rate.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View arg0) {
          Intent i = new Intent(
              Intent.ACTION_VIEW,
              Uri.parse("market://details?id=pname:com.ringtone.music.search1"));
          startActivity(i);
        }
    });
  }
  
  private void search() {
	  String title = searchTitle.getText().toString();
      boolean hasTitle = title.length() > 0;
      if(hasTitle) {
        Intent intent = new Intent();
        intent.putExtra(Const.Key, title);
        intent.setClass(SearchTab.this, SearchList.class);
        startActivityForResult(intent, 1);
      }
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    // Inflate the currently selected menu XML resource.
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.m, menu);      
      
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.menu_search:
      startSearch(this.getString(R.string.search_hint), true, null, false);
      return true;
    case R.id.menu_hot:
      startActivity(new Intent(this, StringList.class));
      return true;
    case R.id.menu_local:
      startActivity(new Intent(this, local.class));
      return true;
    case R.id.menu_ringdroid:
      startActivity(new Intent(this, com.other.RingSelectActivity.class));
      return true;
    case R.id.menu_music:
      loadDefaultMusicApp();
      return true;
    case R.id.menu_help:
      startActivity(new Intent(this, help.class));
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void loadDefaultMusicApp() {
    try {
      Intent intent = new Intent();
      intent.setClassName("com.android.music",
          "com.android.music.MusicBrowserActivity");
      startActivity(intent);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
