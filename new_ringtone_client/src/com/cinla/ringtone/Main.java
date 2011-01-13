package com.cinla.ringtone;

import java.util.ArrayList;
import com.ringdroid.RingdroidSelectActivity;
import com.ringdroidlib.RingSelectActivity;

import android.R.integer;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.SimpleAdapter.ViewBinder;

public class Main extends ListActivity {
	
	private static ArrayList<String> sFunctionArray;
	
	private SearchBar mSearchBar;
	private FunctionAdapter mAdapter;
	
	private static final int HOME_TOP_DOWNLOAD = 0;
	private static final int HOME_NEWEST = 1;
	private static final int HOME_ALL_CATEGORIES = 2;
	private static final int HOME_LIBRARY = 3;
	private static final int HOME_RATE_US = 4;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        //init. should use a background thread and splash screen...
        Constant.init(this);
        
        mSearchBar = new SearchBar(this);

        init();
    }
    
    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
    	switch (position) {
		case 0:
			SearchListActivity.startQueryByDownloadcount(Main.this);
			break;
		case 1:
			SearchListActivity.startQueryByDate(Main.this);
			break;
		case 2:
			Intent intent = new Intent(Main.this, CategoriesListActivity.class);
			startActivity(intent);
			break;
		case 3:
			Intent intent2 = new Intent(Main.this, RingSelectActivity.class);
			startActivity(intent2);
			break;
		case 4:
			// market intent
			break;
		default:
			break;
		}
		super.onListItemClick(l, v, position, id);
	}

	private void init() {
    	if (sFunctionArray == null) {
    		sFunctionArray = new ArrayList<String>();
    	} else {
    		sFunctionArray.clear();
    	}
    	sFunctionArray.add(getString(R.string.top_download));
    	sFunctionArray.add(getString(R.string.newest));
    	sFunctionArray.add(getString(R.string.all_categories));
    	sFunctionArray.add(getString(R.string.library));
    	sFunctionArray.add(getString(R.string.rate_promote));
    	if (mAdapter == null) {
    		mAdapter = new FunctionAdapter(Main.this, R.layout.function_item);
    	} 
    	getListView().setAdapter(mAdapter);
    	mAdapter.notifyDataSetChanged();
    }

    private class FunctionAdapter extends BaseAdapter {

    	private int mResource;
		private LayoutInflater mInflater;
		
		public FunctionAdapter(Context context, int resource) {
			mResource = resource;
			mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		@Override
		public int getCount() {
			if (sFunctionArray != null) {
				return sFunctionArray.size();
			} else {
				return 0;
			}
		}

		@Override
		public Object getItem(int position) {
			if (sFunctionArray==null || position>=sFunctionArray.size()) {
				return null;
			}
			return sFunctionArray.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			Object item = sFunctionArray.get(position);
			if (convertView == null) {
				v = mInflater.inflate(mResource, parent, false);
			} else {
				v = convertView;
			}
			String functionTitle = (String) item;
			
			((TextView) v.findViewById(R.id.function_title)).setText(functionTitle);
			
			return v;
		}
    	
    }
}