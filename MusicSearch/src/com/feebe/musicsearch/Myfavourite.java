package com.feebe.musicsearch;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Map;

import com.feebe.lib.BaseList;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

public class Myfavourite extends BaseList {

	private ArrayList<String> artistList = new ArrayList<String>();
	
	private static final String URL_BASE = "http://ggapp.appspot.com/ringtone/search/?json=1&";
	
	
	private void initData() {
		artistList.clear();
		SharedPreferences s = getSharedPreferences(Const.MP3TITLE, 0);
		Map<String, ?> layers = s.getAll();
		if (layers.size() > 0) {
			for (String id : layers.keySet()) {
				if (!artistList.contains(id)) {
					artistList.add(id);
				}
			}
		}
	}
	
	@Override
	public ListAdapter getAdapter() {
		initData();
		if (artistList.size() != 0) {
			String temp [] = new String[artistList.size()];
			String data []= (String[]) (artistList.toArray(temp));
			return new ArrayAdapter<String>(this,  android.R.layout.simple_list_item_1, data);
		}
		return null;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
		String artist = artistList.get(pos);
		Intent intent = new Intent();
		intent.putExtra(Const.Key, artist);
	    intent.putExtra(Const.expire, Const.OneWeek);
	    intent.setClass(this, SearchList.class);
	    startActivity(intent);
	}
	
}
