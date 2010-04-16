package com.popczar.music;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.popczar.music.download.DownloadActivity;
import com.popczar.music.download.DownloadInfo;
import com.popczar.music.download.DownloadService;

import com.popczar.music.R;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class SearchResultActivity extends Activity {
	private static final String TAG = Utils.TAG;
	private static final int DIALOG_WAITING_FOR_SERVER = 1; 
	
	private static final int MENU_PREVIEW = Menu.FIRST;
	private static final int MENU_DOWNLOAD = Menu.FIRST + 1;
	
	@SuppressWarnings("unused")
	private SearchBar mSearch;

	// Shared by multiple threads.
	private volatile Mp3ListWrapper mData;
	
	private ProgressDialog mProgressDialog;
	private ListView mListView;

	private Mp3ListAdapter mAdapter;

	private boolean mHasMoreData = true;
	
	private SogouMusicSearcher mFetcher;
	private DownloadService mDownloadService;
	
	private static ProgressDialog sStreaming;
	private static MediaPlayer sPlayer = new MediaPlayer();;
	
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mDownloadService = ((DownloadService.LocalBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mDownloadService = null;
        }
    };

	@Override
	protected Dialog onCreateDialog(int id) {
		Utils.D("onCreateDialog() " + id);
		switch (id) {
		case DIALOG_WAITING_FOR_SERVER: {
			if (mProgressDialog == null) {
				mProgressDialog = new ProgressDialog(this);
				mProgressDialog.setMessage(getString(R.string.wait));
				mProgressDialog.setIndeterminate(true);
				mProgressDialog.setCancelable(true);
			}
			return mProgressDialog;
		}
		}
		return null;
	}

	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_result_menu, menu);
        return true;
    }
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.dowloads:
			Intent intent = new Intent(SearchResultActivity.this, DownloadActivity.class);
			startActivity(intent);
        	return true;
        }
        return false;
    }	
	private OnClickListener mRetryListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			fetchNextMp3ListBatch();
			mAdapter.setStatus(ListStatusView.Status.LOADING);
			mAdapter.notifyDataSetChanged();
		}
		
	};
	
	private void fetchNextMp3ListBatch() {
		if (mFetcher != null) {
			new FetchMp3ListTask().execute();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Utils.D("onResume");
	}

	private void startQuery(Intent intent) {
		String  keyWords = intent.getStringExtra(Constants.QUERY);
		if (!TextUtils.isEmpty(keyWords)) {
			if (mData != null) {
				mData.clear();
			}
			mFetcher = new SogouMusicSearcher(keyWords);
		}
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		startQuery(intent);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Utils.D("Mp3ListActivity onCreate()");

		setContentView(R.layout.result_list);
		
		bindService(new Intent(this, DownloadService.class),
				mConnection, Context.BIND_AUTO_CREATE);

		mSearch = new SearchBar(this);
		
        mListView = (ListView)findViewById(R.id.result_list);
		mListView.setTextFilterEnabled(true);
		mListView.setFocusable(true);
		mListView.setItemsCanFocus(true);
		
		mData = new Mp3ListWrapper();
		
		mAdapter = new Mp3ListAdapter(
				SearchResultActivity.this,
				R.layout.result_item
				);
		
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
			    download(mData.get(position));
				//Toast.makeText(SearchResultActivity.this,
				//		getString(R.string.music_option_prompt), Toast.LENGTH_SHORT);
			}
		});
		
		mListView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				menu.add(0, MENU_PREVIEW, 0, R.string.preview);
				menu.add(0, MENU_DOWNLOAD, 0, R.string.download);
			}
		});

		startQuery(getIntent());
		showDialog(DIALOG_WAITING_FOR_SERVER);
		fetchNextMp3ListBatch();
		
		// TODO: Too hacky.
		if (sStreaming != null) {
			sStreaming  = new ProgressDialog(SearchResultActivity.this);
			sStreaming.setTitle(R.string.streaming);
			sStreaming.setMessage(getString(R.string.wait));
			sStreaming.setIndeterminate(true);
			sStreaming.setCancelable(true);
			sStreaming.setButton(getString(R.string.stop), new DialogInterface.OnClickListener() {			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					sPlayer.stop();
					sStreaming = null;
				}
			});
			sStreaming.show();
		}
	}
	
	
	private void playMusic(MusicInfo mp3) {
		if (mp3.getDownloadUrl().startsWith("http:")) {
			sPlayer.reset();
			try {
				sPlayer.setDataSource(mp3.getDownloadUrl());
			} catch (IllegalArgumentException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IllegalStateException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						sPlayer.prepare();
						sPlayer.start();
						sPlayer.setOnCompletionListener(new OnCompletionListener () {
							@Override
							public void onCompletion(MediaPlayer mp) {
							}
						});
					} catch (IllegalArgumentException e) {
					} catch (IllegalStateException e) {
					} catch (IOException e) {
					}       

				}

			}).start();
		}		
	}
	
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item
				.getMenuInfo();
		MusicInfo mp3 = mData.get(menuInfo.position);
		switch (item.getItemId()) {
		case MENU_PREVIEW: {
			if (sStreaming == null) {
				sStreaming  = new ProgressDialog(SearchResultActivity.this);
				sStreaming.setTitle(R.string.streaming);
				sStreaming.setMessage(getString(R.string.wait));
				sStreaming.setIndeterminate(true);
				sStreaming.setCancelable(true);
				sStreaming.setButton(getString(R.string.stop), new DialogInterface.OnClickListener() {			
					@Override
					public void onClick(DialogInterface dialog, int which) {
						sStreaming.dismiss();
						sStreaming = null;
						sPlayer.stop();
					}
				});
			}
			sStreaming.show();

			if (TextUtils.isEmpty(mp3.getDownloadUrl())) {
				new FetchMp3LinkTaskForPreview().execute(mp3);
				break;
			}
			playMusic(mp3);
			break;
		}
		case MENU_DOWNLOAD: {
		    download(mp3);
			break;
		}
		}

		return true;
	}
    private void download(MusicInfo mp3) {
        if (TextUtils.isEmpty(mp3.getDownloadUrl())) {
            showDialog(DIALOG_WAITING_FOR_SERVER);
            new FetchMp3LinkTaskForDownload().execute(mp3);
        } else {
            DownloadInfo download = new DownloadInfo(mp3.getDownloadUrl(), MusicInfo.downloadPath(mp3));
            mDownloadService.insertDownload(download);

            Intent intent = new Intent(SearchResultActivity.this, DownloadActivity.class);
            startActivity(intent);
        }
        
    }
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Utils.D("onDestroy()");
		if (mProgressDialog != null) {
			removeDialog(DIALOG_WAITING_FOR_SERVER);
			mProgressDialog = null;
		}
		unbindService(mConnection);
	}
		
	private class FetchMp3LinkTaskForDownload extends AsyncTask<MusicInfo, Void, MusicInfo> {
		protected MusicInfo doInBackground(MusicInfo... mp3s) {
			MusicInfo mp3 = mp3s[0];
			mFetcher.setMusicDownloadUrl(mp3);
			return mp3;
		}

		protected void onPostExecute(MusicInfo mp3) {
			if (mp3.getDownloadUrl() == null) {
				dismissDialog(DIALOG_WAITING_FOR_SERVER);

				Toast.makeText(SearchResultActivity.this, R.string.no_download_link, Toast.LENGTH_SHORT).show();
				return;
			}
			
			DownloadInfo download = new DownloadInfo(mp3.getDownloadUrl(), MusicInfo.downloadPath(mp3));
			mDownloadService.insertDownload(download);

			Intent intent = new Intent(SearchResultActivity.this, DownloadActivity.class);
			startActivity(intent);
			dismissDialog(DIALOG_WAITING_FOR_SERVER);
		}
	}
	
	private class FetchMp3LinkTaskForPreview extends AsyncTask<MusicInfo, Void, MusicInfo> {
		protected MusicInfo doInBackground(MusicInfo... mp3s) {
			MusicInfo mp3 = mp3s[0];
			mFetcher.setMusicDownloadUrl(mp3);
			return mp3;
		}

		protected void onPostExecute(MusicInfo mp3) {
			if (mp3.getDownloadUrl() == null) {
				dismissDialog(DIALOG_WAITING_FOR_SERVER);

				Toast.makeText(SearchResultActivity.this, R.string.no_download_link, Toast.LENGTH_SHORT).show();
				return;
			}
			
			dismissDialog(DIALOG_WAITING_FOR_SERVER);
			playMusic(mp3);
		}
	}

	private class FetchMp3ListTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected void onPostExecute(Boolean result) {
			if (mProgressDialog != null && mProgressDialog.isShowing())
				dismissDialog(DIALOG_WAITING_FOR_SERVER);
			
			if (result) {
				mAdapter.setStatus(ListStatusView.Status.LOADED);
				mAdapter.notifyDataSetChanged();
			} else {
				mAdapter.setStatus(ListStatusView.Status.ERROR);
				mAdapter.notifyDataSetInvalidated();
			}
			
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			ArrayList<MusicInfo> mp3List;
			mp3List = mFetcher.getMusicInfoList();
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
		}
	}

	
	// A thread safe wrapper around ArrayList<MP3Info>.
	private final class Mp3ListWrapper {
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
			} finally {
				w.unlock();
			}
		}
		
		public void add(MusicInfo info) {
			w.lock();
			try {
				mMp3List.add(info);
			} finally {
				w.unlock();
			}
		}
		
		public void append(ArrayList<MusicInfo> mp3List) {
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

        private final static int VIEW_TYPE_NORMAL = 0;
        private final static int VIEW_TYPE_FOOTER = 1;
    	
    	private int mResource;
    	private LayoutInflater mInflater;
    	
    	private ListStatusView.Status mStatus;

    	public Mp3ListAdapter(Context context, int resource) {
    		mResource = resource;
    		mInflater = (LayoutInflater)context.getSystemService(
    				Context.LAYOUT_INFLATER_SERVICE);
    		mStatus = ListStatusView.Status.LOADED;
    	}
    	
    	public void setStatus(ListStatusView.Status status) {
    		mStatus = status;
    	}
    	
    	@Override
    	public int getCount() {
    		boolean showFooter =
    			mStatus == ListStatusView.Status.ERROR ||
    			mStatus == ListStatusView.Status.LOADING;
    		
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
				ListStatusView footerView = (ListStatusView)convertView;
				if (footerView == null) {
					footerView = (ListStatusView)mInflater.inflate(
							R.layout.liststatus, null);
					TextView text = (TextView)footerView.findViewById(R.id.prompt);
					text.setText(R.string.loading);
				}
				if (mStatus == ListStatusView.Status.LOADING) {
					footerView.setLoadingStatus();
				} else if (mStatus == ListStatusView.Status.ERROR) {
					footerView.setErrorStatus(mRetryListener);
				}
				return footerView;
			}
			
			View v;
			Object item = mData.get(position);
			if (convertView == null) {
				v = mInflater.inflate(mResource, parent, false);
			} else {
				v = convertView;
			}

			MusicInfo info = (MusicInfo)item;
			((TextView)v.findViewById(R.id.title)).setText(info.getTitle());
			((TextView)v.findViewById(R.id.artist)).setText(info.getArtist());
			((TextView)v.findViewById(R.id.size)).setText(info.getDisplayFileSize());

			if (mHasMoreData && position == mData.size() - 1 &&
				mStatus == ListStatusView.Status.LOADED) {
				fetchNextMp3ListBatch();
				setStatus(ListStatusView.Status.LOADING);
				notifyDataSetChanged();
			}

			return v;
		}
    }

	public static void handleMp3ListIntent(Context context,
			String keyWords) {
		Intent intent = new Intent(context, SearchResultActivity.class);
		intent.putExtra(Constants.QUERY, keyWords);
    	context.startActivity(intent);
	}
}
