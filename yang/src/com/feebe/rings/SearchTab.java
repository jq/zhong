package com.feebe.rings;

import java.util.ArrayList;

import com.feebe.lib.DbAdapter;
import com.feebe.lib.SearchAdapter;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SearchTab extends Activity{
	
	private AutoCompleteTextView searchArtist;
	private AutoCompleteTextView searchTitle;
	private Button searchButton;
  private Button lyricsButton;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_tab);
		AdsView.createAdsenseAds(this, AdsView.CHANNEL_ID);
		searchArtist = (AutoCompleteTextView) findViewById(R.id.input_artist);
		searchArtist.setOnKeyListener(new OnKeyListener(){
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if((event.getAction()==KeyEvent.ACTION_DOWN)&&
						(keyCode==KeyEvent.KEYCODE_ENTER)){
					searchTitle.requestFocus();
					return true;
				}
				return false;
			}
		});
		
		searchArtist.setThreshold(1);
		//ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_dropdown_item_1line,history);
		Const.dbAdapter = new DbAdapter(this);
		SearchAdapter myCursorAdapterArtist = new SearchAdapter(
		    this, Const.dbAdapter.getHistoryByType(DbAdapter.TYPE_ARTIST), DbAdapter.TYPE_ARTIST);
		searchArtist.setAdapter(myCursorAdapterArtist);
		
		searchTitle = (AutoCompleteTextView) findViewById(R.id.input_title);
		searchTitle.setOnKeyListener(new OnKeyListener(){
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if((event.getAction()==KeyEvent.ACTION_DOWN)&&
						(keyCode==KeyEvent.KEYCODE_ENTER)){
					actionListener();
					return true;
				}
				return false;
			}
		});
		searchTitle.setThreshold(1);
		SearchAdapter myCursorAdapterTitle = new SearchAdapter(
		    this, Const.dbAdapter.getHistoryByType(DbAdapter.TYPE_TITLE), DbAdapter.TYPE_TITLE);
		searchTitle.setAdapter(myCursorAdapterTitle);
		
		searchButton = (Button) findViewById(R.id.search_button);
		
		searchButton.setOnClickListener(new OnClickListener() {		
			@Override
			public void onClick(View v) {
				actionListener();
			}
		});

		lyricsButton = (Button) findViewById(R.id.lyrics_button);
    
		lyricsButton.setOnClickListener(new OnClickListener() {   
      @Override
      public void onClick(View v) {        
        String artist = searchArtist.getText().toString();
        String title = searchTitle.getText().toString();
        boolean hasTitle = title.length() > 0;
        boolean hasArtist = artist.length() > 0;
        if (hasTitle && hasArtist) {
          Intent intent = new Intent();
          String url = "http://ggapp.appspot.com/mobile/lyric/?a=" + artist + "&s=" + title;
          intent.putExtra("url", url);
          intent.setClass(SearchTab.this, WebViewActivity.class);
          startActivity(intent);

        } else {
          Toast.makeText(SearchTab.this, R.string.lyrics_search_warning, Toast.LENGTH_SHORT);
        }
      }
    });
	}
	
	private void actionListener(){
		String artist = searchArtist.getText().toString();
		String title = searchTitle.getText().toString();
		boolean hasTitle = title.length() > 0;
		boolean hasArtist = artist.length() > 0;
		Const.dbAdapter = new DbAdapter(SearchTab.this);
		if(hasTitle) {
			if (hasArtist) {
				Const.dbAdapter.intsertHistory(artist, DbAdapter.TYPE_ARTIST);
				Const.dbAdapter.intsertHistory(title, DbAdapter.TYPE_TITLE);
				Search.getArtistAndTitle(this, artist, title);
			} else {
				Const.dbAdapter.intsertHistory(title, DbAdapter.TYPE_TITLE);
				Search.getTitleRing(this, title);
			}
		} else {
			if (hasArtist) {
				Const.dbAdapter.intsertHistory(artist, DbAdapter.TYPE_ARTIST);
				Search.getArtistRing(this, artist);
			}
		}
	}
	
	/*public void getHistory(){
		 if (searchDBAdapter == null) {
       	  searchDBAdapter = new SearchDBAdapter(this.getBaseContext(),Const.DBName);
         }
         searchDBAdapter.open();
         
         searchHistory = new ArrayList<String>();
	     Cursor c = searchDBAdapter.getAllHistories();
	     if(c.getCount()>0){
	    	 c.moveToFirst();
	    	 do{
	    		 searchHistory.add(c.getString(0));
	    	 }while(c.moveToNext());
	     }
	     
	     //Toast.makeText(getContext(), histories, Toast.LENGTH_LONG).show();
	     //searchDBAdapter.close();
	}*/
}
