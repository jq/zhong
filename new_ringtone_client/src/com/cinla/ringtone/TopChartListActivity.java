package com.cinla.ringtone;

import java.lang.reflect.Array;
import java.util.ArrayList;
import com.latest.ringtone.R;
import android.app.ListActivity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class TopChartListActivity extends ListActivity {
	
	private ProgressBar mProgressBar;
	private TextView mSearchMessage;
	private Button mRetryButton;
	
	private int mChartType;
	private String mChartTypeName;
	
	private ArrayList<TopItem> mTopItemList;
	private TopItemAdapter mAdapter;
	private static FetchTopListTask sFetchTopListTask;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.top_chart_page);
		
		mProgressBar = (ProgressBar) findViewById(R.id.search_progress);
		mSearchMessage = (TextView) findViewById(R.id.search_message);
		mRetryButton = (Button) findViewById(R.id.retry_button);
		mRetryButton.setOnClickListener(new OnRetryClickListener());
		
		mChartType = getIntent().getIntExtra(Constant.CHART_TYPE, 0);
		
		Utils.D("********************************type: "+mChartType);
		mChartTypeName = Constant.CHART_TYPE_NAME[mChartType];
		
		mAdapter = new TopItemAdapter(this, R.layout.top_track_item);
		getListView().setAdapter(mAdapter);
		
		if (sFetchTopListTask != null) {
			sFetchTopListTask.cancel(true);
			sFetchTopListTask = null;
		}
		sFetchTopListTask = new FetchTopListTask();
		sFetchTopListTask.execute(null);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		SearchListActivity.startQeuryByKey(TopChartListActivity.this, mTopItemList.get(position).getTitle(), true);
	}

	private void setLoadingStatus() {
		mProgressBar.setVisibility(View.VISIBLE);
		mSearchMessage.setVisibility(View.VISIBLE);
		mSearchMessage.setText(getString(R.string.loading)+" "+mChartTypeName);
		mRetryButton.setVisibility(View.GONE);
	}
	
	private void setErrorStatus() {
		mProgressBar.setVisibility(View.GONE);
		mSearchMessage.setVisibility(View.VISIBLE);
		mSearchMessage.setText(R.string.loading_error_message);
		mRetryButton.setVisibility(View.VISIBLE);
	}
	
	private class FetchTopListTask extends AsyncTask<Void, Void, ArrayList<TopItem>> {
		@Override
		protected void onPreExecute() {
			setLoadingStatus();
		}
		@Override
		protected ArrayList<TopItem> doInBackground(Void... params) {
			ArrayList<TopItem> topItemList = null;
			try {
				ITopChartFetcher fetcher = TopChartFactory.getTopChartFetcher(mChartType);
				topItemList = fetcher.getTopItemList();
			} catch (Exception e) {
				return null;
			}
			return topItemList;
		}
		@Override
		protected void onPostExecute(ArrayList<TopItem> result) {
			Utils.D("result: "+result);
			if (result == null) {
				setErrorStatus();
				return;
			} 
			if (mAdapter == null) {
				mAdapter = new TopItemAdapter(TopChartListActivity.this, R.layout.top_track_item);
			}
			mTopItemList = result;
			mAdapter.notifyDataSetChanged();
		}
	}
	
	private class TopItemAdapter extends BaseAdapter {
		
		private int mResource;
		private LayoutInflater mInflater;
		
		public TopItemAdapter(Context context, int resource) {
			mResource = resource;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		@Override
		public int getCount() {
			if (mTopItemList != null) {
				return mTopItemList.size();
			}
			return 0;
		}
		@Override
		public Object getItem(int position) {
			if (mTopItemList != null) {
				return mTopItemList.get(position);
			}
			return null;
		}
		@Override
		public long getItemId(int position) {
			if (mTopItemList == null) {
				return -1;
			}
			return position;
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = mInflater.inflate(mResource, parent, false);
				holder.image = (ImageView) convertView.findViewById(R.id.image);
				holder.title = (TextView) convertView.findViewById(R.id.title);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			TopItem topItem = mTopItemList.get(position);
			holder.title.setText(topItem.getTitle());
			if (topItem.getImageUrl()!=null) {
				com.cinla.imageloader.ImageLoader.initialize(TopChartListActivity.this);
				com.cinla.imageloader.ImageLoader.start(topItem.getImageUrl(), holder.image);
			} else {
				holder.image.setBackgroundResource(R.drawable.hot);
			}
			return convertView;
		}
	}
	
	private class ViewHolder {
		ImageView image;
		TextView title;
	}
	
	private class OnRetryClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			mAdapter = new TopItemAdapter(TopChartListActivity.this, R.layout.top_track_item);
			if (sFetchTopListTask != null) {
				sFetchTopListTask.cancel(true);
				sFetchTopListTask = null;
			}
			sFetchTopListTask = new FetchTopListTask();
			sFetchTopListTask.execute(null);
		}
	}
	
}
