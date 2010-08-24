package com.util;

import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.R;

import android.app.Activity;
import android.os.Debug;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

public class SearchBar {

	protected Activity mActivity;
	protected EditText mQuery;
	protected ImageButton mGo;
	
	public SearchBar(Activity activity) {
		mActivity = activity;
        mQuery = (EditText)activity.findViewById(R.id.q);
        mGo = (ImageButton)activity.findViewById(R.id.go);
        
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
			return StringUtils.removeIllegalChars(mQuery.getText().toString());
		}
		return null;
	}
	
	private void doSearch() {
		String query = mQuery.getText().toString();
		query = StringUtils.removeIllegalChars(query);
		com.util.Utils.D("start search");

		if (!TextUtils.isEmpty(query)) {
	    	//Debug.startMethodTracing("blaster");
			SearchResultActivity.handleMp3ListSimpleIntent(mActivity, query);
		}
	}
}
