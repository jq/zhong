package com.feebe.rings;

import java.util.ArrayList;

import com.feebe.lib.DbAdapter;
import com.feebe.lib.SearchAdapter;
import com.feebe.lib.Util;
import com.lib.RingSelect;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
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
import android.widget.TextView;
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
		try {
  		SearchAdapter myCursorAdapterArtist = new SearchAdapter(
  		    this, Const.dbAdapter.getHistoryByType(DbAdapter.TYPE_ARTIST), DbAdapter.TYPE_ARTIST);
  		searchArtist.setAdapter(myCursorAdapterArtist);
      } catch (Exception e) {
      
    }
		
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
		try {
  		SearchAdapter myCursorAdapterTitle = new SearchAdapter(
  		    this, Const.dbAdapter.getHistoryByType(DbAdapter.TYPE_TITLE), DbAdapter.TYPE_TITLE);
  		searchTitle.setAdapter(myCursorAdapterTitle);
		} catch (Exception e) {
		  
		}
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

	TextView billboard = (TextView) findViewById(R.id.billboard);
	billboard.setOnClickListener(new OnClickListener() {   
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(SearchTab.this, BillBoardCate.class);
            startActivity(intent);
        }
    });
    TextView myfavor = (TextView)findViewById(R.id.myfavor);
    myfavor.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            Intent intent = new Intent(SearchTab.this, MyfovorList.class);
            startActivity(intent);
        }
    });
    TextView hot = (TextView)findViewById(R.id.hot);
    hot.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            Intent intent = new Intent(SearchTab.this, HotList.class);
            startActivity(intent);
        }
    });
    TextView rank = (TextView)findViewById(R.id.rank);
    rank.setOnClickListener(new OnClickListener() {
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
            Intent intent = new Intent(SearchTab.this, RingSelect.class);
            startActivity(intent);
        }
    });
    TextView rate = (TextView)findViewById(R.id.rate);
      rate.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View arg0) {
            Util.startRate(SearchTab.this);
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
