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

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class MusicPageActivity extends ListActivity {
	
	private static MusicPageActivity sMusicPageActivity;
	
	private static MusicSearcher sFetcher;
	private static FetchDownloadLinkTask sFetchDownloadLinkTask;
	
	private static int sIndex;
	private static MusicInfo sMusicInfo;
	
	private ProgressBar	mProgressBar;
	private TextView mMessage;
	private Button mRetryButton;
	
	private Button mPreviewButton;
	private Button mDownloadButton;
	
	private DownloadLinkListAdapter mAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music_page);
		mProgressBar = (ProgressBar) findViewById(R.id.search_progress);
		mMessage = (TextView) findViewById(R.id.search_message);
		mRetryButton = (Button) findViewById(R.id.retry_button);
		mPreviewButton = (Button) findViewById(R.id.preview_button);
		mDownloadButton = (Button) findViewById(R.id.download_button);
		sIndex = getIntent().getIntExtra(Const.INDEX, -1);
		sMusicInfo = SearchActivity.sData.get(sIndex);
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		getListView().setClickable(false);
		mDownloadButton.setOnClickListener(new DownloadClickListener());
		fetchDownloadLink();
	}

	private void fetchDownloadLink() {
		if (sFetchDownloadLinkTask != null) {
			sFetchDownloadLinkTask.cancel(true);
		}
		sFetchDownloadLinkTask = new FetchDownloadLinkTask();
		sFetchDownloadLinkTask.execute();
	}

	private class FetchDownloadLinkTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			if (sFetcher == null) {
				sFetcher = new MusicSearcher();
			} 
			sFetcher.setMusicDownloadUrl(sMusicInfo);
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
		}
	}
	
	private void downloadMusic() {
		new DownloadMusicTask(this, sMusicInfo, sMusicInfo.getDownloadUrl().get(0)).execute();
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
					if (mDownloadMusicInfo.getFilesize() != 0)
						Utils.D(""+(int) (count*100)/mDownloadMusicInfo.getFilesize());
						publishProgress((int) (count*100)/mDownloadMusicInfo.getFilesize());
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
			if (result == null) {
				Utils.D("result==null");
			} else {
				Utils.D("result!=null");
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
			mDownloadProgressDialog.setProgress(0);
			mDownloadProgressDialog.setCancelable(true);
			mDownloadProgressDialog.show();
		}
		public void onProgressUpdate(int progress) {
			mDownloadProgressDialog.setProgress(progress);
		}
		public void onDownloadFinish() {
			mDownloadProgressDialog.cancel();
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
			if (sMusicInfo==null || !sMusicInfo.isRealDownloadLink()) {
				return 0;
			} else  {
				return sMusicInfo.getDownloadUrl().size();
			}
		}

		@Override
		public Object getItem(int position) {
			if (sMusicInfo!=null && sMusicInfo.isRealDownloadLink()) {
				return sMusicInfo.getDownloadUrl().get(position);
			} else {
				return null;
			}
		}

		@Override
		public long getItemId(int position) {
			if (sMusicInfo==null || !sMusicInfo.isRealDownloadLink()) {
				return -1;
			}
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			ArrayList<String> linkList = sMusicInfo.getDownloadUrl();
			if (convertView == null) {
				v = mInflater.inflate(mResource, parent, false);
			} else {
				v = convertView;
			}
			String link = linkList.get(position);
			((RadioButton) v.findViewById(R.id.is_checked)).setText(link);
			((RadioButton) v.findViewById(R.id.is_checked)).setOnClickListener(new LinkItemClickListener(position));
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
		}
	} 
	
	private class DownloadClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			downloadMusic();
		}
	}
	
	private void setLoadingStatus() {
		mProgressBar.setVisibility(View.VISIBLE);
		mMessage.setVisibility(View.VISIBLE);
		mRetryButton.setVisibility(View.GONE);
		mMessage.setText(R.string.loading_download_link);
	}
	private void setErrorStatus() {
		mProgressBar.setVisibility(View.VISIBLE);
		mMessage.setVisibility(View.VISIBLE);
		mRetryButton.setVisibility(View.VISIBLE);
		mMessage.setText(R.string.load_download_link_failed);
	}
}
