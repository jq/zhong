package com.cinderella.musicsearch;

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
import android.media.MediaPlayer;
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
	private static PreviewTask sPreviewTask;
	
	private static int sIndex;
	
	private static TextView sArtistTextView;
	private static TextView sAlbumTextView;
	private static TextView sSizeTextView;
	private static TextView sSongTextView;
	
	private MusicInfo mMusicInfo;
	private String mDownloadedMusicPath;
	private boolean mIsBackground;
	private DownloadMusicTask mDownloadMusicTask;
	
	private int mCurLinkIndex = 0;
	
	private ProgressBar	mProgressBar;
	private TextView mMessage;
	private Button mRetryButton;
	
	private Button mPreviewButton;
	private Button mDownloadButton;
	
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
		mPreviewButton.setOnClickListener(new PreviewClickListener());
		mDownloadButton = (Button) findViewById(R.id.download_button);
		mDownloadButton.setOnClickListener(new DownloadClickListener());
		mRetryButton.setOnClickListener(new RetryClickLister());
		sAlbumTextView = (TextView) findViewById(R.id.album);
		sArtistTextView = (TextView) findViewById(R.id.artist);
		sSizeTextView = (TextView) findViewById(R.id.size);
		sSongTextView = (TextView) findViewById(R.id.song);
		initData(getIntent());
		initView();
		
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
		mIsBackground = false;
		mDownloadedMusicPath = null;
		mMusicInfo = SearchActivity.sData.get(sIndex);
		mAdapter = null;
		mCurLinkIndex = 0;
		mDownloadMusicTask = null;
		setListAdapter(mAdapter);
		setLoadingStatus();
	}
	
	private void initView() {
		sAlbumTextView.setText(mMusicInfo.getAlbum());
		sArtistTextView.setText(mMusicInfo.getArtist());
		sSizeTextView.setText("Size: "+mMusicInfo.getDisplayFileSize());
		sSongTextView.setText(mMusicInfo.getTitle());
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
		private boolean mIsDownloadBackGround = false;
		private DownloadProgressDialogListerner mDownloadProgressDialogListerner;
		private String mMusicPath;
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
			mDownloadButton.setText(R.string.show_progress);
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
				mMusicPath = f.getAbsolutePath();
				FileOutputStream file =  new FileOutputStream(f);
				int percent = 0;
				int last_percent = 0;
				while ((len = is.read(buff)) > 0) {
					file.write(buff, 0, len);
					count = count + len;
					if (mDownloadMusicInfo.getFilesize() != 0) {
						percent = (int) ((count*100)/mDownloadMusicInfo.getFilesize());
						if (percent != last_percent) {
							Utils.D("percent: "+percent);
							publishProgress(percent);
							last_percent = percent;
						}
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
				mDownloadButton.setText(R.string.download);
			} else {
				Utils.D("result!=null");
				mDownloadButton.setText(R.string.play);
				mDownloadedMusicPath = result.getAbsolutePath();
			}
			if (!mIsDownloadBackGround) {
				mDownloadProgressDialogListerner.onDownloadFinish();
			}
			if (mIsDownloadBackGround) {
				//add notification to lead user to lib actitivity.
			}
		}
		@Override
		protected void onProgressUpdate(Integer... values) {
			if (!mIsDownloadBackGround) {
				mDownloadProgressDialogListerner.onProgressUpdate(values[0]);
			}
		}
		@Override
		protected void onCancelled() {
			Utils.D("onCancel of DownloadTask called.......");
			mDownloadMusicTask = null;
			mDownloadButton.setText(R.string.download);
			if (mDownloadedMusicPath != null) {
				Utils.deleteFile(mMusicPath);
			}
			if (!mIsDownloadBackGround) {
				mDownloadProgressDialogListerner.cancelDownload();
			}
		}
		public void showProgressDialog() {
			mDownloadProgressDialogListerner.showProgressDialog();
		}
		public void hideProgressDialog() {
			mDownloadProgressDialogListerner.hideProgressDialog();
		}
		public boolean isProgressDialogShowing() {
			return mDownloadProgressDialogListerner.isShowing();
		}
		public void setBackground(boolean isBackground) {
			mIsDownloadBackGround = isBackground;
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
			mDownloadProgressDialog.setButton(DialogInterface.BUTTON1, MusicPageActivity.this.getString(R.string.hide), new HideProgressDialogClickListener());
			mDownloadProgressDialog.setButton(DialogInterface.BUTTON2, MusicPageActivity.this.getString(R.string.cancel), new cancelProgressDialogClickListener());
			mDownloadProgressDialog.setCancelable(true);
			showProgressDialog();
		}
		public void onProgressUpdate(int progress) {
			mDownloadProgressDialog.setProgress(progress);
		}
		public void onDownloadFinish() {
			mDownloadProgressDialog.cancel();
		}
		public void hideProgressDialog() {
			mDownloadProgressDialog.dismiss();
		}
		public void showProgressDialog() {
			mDownloadProgressDialog.show();
		}
		public void cancelDownload() {
			mDownloadProgressDialog.cancel();
		}
		public boolean isShowing() {
			Utils.D("mDownloadProgressDialog.isShowing: "+mDownloadProgressDialog.isShowing());
			return mDownloadProgressDialog.isShowing();
		}
	}
	
	private class PreviewTask extends AsyncTask<Void, Void, Integer> {
		private ProgressDialog mStreamProgressDialog;
		private MediaPlayer sPreviewMediaPlayer;
		@Override
		protected void onPreExecute() {
			mStreamProgressDialog = new ProgressDialog(MusicPageActivity.this);
			mStreamProgressDialog.setTitle(R.string.streaming);
			mStreamProgressDialog.setIndeterminate(true);
			mStreamProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mStreamProgressDialog.setCancelable(true);
			mStreamProgressDialog.setOnDismissListener(new dismissPreviewDialogClickListener());
			mStreamProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, MusicPageActivity.this.getString(R.string.cancel), new cancelPreviewDialogClickListener());
			mStreamProgressDialog.show();
		}
		@Override
		protected Integer doInBackground(Void... params) {
			sPreviewMediaPlayer = new MediaPlayer();
			try {
				if (mDownloadedMusicPath == null) {
					sPreviewMediaPlayer.setDataSource(mMusicInfo.getDownloadUrl().get(mCurLinkIndex));
				} else {
					sPreviewMediaPlayer.setDataSource(mDownloadedMusicPath);
				}
				sPreviewMediaPlayer.prepare();
				sPreviewMediaPlayer.start();
			} catch (Exception e) {
				return null;
			}
			return 1;
		}
		@Override
		protected void onPostExecute(Integer result) {
			Utils.D("in postExecute of PreviewTask.");
			//mStreamProgressDialog.cancel();
		}
		@Override
		protected void onCancelled() {
			Utils.D("in onCancel of PreviewTask");
			mStreamProgressDialog.cancel();
			stopPreviewPlayer();
			sPreviewTask = null;
		}
		public void stopPreviewPlayer() {
			sPreviewMediaPlayer.stop();
			sPreviewMediaPlayer.release();
			sPreviewMediaPlayer = null;
		}
	}
	
	private void previewTask() {
		if (sPreviewTask != null) {
			sPreviewTask.cancel(true);
		}
		sPreviewTask = new PreviewTask();
		sPreviewTask.execute();
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
			if (position == mCurLinkIndex) {
				rb.setChecked(true);
			} else {
				rb.setChecked(false);
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
			mCurLinkIndex = mPosition;
			mAdapter.notifyDataSetChanged();
		}
	}
	
	private class DownloadClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			Utils.D("mDownloadMusicTask == "+mDownloadMusicTask);
			Utils.D("mDownloadedMusicPath == "+mDownloadedMusicPath);
			if (mDownloadMusicTask == null && mDownloadedMusicPath == null) {
				downloadMusic();
			} else if (mDownloadMusicTask!=null && mDownloadedMusicPath==null && mDownloadMusicTask.isProgressDialogShowing()){
				mDownloadMusicTask.hideProgressDialog();
			} else if (mDownloadMusicTask!=null && mDownloadedMusicPath==null && !mDownloadMusicTask.isProgressDialogShowing()) {
				mDownloadMusicTask.showProgressDialog();
			} else if (mDownloadedMusicPath!=null) {
				Utils.startMusicPlayer(MusicPageActivity.this, mDownloadedMusicPath);
			}
		}
	}
	
	private class PreviewClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			previewTask();
		}
	}
	
	private class RetryClickLister implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			fetchDownloadLink();
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
	
	private class cancelProgressDialogClickListener implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (mDownloadMusicTask != null) {
				boolean isCanceled = mDownloadMusicTask.cancel(true);
				Utils.D("isCanceled?:"+isCanceled);
			}
		}               
	}
	
	private class cancelPreviewDialogClickListener implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			cancelPreviewDialog();
		}
	}
	
	private class dismissPreviewDialogClickListener implements DialogInterface.OnDismissListener {
		@Override
		public void onDismiss(DialogInterface dialog) {
			cancelPreviewDialog();
		}
	}
	
	private void cancelPreviewDialog() {
		if (sPreviewTask != null) {
			sPreviewTask.stopPreviewPlayer();
			sPreviewTask.cancel(true);
			Utils.D("previewTask.isCanceled?="+sPreviewTask.isCancelled());
		}
		sPreviewTask = null;
	}

	private void setLoadingStatus() {
		mProgressBar.setVisibility(View.VISIBLE);
		mMessage.setVisibility(View.VISIBLE);
		mRetryButton.setVisibility(View.GONE);
		mMessage.setText(R.string.loading_downloaded_music);
	}
	
	private void setErrorStatus() {
		mProgressBar.setVisibility(View.GONE);
		mMessage.setVisibility(View.VISIBLE);
		mRetryButton.setVisibility(View.VISIBLE);
		mMessage.setText(R.string.load_download_link_failed);
	}
}
