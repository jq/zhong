package com.cinla.ringtone;

import com.latest.ringtone.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.SimpleAdapter.ViewBinder;

public class LastFmChartListActivity extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.lastfm_chart_list);
		
		TextView topTracks = (TextView) findViewById(R.id.top_tracks);
		topTracks.setOnClickListener(new OnLastFMListItemClickListener(Constant.CHART_TYPE_TOPTRACKS));
		
		TextView topArtists = (TextView) findViewById(R.id.top_artists);
		topArtists.setOnClickListener(new OnLastFMListItemClickListener(Constant.CHART_TYPE_TOPARTISTS));
		
		TextView lovedTracks = (TextView) findViewById(R.id.loved_tracks);
		lovedTracks.setOnClickListener(new OnLastFMListItemClickListener(Constant.CHART_TYPE_LOVEDTRACKS));
		
		TextView hypedTracks = (TextView) findViewById(R.id.hyped_tracks);
		hypedTracks.setOnClickListener(new OnLastFMListItemClickListener(Constant.CHART_TYPE_HYPEDTRACKS));
		
		TextView hypedArtists = (TextView) findViewById(R.id.hyped_artists);
		hypedArtists.setOnClickListener(new OnLastFMListItemClickListener(Constant.CHART_TYPE_HYPEDARTISTS));
		
		TextView topTags = (TextView) findViewById(R.id.top_tags);
		topTags.setOnClickListener(new OnLastFMListItemClickListener(Constant.CHART_TYPE_TOPTAGS));
		
	}
	
	private class OnLastFMListItemClickListener implements View.OnClickListener {
		private int mChartID;
		public OnLastFMListItemClickListener(int mChartID) {
			super();
			this.mChartID = mChartID;
		}
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(LastFmChartListActivity.this, TopChartListActivity.class);
			intent.putExtra(Constant.CHART_TYPE, mChartID);
			LastFmChartListActivity.this.startActivity(intent);
		}
	}
	
}
