package com.feebe.musicsearch;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DownloadedActivity extends ListActivity {

	private static ArrayList<DownloadedMusicInfo> sDownloadedMusicInfoList;
	private static FetchDownloadedMusicTask sFetchDownloadedMusicTask;
	private static TextView sLoadingMessage;
	private static ProgressBar sProgressBar;
	private static Button sRetryButton;
	
	private DownloadedAdapter mAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.downloaded_activity);
		
		sProgressBar = (ProgressBar) findViewById(R.id.search_progress);
		sLoadingMessage = (TextView) findViewById(R.id.search_message);
		sRetryButton = (Button) findViewById(R.id.retry_button);
		
		fetchDownloadedMusiTask();
	}
	
	private void fetchDownloadedMusiTask() {
		if (sFetchDownloadedMusicTask != null) {
			sFetchDownloadedMusicTask.cancel(true);
		}
		sFetchDownloadedMusicTask = new FetchDownloadedMusicTask();
		sFetchDownloadedMusicTask.execute();
	}

	private class FetchDownloadedMusicTask extends AsyncTask<Void, Void, ArrayList<DownloadedMusicInfo>> {
		@Override
		protected void onPreExecute() {
			setLoadingStatus();
		}
		@Override
		protected ArrayList<DownloadedMusicInfo> doInBackground(Void... params) {
			try {
				ArrayList<DownloadedMusicInfo> list = new ArrayList<DownloadedMusicInfo>();
				File musicDir = new File(Const.sMusicDir);
				Utils.D("musicDir: "+Const.sMusicDir);
				File[] mp3Files = musicDir.listFiles(new Mp3FileFilter());
				for (File mp3 : mp3Files) {
					list.add(new DownloadedMusicInfo(mp3.getName(), mp3.length(), mp3.lastModified()));
				}
				return list;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		@Override
		protected void onPostExecute(ArrayList<DownloadedMusicInfo> result) {
			if (result != null) {
				sDownloadedMusicInfoList = result;
			}
			if (mAdapter == null) {
				mAdapter = new DownloadedAdapter(DownloadedActivity.this, R.layout.downloaded_item);
				setListAdapter(mAdapter);
			}
			mAdapter.notifyDataSetChanged();
			Utils.D("sDownloadedMusicInfoList.size(): "+sDownloadedMusicInfoList.size());
			sFetchDownloadedMusicTask = null;
		}
	}
	
	private class Mp3FileFilter implements FileFilter {
		@Override
		public boolean accept(File pathname) {
			String fileName = pathname.getName();
			Utils.D("in filter: "+fileName.substring(fileName.length()-4));
			if (fileName.substring(fileName.length()-4).equalsIgnoreCase(".mp3")) {
				return true;
			} else {
				return false;
			}
		}
	}
	
	private class DownloadedMusicInfo {
		private String mFileName;
		private long mSize;
		private long mLastModified;
		public DownloadedMusicInfo(String fileName, long size, long lastModified) {
			this.mFileName = fileName;
			this.mSize = size;
			this.mLastModified = lastModified;
		}
		public String getFileName() {
			return mFileName;
		}
		public long getSize() {
			return mSize;
		}
		public long getLastModified() {
			return mLastModified;
		}
	}
	
	private class DownloadedAdapter extends BaseAdapter {
		private int mResource;
		private LayoutInflater mInflater;
		
		public DownloadedAdapter(Context context, int resource) {
			mResource = resource;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		@Override
		public int getCount() {
			if (sDownloadedMusicInfoList != null) {
				return sDownloadedMusicInfoList.size();
			} else {
				return 0;
			}
		}
		@Override
		public Object getItem(int arg0) {
			if (sDownloadedMusicInfoList!=null && arg0<sDownloadedMusicInfoList.size()) {
				return sDownloadedMusicInfoList.get(arg0);
			} else {
				return null;
			}
		}
		@Override
		public long getItemId(int arg0) {
			if (sDownloadedMusicInfoList!=null && arg0<sDownloadedMusicInfoList.size()) {
				return arg0;
			} else {
				return 0;
			}
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			if (convertView == null) {
				v = mInflater.inflate(mResource, parent, false);
			} else {
				v = convertView;
			}
			TextView song = (TextView) v.findViewById(R.id.song);
			Utils.D("song: "+song);
			song.setText(sDownloadedMusicInfoList.get(position).getFileName());
			return v;
		}
	}
	
	private void setLoadingStatus() {
		sProgressBar.setVisibility(View.VISIBLE);
		sLoadingMessage.setVisibility(View.VISIBLE);
		sRetryButton.setVisibility(View.GONE);
		sLoadingMessage.setText(R.string.loading_download_link);
	}
	
	private void setErrorStatus() {
		sProgressBar.setVisibility(View.GONE);
		sLoadingMessage.setVisibility(View.VISIBLE);
		sRetryButton.setVisibility(View.VISIBLE);
		sLoadingMessage.setText(R.string.load_download_link_failed);
	}
	
}
