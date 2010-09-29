package com.feebe.musicsearch;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.R.integer;
import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SimpleAdapter.ViewBinder;

public class MusicPageActivity extends ListActivity {
	
	private static MusicPageActivity sMusicPageActivity;
	
	private static MusicSearcher sFetcher;
	private static FetchDownloadLinkTask sFetchDownloadLinkTask;
	
	private static int sIndex;
	
	private MusicInfo mMusicInfo;
	private String mDownloadedMusicPath;
	private boolean mIsBackground;
	private DownloadMusicTask mDownloadMusicTask;
	
	private int mCurLinkIndex = -1;
	
	private ProgressBar	mProgressBar;
	private TextView mMessage;
	private Button mRetryButton;
	
	private Button mPreviewButton;
	private Button mDownloadButton;
	
	private ImageButton mPrevButton;
	private ImageButton mNextButton;
	
	private DownloadLinkListAdapter mAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.D("onCreate()");
		setContentView(R.layout.music_page);
		mProgressBar = (ProgressBar) findViewById(R.id.search_progress);
		mMessage = (TextView) findViewById(R.id.search_message);
		mRetryButton = (Button) findViewById(R.id.retry_button);
		mPreviewButton = (Button) findViewById(R.id.preview_button);
		mDownloadButton = (Button) findViewById(R.id.download_button);
		mPrevButton = (ImageButton) findViewById(R.id.btn_prev);
		mNextButton = (ImageButton) findViewById(R.id.btn_next);
		mPrevButton.setOnClickListener(new PrevClickListener());
		mNextButton.setOnClickListener(new NextClickListener());
		mRetryButton.setOnClickListener(new RetryClickLister());
		initData(getIntent());
		initView();
		mDownloadButton.setOnClickListener(new DownloadClickListener());
		fetchDownloadLink();
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		Utils.D("onNewIntent()");
		initData(intent);
		initView();
		fetchDownloadLink();
	}

	private void initData(Intent intent) {
		sIndex = intent.getIntExtra(Const.INDEX, 0);
		Utils.D("sIndex: "+sIndex);
		mMusicInfo = SearchActivity.sData.get(sIndex);
		mAdapter = null;
		setListAdapter(mAdapter);
		setLoadingStatus();
	}
	
	private void initView() {
		mDownloadButton.setText(R.string.download);
		if (sIndex == 0) {
			mPrevButton.setEnabled(false);
			mPrevButton.setClickable(false);
		} else {
			mPrevButton.setEnabled(true);
			mPrevButton.setEnabled(true);
		}
		Utils.D("PrevbuttonEnable? "+mPrevButton.isEnabled());
		if (sIndex == SearchActivity.sData.size()-1) {
			mNextButton.setEnabled(false);
			mNextButton.setClickable(false);
		} else {
			mNextButton.setEnabled(true);
			mNextButton.setClickable(true);
		}
		mDownloadButton.setText(R.string.download);
	}

	private void fetchDownloadLink() {
		if (sFetchDownloadLinkTask != null) {
			sFetchDownloadLinkTask.cancel(true);
		}
		sFetchDownloadLinkTask = new FetchDownloadLinkTask();
		sFetchDownloadLinkTask.execute();
		setLoadingStatus();
	}

	private class FetchDownloadLinkTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			if (sFetcher == null) {
				sFetcher = new MusicSearcher();
			} 
			sFetcher.setMusicDownloadUrl(mMusicInfo);
			return null;
		}

		@Override
		protected void onPostExecute(Void parm) {
			sFetchDownloadLinkTask = null;
			if (mAdapter == null) {
				mAdapter = new DownloadLinkListAdapter(MusicPageActivity.this, R.layout.link_item);
				setListAdapter(mAdapter);
			}
			mAdapter.notifyDataSetChanged();
			if (mAdapter.getCount() == 0) {
				setErrorStatus();
			} else {
				mDownloadButton.setEnabled(true);
				mPreviewButton.setEnabled(true);
			}
		}
	}
	
	private void downloadMusic() {
	 	mDownloadMusicTask = new DownloadMusicTask(this, mMusicInfo, mMusicInfo.getDownloadUrl().get(mCurLinkIndex));
	 	mDownloadMusicTask.execute();
	}
	
	private class DownloadMusicTask extends AsyncTask<Void, Integer, File> {
		private Context mContext;
		private MusicInfo mDownloadMusicInfo;
		private boolean mIsBackGround = false;
		private DownloadProgressDialogListerner mDownloadProgressDialogListerner;
		private String mUrl;
		public DownloadMusicTask(Context context, MusicInfo musicInfo, String url) {
			mContext = context;
			mDownloadMusicInfo = musicInfo;
			mUrl = url;
		}
		@Override
		protected void onPreExecute() {
			mDownloadProgressDialogListerner = new DownloadProgressDialogListerner(mContext);
			mDownloadProgressDialogListerner.onDownloadStart();
		}
		@Override
		protected File doInBackground(Void... params) {
			Utils.D("background start:");
			int count = 0;
			URL url = null;
			HttpURLConnection urlConn = null;
			InputStream stream = null;
			DataInputStream is = null;
			try {
				url = new URL(mUrl);
				urlConn = (HttpURLConnection)url.openConnection();
				urlConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3 -Java");
				urlConn.setConnectTimeout(4000);
				urlConn.connect();
				stream = urlConn.getInputStream();
				byte[] buff = new byte[4096];
				is = new DataInputStream(stream);
				int len;
				File f = new File(Const.sMusicDir+mDownloadMusicInfo.getTitle()+".mp3");
				FileOutputStream file =  new FileOutputStream(f);
				while ((len = is.read(buff)) > 0) {
					file.write(buff, 0, len);
					count = count + len;
					int percent = 0;
					int last_percent = 0;
					if (mDownloadMusicInfo.getFilesize() != 0)
						percent = (int) ((count*100)/mDownloadMusicInfo.getFilesize());
						if (percent != last_percent) {
							publishProgress(percent);
							last_percent = percent;
						}
				}
				urlConn.disconnect();
				return f;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		@Override
		protected void onPostExecute(File result) {
			mDownloadMusicTask = null;
			if (result == null) {
				Utils.D("result==null");
			} else {
				Utils.D("result!=null");
				mDownloadButton.setText(R.string.play);
				mDownloadedMusicPath = result.getAbsolutePath();
			}
			if (!mIsBackGround) {
				mDownloadProgressDialogListerner.onDownloadFinish();
			}
		}
		@Override
		protected void onProgressUpdate(Integer... values) {
			if (!mIsBackGround) {
				mDownloadProgressDialogListerner.onProgressUpdate(values[0]);
			}
		}
		@Override
		protected void onCancelled() {

			super.onCancelled();
		}
		public void showProgressDialog() {
			mDownloadProgressDialogListerner.showProgressDialog();
		}
		public void hideProgressDialog() {
			mDownloadProgressDialogListerner.hideProgressDialog();
		}
	}
	
	private class DownloadProgressDialogListerner {
		private Context mContext;
		private ProgressDialog mDownloadProgressDialog;
		public DownloadProgressDialogListerner(Context context) {
			mContext = context;
		}
		public void onDownloadStart() {
			mDownloadProgressDialog = new ProgressDialog(mContext);
			mDownloadProgressDialog.setTitle(R.string.download);
			mDownloadProgressDialog.setIndeterminate(false);
			mDownloadProgressDialog.setMax(100);
			mDownloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mDownloadProgressDialog.setProgress(0);
			mDownloadProgressDialog.setButton(MusicPageActivity.this.getString(R.string.hide), new HideProgressDialogClickListener());
			mDownloadProgressDialog.setCancelable(true);
			mDownloadProgressDialog.show();
		}
		public void onProgressUpdate(int progress) {
			mDownloadProgressDialog.setProgress(progress);
		}
		public void onDownloadFinish() {
			mDownloadProgressDialog.cancel();
		}
		public void hideProgressDialog() {
			mDownloadProgressDialog.hide();
		}
		public void showProgressDialog() {
			mDownloadProgressDialog.show();
		}
	}
	
	private class DownloadLinkListAdapter extends BaseAdapter {
		
		private int mResource;
		private LayoutInflater mInflater;
		
		public DownloadLinkListAdapter(Context context, int resource) {
			mResource = resource;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() { 
			if (mMusicInfo==null || !mMusicInfo.isRealDownloadLink()) {
				return 0;
			} else  {
				return mMusicInfo.getDownloadUrl().size();
			}
		}

		@Override
		public Object getItem(int position) {
			if (mMusicInfo!=null && mMusicInfo.isRealDownloadLink()) {
				return mMusicInfo.getDownloadUrl().get(position);
			} else {
				return null;
			}
		}

		@Override
		public long getItemId(int position) {
			if (mMusicInfo==null || !mMusicInfo.isRealDownloadLink()) {
				return -1;
			}
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			ArrayList<String> linkList = mMusicInfo.getDownloadUrl();
			if (convertView == null) {
				v = mInflater.inflate(mResource, parent, false);
			} else {
				v = convertView;
			}
			String link = linkList.get(position);
			RadioButton rb = (RadioButton) v.findViewById(R.id.is_checked);
			rb.setText(link);
			rb.setOnClickListener(new LinkItemClickListener(position));
			if (position == 0) {
				mCurLinkIndex = 0;
				rb.setChecked(true);
			}
			return v;
		}
	}
	
	private class LinkItemClickListener implements  View.OnClickListener{
		private int mPosition;
		public LinkItemClickListener(int position) {
			mPosition = position;
		}
		@Override
		public void onClick(View v) {
			ListView lv = getListView();
			for (int i=0; i<lv.getChildCount(); i++) {
				((RadioButton) lv.getChildAt(i).findViewById(R.id.is_checked)).setChecked(false);
			}
			((RadioButton) lv.getChildAt(mPosition).findViewById(R.id.is_checked)).setChecked(true);
			mCurLinkIndex = mPosition;
		}
	}
	
	private class DownloadClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			Utils.D("mDownloadMusicTask == "+mDownloadMusicTask);
			Utils.D("mDownloadedMusicPath == "+mDownloadedMusicPath);
			if (mDownloadMusicTask == null && mDownloadedMusicPath == null) {
				downloadMusic();
			} else if (mDownloadMusicTask!=null && mDownloadedMusicPath==null){
				mDownloadMusicTask.hideProgressDialog();
			} else if (mDownloadedMusicPath!=null) {
	    		Intent intent = new Intent(Intent.ACTION_VIEW);
	    		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    		intent.setDataAndType(Uri.parse("file://" + mDownloadedMusicPath), "audio");
	    		startActivity(intent);
			}
		}
	}
	
	private class PreviewClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			
		}
	}
	
	private class RetryClickLister implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			fetchDownloadLink();
		}
	}
	
	private class PrevClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(MusicPageActivity.this, MusicPageActivity.class);
			intent.putExtra(Const.INDEX, sIndex-1);
			startActivity(intent);
		}
	}
	
	private class NextClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(MusicPageActivity.this, MusicPageActivity.class);
			intent.putExtra(Const.INDEX, sIndex+1);
			startActivity(intent);
		}
	}
	
	private class HideProgressDialogClickListener implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (mDownloadMusicTask != null) {
				mDownloadMusicTask.hideProgressDialog();
			}
		}
	}

	private void setLoadingStatus() {
		mProgressBar.setVisibility(View.VISIBLE);
		mMessage.setVisibility(View.VISIBLE);
		mRetryButton.setVisibility(View.GONE);
		mMessage.setText(R.string.loading_download_link);
	}
	
	private void setErrorStatus() {
		mProgressBar.setVisibility(View.GONE);
		mMessage.setVisibility(View.VISIBLE);
		mRetryButton.setVisibility(View.VISIBLE);
		mMessage.setText(R.string.load_download_link_failed);
	}
}
