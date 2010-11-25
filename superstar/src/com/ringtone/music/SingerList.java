package com.ringtone.music;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ringtone.music.download.DownloadActivity;
import com.ringtone.music.download.DownloadJson;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class SingerList extends ListActivity implements OnItemClickListener {
	private static SingerList sSingerList;
	private static int NEW_SINGERS_NUMBER = 3;
	private static int HOT_SINGERS_NUMBER = 3;
	
	private static String AllSingerURL="http://ringtone-superstar.s3.amazonaws.com/all.txt";
	private static String HotSingerURL="http://ringtone-superstar.s3.amazonaws.com/hot.txt";
	
	private ProgressBar mProgressBar;
	private TextView mSearchMessage;
	private static ArrayList<SingerInfo> sData = new ArrayList<SingerInfo>();	
	private SingerListAdapter mAdapter;
	private static FetchSingerListTask sFetchSingerListTask;
	
	private static String sKeyword;
	private static String sType;
	private static boolean sNoData;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sSingerList = this;
		
		setContentView(R.layout.singer_list);
		Utils.addAds(this);
		
		mProgressBar = (ProgressBar) findViewById(R.id.singer_search_progress);
		mSearchMessage = (TextView) findViewById(R.id.singer_search_message);
		
		mAdapter = new SingerListAdapter(SingerList.this,R.layout.singer_item);
		setListAdapter(mAdapter);
		getListView().setOnItemClickListener(this);
		
		if (sData == null){
				mProgressBar.setVisibility(View.VISIBLE);
				mSearchMessage.setVisibility(View.VISIBLE);
		}

		Bundle myExtras = getIntent().getExtras();
		String keyword = myExtras.getString("keyword");
		String type = myExtras.getString("type");
		startQuery(SingerList.this, keyword, type);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		sSingerList = null;
		sData = null;
	}
	
	private final class SingerListAdapter extends BaseAdapter {
		private int mResource;
		private LayoutInflater mInflater;
		
		public SingerListAdapter(Context context,int resource){
			mResource = resource;
			mInflater = (LayoutInflater)context.getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			if (sData == null) return 0;
				else return sData.size(); 
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			if (sData == null) return null;
			return sData.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			View v;
			Object item = sData.get(position);
			
			if (convertView == null){
				v = mInflater.inflate(mResource,parent,false);
			} else {
				v = convertView;
			}
			
			SingerInfo info = (SingerInfo) item;
			ImageView iv=(ImageView) v.findViewById(R.id.singer_image);
			TextView txt=(TextView) v.findViewById(R.id.singer_name);
			
			switch (info.getType()) {
				case SingerInfo.MATCH_SINGER_LIST_SINGER:
					iv.setImageResource(R.drawable.puzzle_green);
					break;
				case SingerInfo.HOT_SINGER_LIST_SINGER:
					iv.setImageResource(R.drawable.puzzle_red);
					break;
				case SingerInfo.NEW_SINGER_LIST_SINGER:
					iv.setImageResource(R.drawable.puzzle_yellow);
					break;
				case SingerInfo.MATCH_SINGER_LIST_TITLE:
					iv.setImageResource(R.drawable.singer_match);
					txt.setTextColor(Color.rgb(157, 205, 105));
					break;
				case SingerInfo.NEW_SINGER_LIST_TITLE:
					iv.setImageResource(R.drawable.singer_new);
					txt.setTextColor(Color.rgb(252,191,10));
					break;
				case SingerInfo.HOT_SINGER_LIST_TITLE:
					iv.setImageResource(R.drawable.singer_hot);
					txt.setTextColor(Color.rgb(236,111,33));
					break;
				case SingerInfo.ALL_SINGER_LIST_SINGER:
					iv.setImageResource(R.drawable.hot);
					break;
				default:
					break;
			}

			txt.setText(info.getSingerName());
			
			return v;
		}
		
	}
	
	private static class FetchSingerListTask extends AsyncTask<Void, Void, Void> {
		Context mContext;
		
		public FetchSingerListTask(Context context){
			super();
			mContext = context;
		}

		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			if (sSingerList != null){
				sSingerList.getlist();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void v) {
			if (sFetchSingerListTask != this) {
				return;
			}
			
			sFetchSingerListTask = null;

			if (sSingerList != null){
				sSingerList.handleSearchResult();
			}
		}
		
	}
	
	private void handleSearchResult(){
		if (sNoData==true) {
			mProgressBar.setVisibility(View.GONE);
			mSearchMessage.setVisibility(View.VISIBLE);
			mSearchMessage.setText("No Data Found");
			return;
		}
		
		mProgressBar.setVisibility(View.GONE);
		mSearchMessage.setVisibility(View.GONE);

		if (sSingerList!=null)
			sSingerList.notifyDataSetInvalidated();
		
		getListView().setFocusable(true);
		getListView().requestFocus();
	}
	
	private ArrayList<SingerInfo> getlist(){
		if (sData == null) sData = new ArrayList<SingerInfo>();
//		String allsingerdata=DownloadJson.readFile(new File("/sdcard/ringtonehelper/superstar/json/json.txt"));
//		String hotsingerdata=DownloadJson.readFile(new File("/sdcard/ringtonehelper/superstar/json/hot.txt"));
		
		if (sType!=null && sType.equals("allsingers")){
			try {
	//			JSONObject allSingerObj=new JSONObject(allsingerdata);
				JSONObject allSingerObj=DownloadJson.getJsonFromUrl(AllSingerURL, DownloadJson.ThreeAndAHalfDays);
				if (allSingerObj == null) {
					sNoData=true;
					return null;
				}
				JSONArray mAllSingerList = allSingerObj.getJSONArray("superstar");
				for(int i = 0; i < mAllSingerList.length(); i++) {
			        JSONArray item = mAllSingerList.getJSONArray(i);
			        SingerInfo info = new SingerInfo(SingerInfo.ALL_SINGER_LIST_SINGER,item.getString(1),item.getString(0));
					boolean exist=false;
					for (SingerInfo d : sData) {
						if (d.getSingerName().equals(info.getSingerName())) {
							exist=true;
							break;
						}
					}
					if (!exist)	sData.add(info);
				}
				
				if (sData.size() == 0) {
					sNoData=true;
					return null;
				}
				
				/* 显示所有时排序 */
				Collections.sort(sData, new CompareSingerInfo());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				sNoData=true;
				return null;
			}
		} else {
			try {
	//			JSONObject allSingerObj=new JSONObject(allsingerdata);
	//			JSONObject hotSingerObj=new JSONObject(hotsingerdata);
	
				JSONObject allSingerObj=DownloadJson.getJsonFromUrl(AllSingerURL, DownloadJson.ThreeAndAHalfDays);
				JSONObject hotSingerObj=DownloadJson.getJsonFromUrl(HotSingerURL, DownloadJson.ThreeAndAHalfDays);
				if (allSingerObj == null || hotSingerObj == null) {
					sNoData=true;
					return null;
				}
				
				JSONArray mAllSingerList = allSingerObj.getJSONArray("superstar");
				JSONArray hotSingerList = hotSingerObj.getJSONArray("hot");
				JSONArray newSingerList = hotSingerObj.getJSONArray("new");
				
				ArrayList<SingerInfo> primaryList = new ArrayList<SingerInfo>();
				for(int i = 0; i < mAllSingerList.length(); i++) {
			        JSONArray item = mAllSingerList.getJSONArray(i);
			        if (item == null) continue;
			        String primary = item.getString(2);
			        if (isMatch(sKeyword, primary)) {
				        SingerInfo info = new SingerInfo(SingerInfo.HOT_SINGER_LIST_SINGER,item.getString(1),item.getString(0));
				        primaryList.add(info);
			        }
				}
				
				ArrayList<SingerInfo> secondaryList = new ArrayList<SingerInfo>();
				for(int i = 0; i < mAllSingerList.length(); i++) {
			        JSONArray item = mAllSingerList.getJSONArray(i);
			        if (item == null) continue;
			        String secondary = item.getString(3);
			        if (isMatch(sKeyword, secondary)) {
				        SingerInfo info = new SingerInfo(SingerInfo.HOT_SINGER_LIST_SINGER,item.getString(1),item.getString(0));
				        secondaryList.add(info);
			        }
				}
				
				 if (primaryList.size()!=0 || secondaryList.size()!=0){
					 SingerInfo info=new SingerInfo(SingerInfo.MATCH_SINGER_LIST_TITLE, "","The most similar singers");
					 sData.add(info);
					 for (int i=0;i<primaryList.size();i++){
						 SingerInfo primaryinfo = new SingerInfo(SingerInfo.MATCH_SINGER_LIST_SINGER, 
								 primaryList.get(i).getPackageName(), 
								 primaryList.get(i).getSingerName());
						boolean exist=false;
						for (SingerInfo d : sData) {
							if (d.getSingerName().equals(primaryinfo.getSingerName())) {
								exist=true;
								break;
							}
						}
						if (!exist)	sData.add(primaryinfo);
					 }
					 for (int i=0;i<secondaryList.size();i++){
						 SingerInfo secondaryinfo = new SingerInfo(SingerInfo.MATCH_SINGER_LIST_SINGER, 
								 secondaryList.get(i).getPackageName(), 
								 secondaryList.get(i).getSingerName());
						boolean exist=false;
						for (SingerInfo d : sData) {
							if (d.getSingerName().equals(secondaryinfo.getSingerName())) {
								exist=true;
								break;
							}
						}
						if (!exist)	sData.add(secondaryinfo);
					}
				 } else {
					 SingerInfo info=new SingerInfo(SingerInfo.MATCH_SINGER_LIST_TITLE, "","No match singer");
					 sData.add(info);
				 }
				 
				 if (newSingerList.length() != 0){
					 SingerInfo info=new SingerInfo(SingerInfo.NEW_SINGER_LIST_TITLE, "","New singers");
					 sData.add(info);
					 Random rd=new Random();
					 int listlong = newSingerList.length();
	
					 for (int i=0,j=0;i<listlong*2 && j< NEW_SINGERS_NUMBER;i++){
						 int off=Math.abs(rd.nextInt()) % listlong;
						 JSONArray item = newSingerList.getJSONArray(off);
					     SingerInfo newinfo = new SingerInfo(SingerInfo.NEW_SINGER_LIST_SINGER,item.getString(1),item.getString(0));
					     boolean exist=false;
					     for (SingerInfo d : sData) {
					    	 if (d.getSingerName().equals(newinfo.getSingerName())) {
					    		 exist=true;
					    		 break;
					    	 }
					     }
					     if (!exist) {
					    	 sData.add(newinfo);
					    	 j++;
					     }
					 }
				 }
	
				 if (hotSingerList.length() != 0){
					 SingerInfo info=new SingerInfo(SingerInfo.HOT_SINGER_LIST_TITLE, "","Hot singers");
					 sData.add(info);
					 Random rd=new Random();
					 int listlong = hotSingerList.length();
					 for (int i=0,j=0;i<listlong*2 && j< HOT_SINGERS_NUMBER;i++){
						 int off=Math.abs(rd.nextInt()) % listlong;
						 JSONArray item = hotSingerList.getJSONArray(off);
					     SingerInfo hotinfo = new SingerInfo(SingerInfo.HOT_SINGER_LIST_SINGER,item.getString(1),item.getString(0));
					     boolean exist=false;
					     for (SingerInfo d : sData) {
					    	 if (d.getSingerName().equals(hotinfo.getSingerName())) {
					    		 exist=true;
					    		 break;
					    	 }
					     }
					     if (!exist) {
					    	 sData.add(hotinfo);
					    	 j++;
					     }
					 }
				 }
			}catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				sNoData=true;
				return null;
			}
		}
		return null;
	}
	
	private boolean isMatch(String keyword,String filter){
		if (keyword == null || filter == null) return false;
		String sKeyword = keyword.toLowerCase();
		String[] sFilters = filter.toLowerCase().split("\\|");
		for (int i=0;i<sFilters.length;i++){
			String key = sFilters[i];
			if (key == null || key.length() ==0 ) continue;
			if (sKeyword.contains(key)) return true;
		}
		return false;
	}
	
	private class SingerInfo {
		private static final int MATCH_SINGER_LIST_TITLE = 0;
		private static final int HOT_SINGER_LIST_TITLE = 1;
		private static final int NEW_SINGER_LIST_TITLE = 2;
		private static final int MATCH_SINGER_LIST_SINGER = 3;
		private static final int HOT_SINGER_LIST_SINGER = 4;
		private static final int NEW_SINGER_LIST_SINGER = 5;
		private static final int ALL_SINGER_LIST_SINGER = 6;
		
		private int mType;
		private String mPackageName;
		private String mSingerName;
		
		public SingerInfo(int type,String packagename,String singername){
			mType=type;
			mPackageName=packagename;
			mSingerName=singername;
		}

		public int getType() {
			return mType;
		}
		public String getPackageName() {
			return mPackageName;
		}
		public String getSingerName() {
			return mSingerName;
		}
		
		@Override
		public boolean equals(Object o) {
			SingerInfo info=(SingerInfo) o;
			if (info.getPackageName() == mPackageName)
				return true;
			return false;
		}
	}
	
	private class CompareSingerInfo implements Comparator {
		@Override
		public int compare(Object object1, Object object2) {
			// TODO Auto-generated method stub
			SingerInfo singer1 = (SingerInfo) object1;
			SingerInfo singer2 = (SingerInfo) object2;
			return singer1.getSingerName().compareTo(singer2.getSingerName());
		}
	}
	
	public static void startQuery(Context context,String keyword,String type){
		if (!TextUtils.isEmpty(keyword) || !TextUtils.isEmpty(type)) {
			sKeyword=keyword;
			sType=type;
			sData = null;
			sNoData = false;
			if (sSingerList!=null)
				sSingerList.notifyDataSetInvalidated();
			if (sFetchSingerListTask != null)
				sFetchSingerListTask.cancel(true);
			sFetchSingerListTask = new FetchSingerListTask(context);
			sFetchSingerListTask.execute();
		} else {
			sData = null;
			sFetchSingerListTask = null;
		}
	}
	
	private void notifyDataSetInvalidated(){
		if (mAdapter != null)
			mAdapter.notifyDataSetInvalidated();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub
		int type=sData.get(position).getType();
		if (type!=SingerInfo.ALL_SINGER_LIST_SINGER
				&& type!=SingerInfo.MATCH_SINGER_LIST_SINGER
				&& type!=SingerInfo.HOT_SINGER_LIST_SINGER
				&& type!=SingerInfo.NEW_SINGER_LIST_SINGER)
			return;
		String url = "market://search?q=pname:" + sData.get(position).getPackageName();
		try {
			Uri uri = Uri.parse(url);
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}