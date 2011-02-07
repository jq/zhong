package com.cinla.ringtone;

import com.latest.ringtone.R;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class SearchBar {

	private Activity mActivity;
	private AutoCompleteTextView mQuery;
	private Button mGo;
	LinearLayout mSearchBarLayout;
	private Handler mHandler = new Handler();
	
	private static final String CODING = "utf-8";
	
	public SearchBar(Activity activity) {
		mActivity = activity;
        mQuery = (AutoCompleteTextView)activity.findViewById(R.id.query_key);
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
        mQuery.setThreshold(1);
        DbAdapter dbAdapter = new DbAdapter(mActivity);
        try {
        	SearchAdapter myCursorAdapterSearch = new SearchAdapter(mActivity, dbAdapter.getHistoryByType(DbAdapter.TYPE_SEARCH), DbAdapter.TYPE_SEARCH);
        	mQuery.setAdapter(myCursorAdapterSearch);
        } catch (Exception e) {
			
		} finally {
			dbAdapter.close();
		}
        
        mGo.setOnClickListener(new OnClickListener() {   
            @Override
            public void onClick(View v) {
            	doSearch();
            }
        });
	}
	
	public void setHint() {
		new SetSearchBarHintTask().execute(null);
	}
	
	public void setQueryKeyWord(String keyWord) {
		mQuery.setText(keyWord);
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
		String query = mQuery.getText().toString().trim();
		if (query.length() == 0) {
			Toast.makeText(mActivity, R.string.input_key, Toast.LENGTH_SHORT).show();
			return;
		}
		DbAdapter db = new DbAdapter(mActivity);
		db.intsertHistory(query, DbAdapter.TYPE_SEARCH);
		db.close();
		if (!TextUtils.isEmpty(query)) {
			SearchListActivity.startQeuryByKey(mActivity, query);
		}
	}
	
	private class SetSearchBarHintTask extends AsyncTask<Void, Void, Long> {
		@Override
		protected Long doInBackground(Void... params) {
			String response;
			try {
				response = NetUtils.fetchHtmlPage(Constant.BASE_URL+Constant.COUNT_URL, CODING, Constant.ONE_WEEK);
				Utils.D("*************************response of get all: "+response);
			} catch (Exception e) {
				Utils.D("*************************Exception in get all. ");
				Utils.D(e.getMessage());
				return null;
			}
			if (response==null || response.length()==0) {
				return null;
			}
			return Long.parseLong(response);
		}
		@Override
		protected void onPostExecute(final Long result) {
			super.onPostExecute(result);
			if (result == null) {
				return;
			}
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					if (mQuery.getText().toString().trim().length()==0) {
						mQuery.setHint(mActivity.getString(R.string.total_ringtones1)+" "+result.toString()+" "+mActivity.getString(R.string.total_ringtones2));
					}
				}
			});

		}
		
	}
}
