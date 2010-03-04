

package com.trans.music.search;


import com.admob.android.ads.*;

import android.app.Activity;
import android.os.Bundle;


import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import android.view.View;
import android.view.View.OnClickListener;

import android.content.Intent;

import android.util.Log;

/**
 * A list view example where the 
 * data for the list comes from an array of strings.
 */
public class Popular extends Activity {
	
	ListView mTypesList;
	private String[] mCurTypes;
	private AdView mAd;

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.popular);
        findViewById(R.id.center_text).setVisibility(View.GONE);

        mAd = (AdView) findViewById(R.id.ad);	
        mAd.setVisibility(View.VISIBLE);
        //new AdsView(this);
        
        mTypesList = (ListView) findViewById(R.id.popular);

		mTypesList.setAdapter(new ArrayAdapter<String>(this,  android.R.layout.simple_list_item_1, mType_Animals));
		
        mTypesList.setTextFilterEnabled(true);


        mTypesList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {         
				//final String key = mCurTypes[position];
                //setResult(RESULT_OK, new Intent().putExtra("keyword", key));
                //finish();

            }
        });

		
    }

    private String[] mType_Animals = {
            "Yahoo! Music Top Songs",
			"Artist Library ",
			"Hot Top Songs",
			"Hip Hop / R&B Songs",
			"Country Songs",
			"Rock Songs",
			"Dance/Club Play Songs",
			"Rap Tracks",
			"Pop Songs",
			"Mainstream Rock Tracks"
		};




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
			Bundle extras = data.getExtras();
			String key = extras.getString("keyword");
			String artist = extras.getString("artist");
			String song = extras.getString("song");


			Intent intent = new Intent();
			intent.putExtra("keyword", key);
			intent.putExtra("artist", artist);
			intent.putExtra("song", song);
			Log.e("MusicSearch :Popular ", "artist: " + artist + "  song: " + song);
			
            setResult(RESULT_OK, intent);
            finish();
        } 
    }
	
}
