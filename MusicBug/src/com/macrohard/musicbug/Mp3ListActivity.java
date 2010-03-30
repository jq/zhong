package com.macrohard.musicbug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.macrohard.musicbug.R;
import com.macrohard.musicbug.Mp3FetcherInterface.Mp3FetcherException;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class Mp3ListActivity extends Activity implements ListFooterView.RetryNetworkInterface {
	private static final String TAG = Debug.TAG;
	private static final int DIALOG_WAITING_FOR_SERVER = 1; 

	// Shared by multiple threads.
	private volatile Mp3ListWrapper mData;
	
	private ProgressDialog mProgressDialog;
	private ListView mListView;

	private Mp3ListAdapter mAdapter;

	private boolean mHasMoreData = true;
	
	private Mp3FetcherInterface mFetcher;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case DIALOG_WAITING_FOR_SERVER: {
               mProgressDialog = new ProgressDialog(this);
               mProgressDialog.setMessage(getString(R.string.waiting_for_server));
               mProgressDialog.setIndeterminate(true);
               mProgressDialog.setCancelable(true);
               return mProgressDialog;
			}
		}
		return null;
	}
	
	@Override
	public void retryNetwork() {
		fetchNextMp3ListBatch();
		mAdapter.setStatus(ListFooterView.Status.LOADING);
		mAdapter.notifyDataSetChanged();
	}

	private void fetchNextMp3ListBatch() {
		if (mFetcher != null) {
			new FetchMp3ListTask().execute();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		showDialog(DIALOG_WAITING_FOR_SERVER);
		fetchNextMp3ListBatch();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		Debug.D("Mp3ListActivity onCreate()");

		setContentView(R.layout.music_list);

        mListView = (ListView)findViewById(R.id.post_list);
		mListView.setTextFilterEnabled(true);
		mListView.setFocusable(true);
		mListView.setItemsCanFocus(true);
		
		mData = new Mp3ListWrapper();
		
		mAdapter = new Mp3ListAdapter(
				Mp3ListActivity.this,
				R.layout.music_item
				);
		
		mListView.setAdapter(mAdapter);
		mListView.setSelection(0);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				if (mData == null || position == mData.size())
					return;
				
				final MP3Info mp3 = mData.get(position);
		
				showDialog(DIALOG_WAITING_FOR_SERVER);
				new FetchMp3LinkTask().execute(mp3);
            }
        });


		String  keyWords = getIntent().getStringExtra(Const.Key);
		if (!TextUtils.isEmpty(keyWords)) {
			mFetcher = new SogoMp3Fetcher(this, keyWords);
		}
	}

	private class Mp3InfoHolder {
		public MP3Info mp3;
		public String downloadLink;
	};

	private class FetchMp3LinkTask extends AsyncTask<MP3Info, Void, Mp3InfoHolder> {
		protected Mp3InfoHolder doInBackground(MP3Info... mp3s) {
			String mp3Link = null;
			try {
				// TODO: This is a temporary hack. Consider using the fetcher interface when it is finished.
				mp3Link = SogoMp3Fetcher.getDownloadLink(mp3s[0]); 
			} catch (IOException e) {
				mp3Link = null;
				e.printStackTrace();
			}
			Mp3InfoHolder holder = new Mp3InfoHolder();
			holder.mp3 = mp3s[0];
			holder.downloadLink = mp3Link;
			return holder;
		}

		protected void onPostExecute(Mp3InfoHolder info) {
			MP3Info mp3 = info.mp3;
			String mp3Link = info.downloadLink;
			
			if (mp3Link == null) {
				mProgressDialog.dismiss();

				Toast.makeText(Mp3ListActivity.this, R.string.no_result, Toast.LENGTH_SHORT).show();
				return;
			}

		  	float rate = mp3.getRate();

		    Intent intent = new Intent(Mp3ListActivity.this, MusicPage.class);
		  	intent.putExtra(Const.MP3RATE, ((Float)rate).toString());
		    intent.putExtra(Const.MP3LOC, mp3Link);
		    intent.putExtra(Const.MP3TITLE, mp3.getTitle());
		    intent.putExtra(Const.MP3SINGER, mp3.getArtist());
		    startActivity(intent);
		    
			mProgressDialog.dismiss();
		}
	}

	private class FetchMp3ListTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected void onPostExecute(Boolean result) {
			mProgressDialog.dismiss();
			
			if (result) {
				mAdapter.setStatus(ListFooterView.Status.LOADED);
				mAdapter.notifyDataSetChanged();
			} else {
				mAdapter.setStatus(ListFooterView.Status.ERROR);
				mAdapter.notifyDataSetInvalidated();
			}
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			ArrayList<MP3Info> mp3List;
			try {
				mp3List = mFetcher.getNextListBatch();
				if (mp3List == null) {
					// Some error.
					return false;
				} else {
					if (mp3List.size() > 0) {
						mData.append(mp3List);
						return true;
					} else {
						mHasMoreData = false;
						return true;
					}
				}
			} catch (Mp3FetcherException e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	
	// A thread safe wrapper around ArrayList<MP3Info>.
	private final class Mp3ListWrapper {
		private ArrayList<MP3Info> mMp3List;
		
		private ReadWriteLock lock = new ReentrantReadWriteLock();
		private Lock r = lock.readLock();
		private Lock w = lock.writeLock();

		public Mp3ListWrapper() {
			mMp3List = new ArrayList<MP3Info>();
		}
		
		public void add(MP3Info info) {
			w.lock();
			try {
				mMp3List.add(info);
			} finally {
				w.unlock();
			}
		}
		
		public void append(ArrayList<MP3Info> mp3List) {
			w.lock();
			try {
				for (int i = 0; i < mp3List.size(); ++i) {
					add(mp3List.get(i));
				}
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
		
		public MP3Info get(int i) {
			r.lock();
			try {
				return mMp3List.get(i);
			} finally {
				r.unlock();
			}
		}
		
		public void remove(int i) {
			r.lock();
			try {
				mMp3List.remove(i);
			} finally {
				r.unlock();
			}
		}

	}
	
    private final class Mp3ListAdapter extends BaseAdapter {

        private final static int VIEW_TYPE_NORMAL = 0;
        private final static int VIEW_TYPE_FOOTER = 1;
    	
    	private int mResource;
    	private LayoutInflater mInflater;
    	
    	private ListFooterView.Status mStatus;

    	public Mp3ListAdapter(Context context, int resource) {
    		mResource = resource;
    		mInflater = (LayoutInflater)context.getSystemService(
    				Context.LAYOUT_INFLATER_SERVICE);
    		mStatus = ListFooterView.Status.LOADED;
    	}
    	
    	public void setStatus(ListFooterView.Status status) {
    		mStatus = status;
    	}
    	
    	@Override
    	public int getCount() {
    		boolean showFooter =
    			mStatus == ListFooterView.Status.ERROR ||
    			mStatus == ListFooterView.Status.LOADING;
    		
    		int footerCount = showFooter ? 1 : 0;
    		
    		if (mData == null)
    			return footerCount;
    		return mData.size() + footerCount;
    	}
    	
		@Override
		public Object getItem(int position) {
			if (mData == null)
				return null;
			
			if (position < mData.size())
				return mData.get(position);
			return null;  // footer.
		}

		@Override
		public long getItemId(int position) {
			if (mData == null)
				return -1;
			
			if (position < mData.size())
				return position;
			return -1;  // footer.
		}
		
		@Override
	    public int getViewTypeCount() {
	        return 2;
	    }

	    @Override
	    public int getItemViewType(int position) {
	        if (position == mData.size()) {
	            return VIEW_TYPE_FOOTER;
	        }
	        return VIEW_TYPE_NORMAL;
	    }

		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			boolean isFooter = position == mData.size();
			
			if (isFooter) {
				ListFooterView footerView = (ListFooterView)convertView;
				if (footerView == null) {
					footerView = (ListFooterView)mInflater.inflate(
							R.layout.list_footer, null);
					TextView text = (TextView)footerView.findViewById(R.id.footer_text);
					text.setText(R.string.loading);
				}
				footerView.bind(mStatus, Mp3ListActivity.this);
				return footerView;
			}
			
			View v;
			Object item = mData.get(position);
			if (convertView == null) {
				v = mInflater.inflate(mResource, parent, false);
			} else {
				v = convertView;
			}

			v.setBackgroundResource(
					position % 2 == 0 ? R.color.mp3_item_color
							: R.color.mp3_item_color2);

			MP3Info info = (MP3Info)item;
			((TextView)v.findViewById(R.id.title)).setText(info.getTitle());
			((TextView)v.findViewById(R.id.artist)).setText(info.getArtist());

			if (mHasMoreData && position == mData.size() - 1 &&
				mStatus == ListFooterView.Status.LOADED) {
				Debug.D("Loading more");
				fetchNextMp3ListBatch();
				setStatus(ListFooterView.Status.LOADING);
				notifyDataSetChanged();
			}

			return v;
		}
    }

	public static void handleMp3ListIntent(Context context,
			String keyWords) {
		Intent intent = new Intent(context, Mp3ListActivity.class);
		intent.putExtra(Const.Key, keyWords);
    	context.startActivity(intent);
	}
}
