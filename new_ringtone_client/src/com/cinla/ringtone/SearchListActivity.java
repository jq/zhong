package com.cinla.ringtone;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import com.cinla.imageloader.ImageLoaderHandler;
import com.cinla.ringtone.ListStatusView.Status;

import android.R.integer;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SimpleAdapter.ViewBinder;

public class SearchListActivity extends ListActivity {
	
	private ProgressBar mProgressBar;
	private TextView mSearchMessage;
	private Button mRetryButton;
//	private SearchListFooterView mFooter;
	
	private SearchBar mSearchBar;
	
	private Mp3ListAdapter mAdapter;
	
	private Mp3ListWrapper mData;
	
	private MusicParser mFetcher;
	private FetchMp3ListTask mFetchMp3ListTask;
	
	private Handler mHandler = new Handler();

	private int mSearchType;
	private String mSearchKey;
	
	private int mStartPos;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_list_activity);
		
		mProgressBar = (ProgressBar) findViewById(R.id.search_progress);
		mSearchMessage = (TextView) findViewById(R.id.search_message);
		mRetryButton = (Button) findViewById(R.id.retry_button);
		mRetryButton.setOnClickListener(new RetryButtonClickListener());
		
//		mFooter = new SearchListFooterView(this);
//		getListView().addFooterView(mFooter);
//		mFooter.setFocusable(false);
//		mFooter.getBtnNext().setOnClickListener(new NextButtonClickListener());
//		mFooter.getBtnPre().setOnClickListener(new PrevButtonClickListener());
		
		mSearchBar = new SearchBar(this);
		adjustSearchType(getIntent());
		if (mSearchType==Constant.TYPE_KEY) {
			mSearchBar.setQueryKeyWord(mSearchKey);
		}
		if (mSearchType!=Constant.TYPE_EMPTY) {
			startQuery(getIntent().getStringExtra(Constant.QUERY));
			setLoadingStatus();
		} else if (mSearchType==Constant.TYPE_EMPTY){
			setHintStatus();
		} 
	}
	
	private void adjustSearchType(Intent intent) {
		Utils.D("in on NewIntent()");
		mSearchType = intent.getIntExtra(Constant.SEARCH_TYPE, -1);
		mSearchKey = intent.getStringExtra(Constant.QUERY);
		switch (mSearchType) {
		case Constant.TYPE_EMPTY:
			
			break;
		case Constant.TYPE_KEY:
			
			break;
		case Constant.TYPE_ARTIST:
			
			mSearchBar.hide();
			break;
		case Constant.TYPE_CATEGORY:
			
			mSearchBar.hide();
			break;
		case Constant.TYPE_NEWEST:
			
			mSearchBar.hide();
			break;
		case Constant.TYPE_TOP_DOWNLOAD:
			
			mSearchBar.hide();
			break;
		default:
			break;
		}
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		adjustSearchType(intent);
		Utils.D("in onNewIntent of SearchListActivity");
		Utils.D("onNewIntent: "+intent.getStringExtra(Constant.QUERY));
		startQuery(intent.getStringExtra(Constant.QUERY));
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
//		super.onListItemClick(l, v, position, id);
		MusicInfo clickedMusicInfo = mData.get(position);
		MusicPageActivity.startMusicPageActivity(this, clickedMusicInfo);
	}

	public void startQuery(String keyWord) {
		mStartPos = 0;
		continueFetch(keyWord, true);
	}
	
	public void continueFetch(String keyWord, boolean isNext) {
		if (mFetchMp3ListTask != null) {
			mFetchMp3ListTask.cancel(true);
		}
		mFetchMp3ListTask = new FetchMp3ListTask(isNext);
		mFetchMp3ListTask.execute(keyWord);
	}

	public void handleSearchResults(ArrayList<MusicInfo> mp3List) {
		if (mAdapter == null) {
			mAdapter = new Mp3ListAdapter(SearchListActivity.this, R.layout.result_item);
			setListAdapter(mAdapter);
		}
		if (mp3List != null) {
			if (mData == null) {
				mData = new Mp3ListWrapper();
			}
			mData.append(mp3List);
			mAdapter.notifyDataSetChanged();
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

	private final class Mp3ListAdapter extends BaseAdapter {

		private int mResource;
		private LayoutInflater mInflater;
		private ListStatusView.Status mStatus;

		public Mp3ListAdapter(Context context, int resource) {
			mResource = resource;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			if (mData == null) {
				return 0;
			} else {
				return mData.size();
			}
		}

		@Override
		public Object getItem(int position) {
			if (mData == null) {
				return null;
			} else {
				return mData.get(position);
			}
		}

		@Override
		public long getItemId(int position) {			
			if (mData == null) {
				return -1;
			} else {
				return position;
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			if (position == mData.size()-1) {
//				ListStatusView footerView = (ListStatusView) convertView;
				ListStatusView footerView = null;
//				if (footerView == null) {
					footerView = (ListStatusView) mInflater.inflate(R.layout.liststatus, null);
					TextView text = (TextView)footerView.findViewById(R.id.prompt);
					text.setText(R.string.loading_more);
//				}
				if (mStatus == ListStatusView.Status.LOADING) {
					footerView.setLoadingStatus();
				} else if (mStatus == ListStatusView.Status.ERROR) {
					footerView.setErrorStatus(new RetryLoadingClickListener());
				}
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						continueFetch(mSearchBar.getQuery().trim(), true);
					}
				});
				return footerView;
			}
			
			Object item = mData.get(position);
//			if (convertView == null) {
				v = mInflater.inflate(mResource, parent, false);
//			} else {
//				v = convertView;
//			}
			MusicInfo info = (MusicInfo) item;
			ImageView imageView = (ImageView) v.findViewById(R.id.image);
//			new ImageLoader(info.getmImageUrl(), imageView).startLoadImage();
			
			if (!NetUtils.isInCache(info.getmImageUrl())) {
				imageView.setBackgroundResource(R.drawable.image_loading);
			}

			com.cinla.imageloader.ImageLoader.initialize(SearchListActivity.this);
			com.cinla.imageloader.ImageLoader.start(info.getmImageUrl(), imageView);
			
			((TextView) v.findViewById(R.id.title)).setText(info.getmTitle());
			((TextView) v.findViewById(R.id.artist)).setText(info.getmArtist());
			((TextView) v.findViewById(R.id.download_count)).setText(getString(R.string.download_count)+" "+Integer.toString(info.getmDownloadCount()));
            ((RatingBar) v.findViewById(R.id.ratebar_indicator)).setRating((float)info.getmRate()/20);
            return v;
		}
		
		protected void setStatus(ListStatusView.Status status) {
			mStatus = status;
		}
	}
	
	public class FetchMp3ListTask extends AsyncTask<String, Void, ArrayList<MusicInfo>> {

		private int mStartPosTemp;
		private boolean mIsNext;
		
		public FetchMp3ListTask(boolean isNext) {
			mIsNext = isNext;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			setLoadingStatus();
			mStartPosTemp = mStartPos;
			if (mData!=null) {
				mStartPosTemp += mData.size();
			}
			if (mAdapter != null) {
				mAdapter.setStatus(com.cinla.ringtone.ListStatusView.Status.LOADING);
			}
//			if (mData != null) {
//				mData.clear();
//			}
		}

		@Override
		protected ArrayList<MusicInfo> doInBackground(String... params) {
//			Utils.D(params[0]);
			ArrayList<MusicInfo> results = null;
			if (mIsNext) {
				results = getNextMp3List(params[0]);
			} else {
				results = getPrevMp3List(params[0]);
			}
			return results;
		}

		@Override
		protected void onPostExecute(ArrayList<MusicInfo> result) {
			super.onPostExecute(result);
			if ((mData!=null && mData.size()==0)||mData==null) {
				if (result == null) {
					setErrorStatus();
					return;
				}
				if (result.size() == 0) {
					setNoResultStatus();
					return;
				}
			} else {
				if (result == null) {
					mAdapter.setStatus(com.cinla.ringtone.ListStatusView.Status.ERROR);
					mAdapter.notifyDataSetChanged();
				}
			}
			mStartPos = mStartPosTemp;
			SearchListActivity.this.handleSearchResults(result);
		}
		
		private ArrayList<MusicInfo> getNextMp3List(String keyWord) {
			ArrayList<MusicInfo> results = null;
			if (mSearchType==Constant.TYPE_KEY || mSearchType==Constant.TYPE_EMPTY) {
				Utils.D("Position: "+mStartPosTemp);
				results = mFetcher.getMusicListByQueryKey(keyWord, mStartPosTemp);
			} else if (mSearchType == Constant.TYPE_ARTIST) {
				results = mFetcher.getMusicListByArtist(keyWord, mStartPosTemp);
			} else if (mSearchType == Constant.TYPE_CATEGORY) {
				results = mFetcher.getMusicListByCategory(keyWord, mStartPosTemp);
			} else if (mSearchType == Constant.TYPE_NEWEST) {
				results = mFetcher.getMusicListByAddDate(mStartPosTemp);
			} else if (mSearchType == Constant.TYPE_TOP_DOWNLOAD) {
				results = mFetcher.getMusicListByDownloadCount(mStartPosTemp);
			}
			return results;
		}
		
		private ArrayList<MusicInfo> getPrevMp3List(String keyWord) {
			mStartPosTemp -= Constant.EACH_MAX_RESULTS_NUM;
			if (mStartPosTemp < 0) {
				mStartPosTemp = 0;
			}
			ArrayList<MusicInfo> results = null;
			if (mSearchType==Constant.TYPE_KEY || mSearchType==Constant.TYPE_EMPTY) {
				results = mFetcher.getMusicListByQueryKey(keyWord, mStartPosTemp);
			} else if (mSearchType == Constant.TYPE_ARTIST) {
				results = mFetcher.getMusicListByArtist(keyWord, mStartPosTemp);
			} else if (mSearchType == Constant.TYPE_CATEGORY) {
				results = mFetcher.getMusicListByCategory(keyWord, mStartPosTemp);
			} else if (mSearchType == Constant.TYPE_NEWEST) {
				results = mFetcher.getMusicListByAddDate(mStartPosTemp);
			} else if (mSearchType == Constant.TYPE_TOP_DOWNLOAD) {
				results = mFetcher.getMusicListByDownloadCount(mStartPosTemp);
			}
			return results;
		}
	}
	
	private void setLoadingStatus() {
		mProgressBar.setVisibility(View.VISIBLE);
		mRetryButton.setVisibility(View.GONE);
		mSearchMessage.setVisibility(View.VISIBLE);
		mSearchMessage.setText(SearchListActivity.this.getString(R.string.please_wait_search)+" "+mSearchBar.getQuery().trim());
	}
	
	private void setErrorStatus() {
		mProgressBar.setVisibility(View.GONE);
		mRetryButton.setVisibility(View.VISIBLE);
		mSearchMessage.setText(R.string.load_error_message);
		mSearchMessage.setVisibility(View.VISIBLE);
	}
	
	private void setHintStatus() {
		mProgressBar.setVisibility(View.GONE);
		mRetryButton.setVisibility(View.GONE);
		mSearchMessage.setVisibility(View.VISIBLE);
		mSearchMessage.setText(R.string.search_hint_message);
	}
	
	private void setNoResultStatus() {
		mProgressBar.setVisibility(View.GONE);
		mRetryButton.setVisibility(View.GONE);
		mSearchMessage.setVisibility(View.VISIBLE);
		mSearchMessage.setText(R.string.no_result);
	}
	
	private class RetryButtonClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			SearchListActivity.this.startQuery(mSearchBar.getQuery().trim());
		}
	}
	
	private class RetryLoadingClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			mAdapter.setStatus(Status.LOADING);
			mAdapter.notifyDataSetChanged();
			continueFetch(mSearchBar.getQuery().trim(), true);
		}
	}
	
	private class PrevButtonClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			continueFetch(mSearchBar.getQuery().trim(), false);
		}
	}
	
	private class NextButtonClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			continueFetch(mSearchBar.getQuery().trim(), true);
		}
	}

	public static void startQeuryByKey(Context context, String keyWord) {
		Intent intent = new Intent(context, SearchListActivity.class);
		intent.putExtra(Constant.SEARCH_TYPE, Constant.TYPE_KEY);
		intent.putExtra(Constant.QUERY, keyWord);
		context.startActivity(intent);
	}

	public static void startQueryByCategory(Context context, String keyWord) {
		Intent intent = new Intent(context, SearchListActivity.class);
		intent.putExtra(Constant.SEARCH_TYPE, Constant.TYPE_CATEGORY);
		intent.putExtra(Constant.QUERY, keyWord);
		context.startActivity(intent); 
	}

	public static void startQueryByArtist(Context context, String keyWord) {
		Intent intent = new Intent(context, SearchListActivity.class);
		intent.putExtra(Constant.SEARCH_TYPE, Constant.TYPE_ARTIST);
		intent.putExtra(Constant.QUERY, keyWord);
		context.startActivity(intent);
	}

	public static void startQueryByDownloadcount(Context context) {
		Intent intent = new Intent(context, SearchListActivity.class);
		intent.putExtra(Constant.SEARCH_TYPE, Constant.TYPE_TOP_DOWNLOAD);
		intent.putExtra(Constant.QUERY, "");
		context.startActivity(intent);
	}
	
	public static void startQueryByDate(Context context) {
		Intent intent = new Intent(context, SearchListActivity.class);
		intent.putExtra(Constant.SEARCH_TYPE, Constant.TYPE_NEWEST);
		intent.putExtra(Constant.QUERY, "");
		context.startActivity(intent);
	}

}
