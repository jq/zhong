package com.trans.music.search;


import java.util.List;

import com.jokes.search.R;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;


public class help extends Activity
{
	// Help Info
	TextView mHelpInformation;	
	Button mButtonDonate;
	Button mButtonMusicSearchPro;
	Button mButtonBbs;
    @Override
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.help);

		mButtonDonate = (Button) findViewById(R.id.button_donate);
        //mButtonMusicSearchPro = (Button) findViewById(R.id.button_musicsearchpro);
        
		mHelpInformation = (TextView) findViewById(R.id.help);
		mHelpInformation.setText("    Enter title or/and singer to search mp3 music. For example enter \"remember the time michael jackson\" to search. \n\n    Click link that you want to listen in search results to play music. Maybe some link is invalid, try another link. If the link can play, click Save in menu to save the selected music to SD card��s MusicSearch Directory.\n\n    In Local Library , you can add music to ringtone when it playing.\n\n    If you like this app, donate this app cost $7.99 or dowload MusicSearchPro cost $9.99 to get more music search results and search faster. \n\n");
	
	    mButtonDonate.setOnClickListener(
	            new OnClickListener() {
	                 public void onClick(View v) {
						Intent intent = new Intent(help.this, paypal.class);
						startActivity(intent);
	                 }
	            }
	    );

	    mButtonBbs = (Button) findViewById(R.id.bbs);
	    mButtonBbs.setOnClickListener(
	            new OnClickListener() {
	                 public void onClick(View v) {
	                	Intent intent;
	                	if (has("net.laoyu.app_review")) {
	                		intent = new Intent();
	                        intent.setAction("net.laoyu.app_review.action.VIEW_TOPIC");
	                        intent.putExtra("net.laoyu.app_review.app_package", "com.trans.music.search");
	                        intent.putExtra("net.laoyu.app_review.app_name", "Music Search");
	                    } else {
		 	               intent = new Intent(
		 	                        Intent.ACTION_VIEW,
		 	                        Uri.parse("market://search?q=pname:net.laoyu.app_review"));
	                	}
						startActivity(intent);
	                 }
	                 
	             	private boolean has(String n1) {
	            		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
	            		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

	            		final PackageManager manager = getPackageManager();
	            		// TODO manager.getApplicationInfo
	            		
	            		final List<ResolveInfo> apps = manager.queryIntentActivities(
	            				mainIntent, 0);
	            		for (int i = 0; i < apps.size(); i++) {
	            			ResolveInfo info = apps.get(i);
	            			if (info.activityInfo.applicationInfo.packageName.equals(n1)) {
	            				return true;
	            			}
	            		}
	            		return false;
	            		
	            	}

	            }
	    );

	    /*
	    mButtonMusicSearchPro.setOnClickListener(
	            new OnClickListener() {
	                 public void onClick(View v) {
	 	                Intent intent = new Intent(
	 	                        Intent.ACTION_VIEW,
	 	                        Uri.parse("market://search?q=pname:com.trans.music.searchpro"));
	 	                startActivity(intent);
	                 }
	            }
	    );
	    */
    }
    
}

