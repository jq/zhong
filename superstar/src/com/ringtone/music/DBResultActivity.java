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
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.GestureDetector.OnGestureListener;
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
	
	private static boolean sHasMoreData = true;

	private MusicInfo mCurrentMusic;
	private int mPageNum ;

	@SuppressWarnings("unused")
//	private SearchBar mSearch;
	private Handler mHandler = new Handler();

	private ProgressDialog mProgressDialog;

	private ProgressBar mProgressBar;
	private TextView mSearchMessage;

	private static Mp3ListAdapter mAdapter;

	private DownloadService mDownloadService;

	private ProgressDialog mStreaming;
	private static String sStreamingTitle;
	private ControlBarView mControlBarView;
	
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
//		mAdapter = new Mp3ListAdapter(
//				DBResultActivity.this,
//				R.layout.result_item);
//
//		setListAdapter(mAdapter);
		
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
				sFetcher = MusicSearcherFactory.getInstance(MusicSearcherFactory.ID_LOCAL);
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

		mControlBarView = (ControlBarView) findViewById(R.id.control_bar);
		
		setListener();
		getListView().setOnTouchListener(new MyGesture());
		
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
	
	private void setListener() {
		// TODO Auto-generated method stub
		ImageButton btn_pre = (ImageButton) mControlBarView.findViewById(R.id.pre);
		ImageButton btn_next = (ImageButton) mControlBarView.findViewById(R.id.next);
		ImageButton btn_refresh = (ImageButton) mControlBarView.findViewById(R.id.refresh);
		ImageButton btn_download = (ImageButton) mControlBarView.findViewById(R.id.download);

		btn_refresh.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mAdapter.refresh();
	    	}
		});
		
		btn_pre.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mAdapter.getLastPage();
			}
		});
		
		btn_next.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mAdapter.getNextPage();
			}
			
		});
		
		btn_download.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
	            Intent intent = new Intent(DBResultActivity.this, DownloadActivity.class);
				startActivity(intent);
	    	}
		});
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
//		if (mAdapter == null) {
//	        mAdapter = new Mp3ListAdapter(
//	            DBResultActivity.this,
//					R.layout.result_item);
//			setListAdapter(mAdapter);
//		}
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
				mPageNum++;
				mControlBarView.setText("Page "+Integer.toString(mPageNum)+" of "+Integer.toString(Constants.dbAdapter.getMaxPageNum())+" Pages");
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
		private boolean mCache;

//		private ControlBarView.Status mStatus;

		public Mp3ListAdapter(Context context, int resource) {
			mResource = resource;
            mInflater = (LayoutInflater)context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
//			mStatus = ControlBarView.Status.LOADED;
            mCache=true;
		}

//		public void setStatus(ControlBarView.Status status) {
//			mStatus = status;
//		}

		public void getNextPage(){
			mPageNum++;
			getData();		
		}
		
		public void getLastPage(){
			if (mPageNum>1) {
				mPageNum--;
				getData();
			}
		}
		
		public void refresh(){
			sData.clear();
			mPageNum=1;
			mCache=false;
			Constants.dbAdapter.dropall();
			getData();
		}
		
		public void getData(){
			Cursor c=Constants.dbAdapter.getHistoryByPage(mPageNum);
			String displaytext="Page "+Integer.toString(mPageNum)+" of "+Integer.toString(Constants.dbAdapter.getMaxPageNum())+" Pages";
			if (mCache==true){
				displaytext="Cache mp3 list\n"+displaytext;
			}
			ArrayList<MusicInfo> musicList = new ArrayList<MusicInfo>();
			if (c != null && c.getCount()>0){
				sData.clear();
				for (c.moveToFirst();!c.isAfterLast(); c.moveToNext()){
					int i=0;
					MusicInfo m=new MusicInfo();
					i=c.getColumnIndex(MusicInfo.TYPE_ALBUM);
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
					mControlBarView.setText(displaytext);
					return;
				}else{
					sData = null;
				}
			}
	
			if (mPageNum == 1) {
				mCache=false;
			}
			
			mPageNum--;

			if (mCache==true) {
				Toast.makeText(getBaseContext(), "There is no more cache music,you can press refresh button to get new music list.", Toast.LENGTH_SHORT).show();
				return;
			} else{
				sData.clear();
				mControlBarView.setText("Fetching new page");
				mProgressBar.setVisibility(View.VISIBLE);
				mSearchMessage.setText("Please wait while we get the music of \"" + getString(R.string.singer) + "\"");
				startQuery(DBResultActivity.this,getString(R.string.singer));
			}
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

	private class MyGesture implements OnTouchListener,OnGestureListener{
		private GestureDetector mGestureDetector;
		
		public MyGesture() {  
	        mGestureDetector = new GestureDetector(this);  
	    }
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
	        return mGestureDetector.onTouchEvent(event);  
		}

		@Override
		public boolean onDown(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
		    // 参数解释：  
		    // e1：第1个ACTION_DOWN MotionEvent  
		    // e2：最后一个ACTION_MOVE MotionEvent  
		    // velocityX：X轴上的移动速度，像素/秒  
		    // velocityY：Y轴上的移动速度，像素/秒  
		  
		    // 触发条件 ：  
		    // X轴的坐标位移大于FLING_MIN_DISTANCE，且移动速度大于FLING_MIN_VELOCITY个像素/秒  	      
		    final int FLING_MIN_DISTANCE = 50, FLING_MIN_VELOCITY = 50;  
		    if (e1.getX() - e2.getX() > FLING_MIN_DISTANCE && Math.abs(velocityX) > FLING_MIN_VELOCITY) {  
		        // Fling left  
//		        Toast.makeText(getBaseContext(), "to get the last page", Toast.LENGTH_SHORT).show(); 
		    	mAdapter.getLastPage();
		    } else if (e2.getX() - e1.getX() > FLING_MIN_DISTANCE && Math.abs(velocityX) > FLING_MIN_VELOCITY) {  
		        // Fling right  
//		        Toast.makeText(getBaseContext(), "to get the next page", Toast.LENGTH_SHORT).show();  
		    	mAdapter.getNextPage();
		    }  
		    return false;  
		}

		@Override
		public void onLongPress(MotionEvent e) {
			// TODO Auto-generated method stub
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onShowPress(MotionEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}
	}
}