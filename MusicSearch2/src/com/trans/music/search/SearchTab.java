package com.trans.music.search;

import com.trans.music.search.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

public class SearchTab  extends Activity {
  private EditText searchTitle;
  private ImageButton searchButton;
  
  @Override
  public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    setContentView(R.layout.search_tab);
    
    searchTitle = (EditText) findViewById(R.id.search_query_words);
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

}
