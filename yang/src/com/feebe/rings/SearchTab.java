package com.feebe.rings;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;

public class SearchTab extends Activity{
	
	private EditText searchArtist;
	private EditText searchTitle;
	private Button searchButton;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_tab);
		
		searchArtist = (EditText) findViewById(R.id.input_artist);
		searchArtist.setOnKeyListener(new OnKeyListener(){
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
			
		
		searchTitle = (EditText) findViewById(R.id.input_title);
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
		
		searchButton = (Button) findViewById(R.id.search_button);
		
		searchButton.setOnClickListener(new OnClickListener() {		
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				/*String artist = searchArtist.getText().toString();
				String title = searchTitle.getText().toString();
				boolean hasTitle = title.length() > 0;
				boolean hasArtist = artist.length() > 0;
				
				if(hasTitle) {
					if (hasArtist) {
					  Search.getArtistAndTitle(artist, title);
					} else {
						Search.getTitleRing(title);
					}
				} else {
					if (hasArtist) {
						Search.getArtistRing(artist);
					}
				}*/
				actionListener();
			}
		});
	}
	private void actionListener(){
		String artist = searchArtist.getText().toString();
		String title = searchTitle.getText().toString();
		boolean hasTitle = title.length() > 0;
		boolean hasArtist = artist.length() > 0;
		
		if(hasTitle) {
			if (hasArtist) {
			  Search.getArtistAndTitle(artist, title);
			} else {
				Search.getTitleRing(title);
			}
		} else {
			if (hasArtist) {
				Search.getArtistRing(artist);
			}
		}
	}
	
}
