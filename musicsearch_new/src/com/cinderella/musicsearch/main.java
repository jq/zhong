package com.cinderella.musicsearch;

import com.libhy.RingSelect;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class main extends Activity {

	private Button mSearchButton;
	private Button mLibraryButton;
	private Button mRingdroidButton;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mSearchButton = (Button)findViewById(R.id.search);
        mSearchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(main.this, SearchActivity.class));
			}
		});
        mLibraryButton = (Button) findViewById(R.id.library);
        mLibraryButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(main.this, DownloadedActivity.class));
			}
		});
        mRingdroidButton = (Button) findViewById(R.id.edit);
        mRingdroidButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(main.this, com.libhy.RingSelect.class));
			}
		});
    }
}