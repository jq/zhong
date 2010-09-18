package com.feebe.musicsearch;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

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
		mProgressBar = (ProgressBar) findViewById(R.id.search_progress);
		mMessage = (TextView) findViewById(R.id.search_message);
		mRetryButton = (Button) findViewById(R.id.retry_button);
		sIndex = getIntent().getIntExtra(Const.INDEX, -1);
		sMusicInfo = SearchActivity.sData.get(sIndex);
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
            return v;
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
