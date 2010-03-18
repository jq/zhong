package com.trans.music.search;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
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
    
    searchButton.setOnClickListener(new OnClickListener() {   
      @Override
      public void onClick(View v) {
        String title = searchTitle.getText().toString();
        boolean hasTitle = title.length() > 0;
        if(hasTitle) {
          Intent intent = new Intent();
          intent.putExtra(Const.Key, title);
          intent.setClass(SearchTab.this, SearchList.class);
          startActivityForResult(intent, 1);
        }
      }
    });
  }

}
