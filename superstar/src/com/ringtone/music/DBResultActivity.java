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
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class DBResultActivity extends ListActivity {
	private static final String TAG = Utils.TAG;
	private static final int DIALOG_WAITING_FOR_SERVER = 1;
	private static final int DIALOG_MUSIC_OPTIONS = 2;
	private static final int DIALOG_MUSIC_STREAMING = 3;

	private static final int MUSIC_OPTION_PREVIEW = 0;
	private static final int MUSIC_OPTION_DOWNLOAD = 1;

	private static Mp3ListWrapper sData;
	private static String sQuery;
	private static FetchMp3ListTask sFetchMp3ListTask;
	private static DBResultActivity sSearchActivity;
	private static volatile IMusicSearcher sFetcher;
	private LayoutInflater mControl_bar;
	private LinearLayout mControlView;
	
	private static boolean sHasMoreData = true;

	private MusicInfo mCurrentMusic;
	private int mPageNum ;

	@SuppressWarnings("unused")
//	private SearchBar mSearch;
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
				DBResultActivity.this,
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
			if (sSearchActivity != null)
				sSearchActivity.notifyDataSetInvalidated();
			sHasMoreData = true;
			if (sFetcher == null)
			{
				sFetcher = MusicSearcherFactory.getInstance(MusicSearcherFactory.ID_SOGOU);
				sFetcher.setQuery(keyWords);
			}
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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Constants.init(this);
		sSearchActivity = this;
        
		Utils.D("Mp3ListActivity onCreate()");

		setContentView(R.layout.result_list);
		Utils.addAds(this);

        bindService(new Intent(this, DownloadService.class),
                mConnection, Context.BIND_AUTO_CREATE);

		mSearchMessage = (TextView) findViewById(R.id.search_message);
		mProgressBar = (ProgressBar) findViewById(R.id.search_progress);

//		mSearch = new SearchBar(this);
		
		mControl_bar = getLayoutInflater();
		mControlView = (LinearLayout) mControl_bar.inflate(R.layout.control_bar, (ViewGroup) findViewById(R.id.control_bar));
		TextView mtv = (TextView) mControlView.findViewById(R.id.text);
		mtv.setText("Hello world");
		ImageButton btn_pre = (ImageButton) mControlView.findViewById(R.id.pre);
		ImageButton btn_next = (ImageButton) mControlView.findViewById(R.id.next);
		ImageButton btn_refresh = (ImageButton) mControlView.findViewById(R.id.refresh);
		ImageButton btn_download = (ImageButton) mControlView.findViewById(R.id.download);
		ImageButton btn_head = (ImageButton) mControlView.findViewById(R.id.head);
		btn_pre.setImageDrawable(getResources().getDrawable(R.drawable.button_pre));
		btn_next.setImageDrawable(getResources().getDrawable(R.drawable.button_next));
		btn_refresh.setImageDrawable(getResources().getDrawable(R.drawable.button_refresh));
		btn_download.setImageDrawable(getResources().getDrawable(R.drawable.button_download));
		btn_head.setImageDrawable(getResources().getDrawable(R.drawable.superstar));
		btn_pre.setBackgroundColor(Color.BLACK);
		btn_next.setBackgroundColor(Color.BLACK);
		btn_refresh.setBackgroundColor(Color.BLACK);
		btn_download.setBackgroundColor(Color.BLACK);
		btn_head.setBackgroundColor(Color.BLACK);
		btn_download.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if (event.getAction()==MotionEvent.ACTION_DOWN){
					((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.button_download_touch));
				}if(event.getAction() == MotionEvent.ACTION_UP){
					((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.button_download));
				}
				return false;
			}
		});
		
		btn_pre.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if (event.getAction()==MotionEvent.ACTION_DOWN){
					((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.button_pre_touch));
				}if(event.getAction() == MotionEvent.ACTION_UP){
					((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.button_pre));
				}
				return false;
			}
		});
		
		btn_next.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if (event.getAction()==MotionEvent.ACTION_DOWN){
					((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.button_next_touch));
				}if(event.getAction() == MotionEvent.ACTION_UP){
					((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.button_next));
				}
				return false;
			}
		});
		
		btn_refresh.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if (event.getAction()==MotionEvent.ACTION_DOWN){
					((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.button_refresh_touch));
				}if(event.getAction() == MotionEvent.ACTION_UP){
					((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.button_refresh));
				}
				return false;
			}
		});
		
		btn_refresh.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				sData.clear();
				mPageNum=1;
				Constants.dbAdapter.dropall();
				mAdapter.getData();
	    	}
		});
		
		btn_pre.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (mPageNum>1) {
					mPageNum--;
					mAdapter.getData();
				}
			}
		});
		
		btn_next.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mPageNum++;
				mAdapter.getData();	
			}
			
		});
		
//		Constants.dbAdapter.initCache();
		mPageNum=1;
		sData = new Mp3ListWrapper();
//		startQuery(this,getString(R.string.singer));
        mAdapter = new Mp3ListAdapter(
	            DBResultActivity.this,
					R.layout.result_item);
		setListAdapter(mAdapter);
		mAdapter.getData();
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
  
    private void handleSearchResult(ArrayList<MusicInfo> mp3List) {
		if (mAdapter == null) {
	        mAdapter = new Mp3ListAdapter(
	            DBResultActivity.this,
					R.layout.result_item);
			setListAdapter(mAdapter);
		}
		// Uncomment until it is really working.
		//mp3List = Utils.dedupMp3List(mp3List);
		if (mp3List != null) {
			if (sData == null)
				sData = new Mp3ListWrapper();
			
			if (mp3List.size() > 0) {
				sData.clear(); // add by jzh
				sData.append(mp3List);
//				Constants.dbAdapter.dropall();
				Constants.dbAdapter.insertHistory(mp3List);
				((TextView) mControlView.findViewById(R.id.text)).setText("Page "+Integer.toString(mPageNum)+" of "+Integer.toString(Constants.dbAdapter.getMaxPageNum())+" Pages");
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
//			mAdapter.setStatus(ControlBarView.Status.LOADED);
//			((TextView) mControlView.findViewById(R.id.text)).setText("Loaded");
            mAdapter.notifyDataSetChanged();
		} else {
//			mAdapter.setStatus(ControlBarView.Status.ERROR);
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
				if (mAdapter != null)
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

//		private ControlBarView.Status mStatus;

		public Mp3ListAdapter(Context context, int resource) {
			mResource = resource;
            mInflater = (LayoutInflater)context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
//			mStatus = ControlBarView.Status.LOADED;
		}

//		public void setStatus(ControlBarView.Status status) {
//			mStatus = status;
//		}

		public void getData(){
			Cursor c=Constants.dbAdapter.getHistoryByPage(mPageNum);
			((TextView) mControlView.findViewById(R.id.text)).setText("Page "+Integer.toString(mPageNum)+" of "+Integer.toString(Constants.dbAdapter.getMaxPageNum())+" Pages");
			ArrayList<MusicInfo> musicList = new ArrayList<MusicInfo>();
			if (c != null && c.getCount()>0){
				sData.clear();
				for (c.moveToFirst();!c.isAfterLast(); c.moveToNext()){
					int i=0;
					MusicInfo m=new MusicInfo();
					i=c.getColumnIndex(MusicInfo.TYPE_ALBUM);
					String tmp=c.getString(i);
					m.setAlbum(c.getString(i));
					i=c.getColumnIndex(MusicInfo.TYPE_ARTIST);
					m.setArtist(c.getString(i));
					i=c.getColumnIndex(MusicInfo.TYPE_DISPLAYSIZE);
					m.setDisplayFileSize(c.getString(i));
					i=c.getColumnIndex(MusicInfo.TYPE_TITLE);
					m.setTitle(c.getString(i));
					i=c.getColumnIndex(MusicInfo.TYPE_TYPE);
					m.setType(c.getString(i));
					i=c.getColumnIndex(MusicInfo.TYPE_URL);
					m.addUrl(c.getString(i));
					musicList.add(m);
				}
				if (musicList.size() > 0) {
					sData.append(musicList);
					notifyDataSetChanged();
					return;
				}else{
					sData = null;
				}
			}
			sData.clear();
			((TextView) mControlView.findViewById(R.id.text)).setText("Fetching new page");
			mProgressBar.setVisibility(View.VISIBLE);
			mSearchMessage.setText("Please wait while we search \"" + getString(R.string.singer) + "\"");
			startQuery(DBResultActivity.this,getString(R.string.singer));
		}
		
		@Override
		public int getCount() {
			return sData.size();
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
}
