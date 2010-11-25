package com.trans.music.search;


import java.io.FileInputStream;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.jokes.search.R;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;


public class online extends Activity
{
    // Online Playlist
	ListView mOnlineList;
	JSONArray mOnlineMp3s = new JSONArray();
    ArrayAdapter<String> mOnlineAdapter; 
    ArrayList<String> mOnlineStrings = new ArrayList<String>();
	
    @Override
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.online);

        // Online Playlist UI
        mOnlineList = (ListView) findViewById(R.id.online_playlist);
        mOnlineAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mOnlineStrings);    
        mOnlineList.setAdapter(mOnlineAdapter); 
        
        try{
        	
			FileInputStream fonlinelist =  openFileInput("online.list");
			byte[] b = new byte[1024*32];
			fonlinelist.read(b);
			fonlinelist.close();
			
			String online = new String(b);
			mOnlineMp3s = new JSONArray(online);
			
			JSONArray entries = mOnlineMp3s;
			for(int i = 0; i < entries.length(); i++){
				if( entries.isNull(i) )
					break;
				
				JSONObject mp3 = entries.getJSONObject(i);
			
				String mp3tile = mp3.getString("title");
				String songer = mp3.getString("songer");
				mOnlineAdapter.add(mp3tile + "[" + songer + "]");
			}
			
        }catch(Exception e) {
			e.printStackTrace();
		} 

    }
}