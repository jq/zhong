package com.ringtone.music;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.admob.android.ads.AdView;
import com.ringtone.music.download.DownloadActivity;
import com.ringtone.music.download.DownloadInfo;
import com.ringtone.music.download.DownloadService;

import com.ringtone.music.R;
import com.qwapi.adclient.android.view.QWAdView;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class SearchResultActivity extends ListActivity {
	private static final String TAG = Utils.TAG;
	private static final int DIALOG_WAITING_FOR_SERVER = 1;
	private static final int DIALOG_MUSIC_OPTIONS = 2;
	private static final int DIALOG_MUSIC_STREAMING = 3;

	private static final int MUSIC_OPTION_PREVIEW = 0;
	private static final int MUSIC_OPTION_DOWNLOAD = 1;

	private static Mp3ListWrapper sData;
	private static String sQuery;
	private static FetchMp3ListTask sFetchMp3ListTask;
	private static SearchResultActivity sSearchActivity;
	private static volatile IMusicSearcher sFetcher;
	private static boolean sHasMoreData = true;

	private MusicInfo mCurrentMusic;

	@SuppressWarnings("unused")
	private SearchBar mSearch;
	private Handler mHandler = new Handler();

	private ProgressDialog mProgressDialog;

	private ProgressBar mProgressBar;
	private TextView mSearchMessage;

	private Mp3ListAdapter mAdapter;

	private DownloadService mDownloadService;

	private ProgressDialog mStreaming;
	private static String sStreamingTitle;
	
	private static volatile MediaPlayer sPlayer;
	private static Thread sPreviewThread;
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mDownloadService = ((DownloadService.LocalBinder) service).getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			mDownloadService = null;
		}
	};

    
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case DIALOG_MUSIC_OPTIONS: {
			if (mCurrentMusic != null)
				dialog.setTitle("Options for \"" + mCurrentMusic.getTitle() + "\"");
			return;
		}
		case DIALOG_MUSIC_STREAMING: {
			if (mCurrentMusic != null) {
				sStreamingTitle = mCurrentMusic.getTitle();
			}
			if (!TextUtils.isEmpty(sStreamingTitle)) {
				dialog.setTitle(sStreamingTitle);
			}
		}
		}
	}

    
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

		case DIALOG_MUSIC_OPTIONS:
            return new AlertDialog.Builder(SearchResultActivity.this)
                .setTitle(R.string.options)
                .setItems(R.array.music_item_options, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case MUSIC_OPTION_PREVIEW:
								if (mCurrentMusic == null)
									return;
								mHandler.post(new Runnable() {
									@Override
									public void run() {
										showDialog(DIALOG_MUSIC_STREAMING);
									}
								});

								if (TextUtils.isEmpty(mCurrentMusic.getDownloadUrl())) {
									new FetchMp3LinkTaskForPreview().execute(mCurrentMusic);
									break;
								}
								playMusic(mCurrentMusic);

								break;
							case MUSIC_OPTION_DOWNLOAD:
								if (mCurrentMusic == null)
									return;
								download(mCurrentMusic);
								break;
							}
						}
                })
                .create();

		case DIALOG_MUSIC_STREAMING:
			if (mStreaming == null) {
				mStreaming = new ProgressDialog(SearchResultActivity.this);
				mStreaming.setTitle("Streaming music...");
				mStreaming.setMessage(getString(R.string.wait_streaming));
				mStreaming.setIndeterminate(true);
				mStreaming.setCancelable(false);
        		mStreaming.setButton(getString(R.string.stop), new DialogInterface.OnClickListener() {          
							@Override
        			public void onClick(DialogInterface dialog, int which) {
						if (mStreaming != null) {
							mStreaming.dismiss();
						}
						
						new Thread(new Runnable() {
							@Override
							public void run() {
								MediaPlayer player = sPlayer;
								if (player != null)
									player.release();
								sPlayer = null;
							}
						}).start();
					}
				});
			}
			return mStreaming;
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
			fetchNextMp3ListBatch(getApplication());
			mAdapter.setStatus(ListStatusView.Status.LOADING);
			mAdapter.notifyDataSetChanged();
		}

	};

	private static void fetchNextMp3ListBatch(Context context) {
		if (sFetchMp3ListTask != null)
			sFetchMp3ListTask.cancel(true);
		sFetchMp3ListTask = new FetchMp3ListTask(context);
		sFetchMp3ListTask.execute();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Utils.D("onResume");

		// Hack
		if (mProgressDialog != null && mProgressDialog.isShowing())
			mProgressDialog.dismiss();

		// Display
		mAdapter = new Mp3ListAdapter(
				SearchResultActivity.this,
				R.layout.result_item);

		setListAdapter(mAdapter);
		
		if (sData == null) {
			if (sFetchMp3ListTask != null) {
				if (!TextUtils.isEmpty(sQuery)) {
					mProgressBar.setVisibility(View.VISIBLE);
					mSearchMessage.setText("Please wait while we search \"" + sQuery + "\"");
				}
			} else {
				mProgressBar.setVisibility(View.GONE);
			}
		}

		if (sPlayer != null) {
			showDialog(DIALOG_MUSIC_STREAMING);
		}
	}

	public void notifyDataSetInvalidated() {
		if (mAdapter != null)
			mAdapter.notifyDataSetInvalidated();
	}

	public static void startQuery(Context context, String keyWords) {
		if (!TextUtils.isEmpty(keyWords)) {
			sQuery = keyWords;
			sData = null;
			if (sSearchActivity != null)
				sSearchActivity.notifyDataSetInvalidated();
			sHasMoreData = true;
            //sFetcher = MusicSearcherFactory.getInstance(MusicSearcherFactory.ID_MERGED);
            //sFetcher = MusicSearcherFactory.getInstance(MusicSearcherFactory.ID_BAIDU);
            sFetcher = MusicSearcherFactory.getInstance(MusicSearcherFactory.ID_SOGOU);
	    	//sFetcher = MusicSearcherFactory.getInstance(MusicSearcherFactory.ID_SKREEMR);
	    	//sFetcher = MusicSearcherFactory.getInstance(MusicSearcherFactory.ID_REALM);
            
			sFetcher.setQuery(keyWords);
			fetchNextMp3ListBatch(context);
		} else {
			sFetchMp3ListTask = null;
			sFetcher = null;
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Utils.D("onNewIntent");
		if (mAdapter != null)
			mAdapter.notifyDataSetInvalidated();
	}

	@Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
		if (sData != null && position < sData.size()) {
			mCurrentMusic = sData.get(position);
			showDialog(DIALOG_MUSIC_OPTIONS);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sSearchActivity = this;

		Utils.D("Mp3ListActivity onCreate()");

		setContentView(R.layout.result_list);
		Utils.addAds(this);

        bindService(new Intent(this, DownloadService.class),
                mConnection, Context.BIND_AUTO_CREATE);

		mSearchMessage = (TextView) findViewById(R.id.search_message);
		mProgressBar = (ProgressBar) findViewById(R.id.search_progress);

		mSearch = new SearchBar(this);
	}
	
	
	private void onPlayError() {
		MediaPlayer player = sPlayer;
		sPlayer = null;
		if (player != null)
			player.release();
		
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (mStreaming != null) {
					mStreaming.dismiss();
				}
				Toast.makeText(getApplication(), "Streaming error", Toast.LENGTH_LONG).show();
			}
		});
	}

    
	private void playMusic(final MusicInfo mp3) {
		if (mp3.getDownloadUrl() !=null && mp3.getDownloadUrl().startsWith("http:")) {
			sPreviewThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						MediaPlayer player = sPlayer;
						if (player != null) {
							player.release();
						}
						
						sPlayer = new MediaPlayer();
						player = sPlayer;
						player.reset();
						player.setDataSource(mp3.getDownloadUrl());
						player.prepare();
						
						player.start();
						player.setOnCompletionListener(new OnCompletionListener () {
							@Override
							public void onCompletion(MediaPlayer mp) {
								sPlayer = null;
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										if (mStreaming != null) {
											mStreaming.dismiss();
										}
									}
								});
							}
						});
						player.setOnErrorListener(new OnErrorListener() {
							@Override
							public boolean onError(MediaPlayer mp, int what, int extra) {
								onPlayError();
								return true;
							}

						});
						
						if (sPlayer == null) {
							// Someone requested us to stop.
							player.release();
						}
					} catch (IllegalArgumentException e) {
						onPlayError();
						e.printStackTrace();
					} catch (IllegalStateException e) {
						//onPlayError();
						e.printStackTrace();
					} catch (IOException e) {
						onPlayError();
						e.printStackTrace();
					} finally {
						sPreviewThread = null;
					}
				}
			});
			sPreviewThread.start();
		}
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
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
		mProgressDialog = null;
		unbindService(mConnection);
		sSearchActivity = null;
	}

    private class FetchMp3LinkTaskForDownload extends AsyncTask<MusicInfo, Void, MusicInfo> {
		protected MusicInfo doInBackground(MusicInfo... mp3s) {
			MusicInfo mp3 = mp3s[0];
			sFetcher.setMusicDownloadUrl(SearchResultActivity.this, mp3);
			return mp3;
		}

		protected void onPostExecute(MusicInfo mp3) {
			if (mp3.getDownloadUrl() == null) {
				if (mProgressDialog != null && mProgressDialog.isShowing()) {
					mProgressDialog.dismiss();
				}

                Toast.makeText(SearchResultActivity.this, R.string.no_download_link, Toast.LENGTH_SHORT).show();
				return;
			}

            DownloadInfo download = new DownloadInfo(mp3.getDownloadUrl(), MusicInfo.downloadPath(mp3));
			mDownloadService.insertDownload(download);

            Intent intent = new Intent(SearchResultActivity.this, DownloadActivity.class);
			startActivity(intent);
			if (mProgressDialog != null && mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
			}
		}
	}

    private class FetchMp3LinkTaskForPreview extends AsyncTask<MusicInfo, Void, MusicInfo> {
		protected MusicInfo doInBackground(MusicInfo... mp3s) {
			MusicInfo mp3 = mp3s[0];
			sFetcher.setMusicDownloadUrl(SearchResultActivity.this, mp3);
			return mp3;
		}

		protected void onPostExecute(MusicInfo mp3) {
			if (mp3.getDownloadUrl() == null) {
				if (mStreaming != null && mStreaming.isShowing()) {
					mStreaming.dismiss();
				}

                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }

                Toast.makeText(SearchResultActivity.this, R.string.no_download_link, Toast.LENGTH_SHORT).show();
				return;
			}

			if (mProgressDialog != null && mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
			}
			playMusic(mp3);
		}
	}

    
    private void handleSearchResult(ArrayList<MusicInfo> mp3List) {
		if (mAdapter == null) {
	        mAdapter = new Mp3ListAdapter(
	            SearchResultActivity.this,
					R.layout.result_item);

			setListAdapter(mAdapter);
		}
		// Uncomment until it is really working.
		//mp3List = Utils.dedupMp3List(mp3List);
		if (mp3List != null) {
			if (sData == null)
				sData = new Mp3ListWrapper();
			
			if (mp3List.size() > 0) {
				sData.append(mp3List);
			} else {
				sHasMoreData = false;
				if (sData.size() == 0) {
					mProgressBar.setVisibility(View.GONE);
					if (!TextUtils.isEmpty(sQuery)) {
    					mSearchMessage.setText("Sorry, we didn't find any result for \"" + sQuery + "\"");
					} else {
						mSearchMessage.setText(getString(R.string.no_result));
					}
				}
			}
			mAdapter.setStatus(ListStatusView.Status.LOADED);
            mAdapter.notifyDataSetChanged();
		} else {
			mAdapter.setStatus(ListStatusView.Status.ERROR);
			mAdapter.notifyDataSetChanged();
		}
	}

	private static class FetchMp3ListTask extends AsyncTask<Void, Void, ArrayList<MusicInfo>> {
		Context mContext;
		public FetchMp3ListTask(Context context) {
			super();
			mContext = context;
		}
		
		@Override
		protected void onPostExecute(ArrayList<MusicInfo> mp3List) {
            if (sFetchMp3ListTask != this) {
                // A new query is going on?
                return;
            }
			sFetchMp3ListTask = null;
			if (sSearchActivity != null) {
				sSearchActivity.handleSearchResult(mp3List);
			}
		}

		@Override
		protected ArrayList<MusicInfo> doInBackground(Void... params) {
			return sFetcher.getNextResultList(mContext);
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

			if (sData == null)
				return footerCount;
			return sData.size() + footerCount;
		}

		@Override
		public Object getItem(int position) {
			if (sData == null)
				return null;

			if (position < sData.size())
				return sData.get(position);
			return null; // footer.
		}

		@Override
		public long getItemId(int position) {
			if (sData == null)
				return -1;

			if (position < sData.size())
				return position;
			return -1; // footer.
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			if (sData == null || position == sData.size()) {
				return VIEW_TYPE_FOOTER;
			}
			return VIEW_TYPE_NORMAL;
		}

        
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			boolean isFooter = (sData == null || position == sData.size());

			if (isFooter) {
				ListStatusView footerView = (ListStatusView) convertView;
				if (footerView == null) {
					footerView = (ListStatusView) mInflater.inflate(
							R.layout.liststatus, null);
                    TextView text = (TextView)footerView.findViewById(R.id.prompt);
					text.setText(R.string.loading_more);
				}
				if (mStatus == ListStatusView.Status.LOADING) {
					footerView.setLoadingStatus();
				} else if (mStatus == ListStatusView.Status.ERROR) {
					footerView.setErrorStatus(mRetryListener);
				}
				return footerView;
			}

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
            ((TextView)v.findViewById(R.id.size)).setText(info.getDisplayFileSize());

            if (sHasMoreData &&
                position == sData.size() - 1 &&
                mStatus == ListStatusView.Status.LOADED) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						fetchNextMp3ListBatch(getApplication());
						setStatus(ListStatusView.Status.LOADING);
						notifyDataSetChanged();
					}
				});
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
