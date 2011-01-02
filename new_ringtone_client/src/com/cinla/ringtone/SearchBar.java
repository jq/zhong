package com.cinla.ringtone;

import android.app.Activity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

public class SearchBar {

	private Activity mActivity;
	private EditText mQuery;
	private Button mGo;
	LinearLayout mSearchBarLayout;
	
	public SearchBar(Activity activity) {
		mActivity = activity;
        mQuery = (EditText)activity.findViewById(R.id.query_key);
        mGo = (Button)activity.findViewById(R.id.go);
        mSearchBarLayout = (LinearLayout) activity.findViewById(R.id.search_bar_view);
        
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
	
	public void hide() {
		mSearchBarLayout.setVisibility(View.GONE);
	}
	
	private void doSearch() {
		String query = mQuery.getText().toString();

		if (!TextUtils.isEmpty(query)) {
//			SearchListActivity.startQuery(query);
//			SearchListActivity.handleMp3ListIntent(mActivity, query);
			((SearchListActivity)mActivity).startQuery(query);
		}
	}
}
