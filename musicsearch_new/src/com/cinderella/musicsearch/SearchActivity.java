package com.cinderella.musicsearch;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SearchActivity extends ListActivity {
	
	public static Mp3ListWrapper sData;
	
	private static MusicSearcher sFetcher;
	
	private static FetchMp3ListTask sFetchMp3ListTask;
	
	private static SearchActivity sSearchActivity;
	
	private static String sQuery;
	
	private Mp3ListAdapter mAdapter;
	
	private TextView mSearchMessage;
	private ProgressBar mProgressBar;
	private Button mRetryButton;
	private SearchListFooterView mFooter;
	
	private SearchBar mSearchBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sSearchActivity = this;
		setContentView(R.layout.search_list);
		
		mProgressBar = (ProgressBar) findViewById(R.id.search_progress);
		mSearchMessage = (TextView) findViewById(R.id.search_message);
		mRetryButton = (Button) findViewById(R.id.retry_button);
		
		mFooter = new SearchListFooterView(this);
		getListView().addFooterView(mFooter);
		mFooter.setFocusable(false);
		mSearchBar = new SearchBar(this);
		mFooter.getBtnPre().setOnClickListener(new onPrevClickListener());
		mFooter.getBtnNext().setOnClickListener(new onNextClickListener());
		mRetryButton.setOnClickListener(new onRetryClickListener());
		setHintStatus();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(this, MusicPageActivity.class);
		intent.putExtra(Const.INDEX, position);
		startActivity(intent);
	}

	private void handleSearchResult(ArrayList<MusicInfo> mp3List) {
    	if (sFetcher.getCurPage() == 1) {
    		mFooter.getBtnPre().setEnabled(false);
    	} else {
    		mFooter.getBtnPre().setEnabled(true);
    	}
		if (mAdapter == null) {
	        mAdapter = new Mp3ListAdapter(SearchActivity.this, R.layout.result_item);
			setListAdapter(mAdapter);
		}
		if (mp3List != null) {
			if (sData == null) {
				sData = new Mp3ListWrapper();
			}
			if (mp3List.size() > 0) {
				sData.append(mp3List);
			} else {
				setNoResultStatus();
			}
            mAdapter.notifyDataSetChanged();
		} else {
			setErrorStatus();
			mAdapter.notifyDataSetChanged();
		}
	}
    
	private static void fetchNextMp3ListBatch() {
		if (sFetchMp3ListTask != null)
			sFetchMp3ListTask.cancel(true);
		sFetchMp3ListTask = new FetchMp3ListTask(true);
		sFetchMp3ListTask.execute();
	}
	
	private static void fetchPrevMp3ListBatch() {
		if (sFetchMp3ListTask != null)
			sFetchMp3ListTask.cancel(true);
		sFetchMp3ListTask = new FetchMp3ListTask(false);
		sFetchMp3ListTask.execute();
	}
	
	private final class Mp3ListAdapter extends BaseAdapter {

		private int mResource;
		private LayoutInflater mInflater;
		
		public Mp3ListAdapter(Context context, int resource) {
			mResource = resource;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			if (sData == null) {
				return 0;
			} else {
				return sData.size();
			}
		}

		@Override
		public Object getItem(int position) {
			if (sData == null) {
				return null;
			} else {
				return sData.get(position);
			}
		}

		@Override
		public long getItemId(int position) {			
			if (sData == null) {
				return -1;
			} else {
				return position;
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			Object item = sData.get(position);
			if (convertView == null) {
				v = mInflater.inflate(mResource, parent, false);
			} else {
				v = convertView;
			}
			MusicInfo info = (MusicInfo) item;
			((TextView) v.findViewById(R.id.title)).setText(info.getTitle());
			((TextView) v.findViewById(R.id.artist)).setText(info.getArtist());
            ((TextView) v.findViewById(R.id.size)).setText(info.getDisplayFileSize());
            return v;
		}	
	}
	
	// A thread safe wrapper around ArrayList<MP3Info>.
	public final class Mp3ListWrapper {
		private ArrayList<MusicInfo> mMp3List;

		private ReadWriteLock lock = new ReentrantReadWriteLock();
		private Lock r = lock.readLock();
		private Lock w = lock.writeLock();

		public Mp3ListWrapper() {
			mMp3List = new ArrayList<MusicInfo>();
		}

		public void clear() {
			w.lock();
			try {
				mMp3List.clear();
				if (mAdapter != null)
					mAdapter.notifyDataSetInvalidated();
			} finally {
				w.unlock();
			}
		}

		public void add(MusicInfo info) {
			w.lock();
			try {
				mMp3List.add(info);
				if (mAdapter != null)
					mAdapter.notifyDataSetChanged();
			} finally {
				w.unlock();
			}
		}

		public void append(ArrayList<MusicInfo> mp3List) {
			w.lock();
			try {
				mMp3List.addAll(mp3List);
				mAdapter.notifyDataSetChanged();
			} finally {
				w.unlock();
			}
		}

		public int size() {
			r.lock();
			try {
				return mMp3List.size();
			} finally {
				r.unlock();
			}
		}

		public MusicInfo get(int i) {
			r.lock();
			try {
				return mMp3List.get(i);
			} finally {
				r.unlock();
			}
		}
	}
	
	private static class FetchMp3ListTask extends AsyncTask<Void, Void, ArrayList<MusicInfo>> {
		
		boolean mIsNext;
		
		public FetchMp3ListTask(boolean isNext) {
			super();
			mIsNext = isNext;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (sData != null) { 
				sData.clear();
			}
			sSearchActivity.setLoadingStatus();
		}

		@Override
		protected void onPostExecute(ArrayList<MusicInfo> mp3List) {
			sFetchMp3ListTask = null;
			if (sSearchActivity != null) {
				sSearchActivity.handleSearchResult(mp3List);
			}
		}

		@Override
		protected ArrayList<MusicInfo> doInBackground(Void... params) {
			if (mIsNext) {
				return sFetcher.getNextResultList();
			} else {
				return sFetcher.getPrevResultList();
			}
		}
	}
	
	private static class onPrevClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			sSearchActivity.fetchPrevMp3ListBatch();
		}
	}
	
	private static class onNextClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			sSearchActivity.fetchNextMp3ListBatch();
		}
	}
	
	private static class onRetryClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			sSearchActivity.fetchPrevMp3ListBatch();
		}
	}
	
	public void notifyDataSetInvalidated() {
		if (mAdapter != null)
			mAdapter.notifyDataSetInvalidated();
	}
	
	public static void startQuery(String keyWords) {
		if (!TextUtils.isEmpty(keyWords)) {
			sSearchActivity.setLoadingStatus();
			sQuery = keyWords;
			sData = null;
			if (sSearchActivity != null)
				sSearchActivity.notifyDataSetInvalidated();
            sFetcher = new MusicSearcher();
			sFetcher.setQuery(keyWords);
			fetchNextMp3ListBatch();
		} else {
			sFetchMp3ListTask = null;
			sFetcher = null;
		}
	}
	
    public static void handleMp3ListIntent(Context context, String keyWords) {
		Intent intent = new Intent(context, SearchActivity.class);
		intent.putExtra(Const.QUERY, keyWords);
		context.startActivity(intent);
	}
    
    public void setHintStatus() {
    	mProgressBar.setVisibility(View.GONE);
    	mSearchMessage.setVisibility(View.VISIBLE);
    	mRetryButton.setVisibility(View.GONE);
    	mSearchMessage.setText(R.string.search_hit);
    }
    
    public void setLoadingStatus() {
    	mProgressBar.setVisibility(View.VISIBLE);
    	mSearchMessage.setVisibility(View.VISIBLE);
    	mRetryButton.setVisibility(View.GONE);
    	mSearchMessage.setText(this.getString(R.string.searching_wait) + sQuery);
    }
    
    public void setNoResultStatus() {
    	mProgressBar.setVisibility(View.GONE);
    	mSearchMessage.setVisibility(View.VISIBLE);
    	mRetryButton.setVisibility(View.GONE);
    	mSearchMessage.setText(R.string.no_result_sorry);
    }
    
    public void setErrorStatus() {
    	mProgressBar.setVisibility(View.VISIBLE);
    	mSearchMessage.setVisibility(View.VISIBLE);
    	mRetryButton.setVisibility(View.VISIBLE);
    	mSearchMessage.setText(R.string.network_error_retry);
    }
}
