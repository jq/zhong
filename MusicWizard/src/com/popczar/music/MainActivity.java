package com.popczar.music;

import com.popczar.music.R;
import com.popczar.music.download.DownloadActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {
	private SearchBar mSearch;
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Utils.createQWAd(this);
		if (!EulaActivity.checkEula(this)) {
			return;
		}
        
        mSearch = new SearchBar(this);
        
        Button downloadsButton = (Button)findViewById(R.id.downloads_button);
        downloadsButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, DownloadActivity.class);
				startActivity(intent);
			}
        	
        });
    }
}