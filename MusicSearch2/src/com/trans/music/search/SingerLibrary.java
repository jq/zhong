package com.trans.music.search;
import com.trans.music.search.R;

import android.app.Activity;
import android.os.Bundle;

import java.util.ArrayList;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.EditText;
import android.view.View;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.util.Log;
import android.content.Intent;


import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A list view example where the 
 * data for the list comes from an array of strings.
 */
public class SingerLibrary extends Activity {
	
	ListView mTypesList;
	private String[] mCurTypes;
	EditText mQueryWho;
	ImageButton mStartSearch;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.popular);
        findViewById(R.id.center_text).setVisibility(View.GONE);

		mTypesList = (ListView) findViewById(R.id.popular);
		AdListener.createAds(this);
        
		LayoutInflater mInflater;
		mInflater = LayoutInflater.from(this);
		View convertView = mInflater.inflate(R.layout.artist_search, null);	
		mQueryWho = (EditText)convertView.findViewById(R.id.search_who);
		mStartSearch = (ImageButton) convertView.findViewById(R.id.search_button);
		        
        mStartSearch.setOnClickListener(
            new OnClickListener() {
                public void onClick(View v) {
					final String queryWords = mQueryWho.getText().toString();
	                Intent intent = new Intent();
					Log.e("OnlineMusic ", "putExtra name : " + queryWords);
					intent.putExtra("name", queryWords);
	            	intent.setClass(SingerLibrary.this, Artist.class);
					startActivity(intent);	
				
				}
        	}	
        );
		mTypesList.addHeaderView(convertView);

		mTypesList.setAdapter(new ArrayAdapter<String>(this,  android.R.layout.simple_list_item_1, mType_Animals));		
        mTypesList.setTextFilterEnabled(true);		
		
        mTypesList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {         
				//final String key = mCurTypes[position];
                //setResult(RESULT_OK, new Intent().putExtra("keyword", key));
                //finish();
				if(position == 1){
                    Intent intent = new Intent();
	            	intent.setClass(SingerLibrary.this, SLMale.class);
					startActivityForResult(intent, 1);
	            	//startActivity(intent);
				}else if(position == 2){
                    Intent intent = new Intent();
	            	intent.setClass(SingerLibrary.this, SLFemale.class);
					startActivityForResult(intent, 1);
	            	//startActivity(intent);
				}else if(position == 3){
                    Intent intent = new Intent();
	            	intent.setClass(SingerLibrary.this, SLBand.class);
					startActivityForResult(intent, 1);
	            	//startActivity(intent);
				}

            }
        });

		
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
			Bundle extras = data.getExtras();
			String key = extras.getString("keyword");
		
            setResult(RESULT_OK, new Intent().putExtra("keyword", key));
            finish();
        } 
    }
    private String[] mType_Animals = {
            "Male Artist","Female Artist","Band"
		};
}

	


