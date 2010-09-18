package com.feebe.musicsearch;

import android.app.Activity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;

public class SearchBar {

	private Activity mActivity;
	private EditText mQuery;
	private Button mGo;
	
	public SearchBar(Activity activity) {
		mActivity = activity;
        mQuery = (EditText)activity.findViewById(R.id.q);
        mGo = (Button)activity.findViewById(R.id.go);
        
        mQuery.setOnKeyListener(new OnKeyListener() {

    		@Override
    		public boolean onKey(View v, int keyCode, KeyEvent event) {
    			if(keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
    				doSearch();
    				return true;
    			}
    			return false;
    		}
        });
        
        mGo.setOnClickListener(new OnClickListener() {   
            @Override
            public void onClick(View v) {
              doSearch();
            }
        });
        
	}
	
	public String getQuery() {
		if (mQuery != null) {
			return mQuery.getText().toString();
		}
		return null;
	}
	
	private void doSearch() {
		String query = mQuery.getText().toString();
		Utils.D("start search");

		if (!TextUtils.isEmpty(query)) {
			SearchActivity.startQuery(query);
			SearchActivity.handleMp3ListIntent(mActivity, query);
		}
	}
}
