package com.feebe.rings;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
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
		searchTitle = (EditText) findViewById(R.id.input_title);
		searchButton = (Button) findViewById(R.id.search_button);
		
		searchButton.setOnClickListener(new OnClickListener() {		
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
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
		});
	}

}
