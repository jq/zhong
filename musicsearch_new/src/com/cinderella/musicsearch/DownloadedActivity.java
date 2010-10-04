package com.cinderella.musicsearch;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import com.libhy.RingSelect;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DownloadedActivity extends ListActivity {
	
	private static final int DIALOG_LIBRARY_ITEM_OPTION = 1;
	private static final int DIALOG_DELETE_CONFIRMATION = 2;
	private static final int DIALOG_SORT = 3;
	
	private static final int MUSIC_OPTION_PLAY = 0;
	private static final int MUSIC_OPTION_EDIT = 1;
	private static final int MUSIC_OPTION_DELETE = 2;
	
	private static final int SORT_TIME_DES = 0;
	private static final int SORT_TIME_AES = 1;
	private static final int SORT_NAME_AES = 2;
	private static final int SORT_NAME_DES = 3;

	private static ArrayList<DownloadedMusicInfo> sDownloadedMusicInfoList;
	private static FetchDownloadedMusicTask sFetchDownloadedMusicTask;
	private static TextView sLoadingMessage;
	private static ProgressBar sProgressBar;
	private static Button sRetryButton;
	private static DownloadedMusicInfo sCurDownloadedMusicInfo;
	private static Button sRefreshButton;
	private static Button sSortButton;
	
	private DownloadedAdapter mAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.downloaded_activity);
		
		sProgressBar = (ProgressBar) findViewById(R.id.search_progress);
		sLoadingMessage = (TextView) findViewById(R.id.search_message);
		sRetryButton = (Button) findViewById(R.id.retry_button);
		sRetryButton.setOnClickListener(new retryClickListener());
		sRefreshButton = (Button) findViewById(R.id.refresh_button);
		sRefreshButton.setOnClickListener(new refreshClickListener());
		sSortButton = (Button) findViewById(R.id.sort_button);
		sSortButton.setOnClickListener(new sortClickListener());
		fetchDownloadedMusiTask();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (sDownloadedMusicInfoList!=null && position<sDownloadedMusicInfoList.size()) {
			sCurDownloadedMusicInfo = sDownloadedMusicInfoList.get(position);
			showDialog(DIALOG_LIBRARY_ITEM_OPTION);
		} 
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_LIBRARY_ITEM_OPTION:
			return new AlertDialog.Builder(this)
				.setTitle(R.string.options)
				.setItems(R.array.music_library_item_options, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (sCurDownloadedMusicInfo == null) {
							return;
						}
						switch (which) {
						case MUSIC_OPTION_PLAY:
							Utils.startMusicPlayer(DownloadedActivity.this, sCurDownloadedMusicInfo.getFullPath());
							break;
						case MUSIC_OPTION_EDIT:
							RingSelect.startPureEditor(DownloadedActivity.this, sCurDownloadedMusicInfo.getFullPath());
							break;
						case MUSIC_OPTION_DELETE:
							showDialog(DIALOG_DELETE_CONFIRMATION);
						default:
							break;
						}
					}
				}).create();
		case DIALOG_DELETE_CONFIRMATION:
			return new AlertDialog.Builder(this)
				.setTitle(this.getString(R.string.alert_delet)+sCurDownloadedMusicInfo.getFileName())
				.setIcon(R.drawable.alert_dialog_icon)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Utils.deleteFile(sCurDownloadedMusicInfo.getFullPath());
						Utils.deleteFromMediaStore(DownloadedActivity.this, sCurDownloadedMusicInfo.getFullPath());
						sDownloadedMusicInfoList.remove(sCurDownloadedMusicInfo);
						sCurDownloadedMusicInfo = null;
						mAdapter.notifyDataSetChanged();
						if (sDownloadedMusicInfoList.size() == 0) {
							setEmptyStatus();
						}
					}
				}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}).create();
		case DIALOG_SORT:
			return new AlertDialog.Builder(this)
				.setTitle(R.string.sort_by)
				.setItems(R.array.sort_by_items, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case SORT_TIME_DES:
							sortByDate(false);
							break;
						case SORT_TIME_AES:
							sortByDate(true);
							break;
						case SORT_NAME_AES:
							sortByName(true);
							break;
						case SORT_NAME_DES:
							sortByName(false);
							break;
						default:
							break;
						}
					}
				}).create();
		default:
			break;
		}
		return null;
	}

	private void fetchDownloadedMusiTask() {
		if (sFetchDownloadedMusicTask != null) {
			sFetchDownloadedMusicTask.cancel(true);
		}
		sFetchDownloadedMusicTask = new FetchDownloadedMusicTask();
		sFetchDownloadedMusicTask.execute();
	}
	
	private void refreshDownloadedList() {
		mAdapter = null;
		mAdapter = new DownloadedAdapter(DownloadedActivity.this, R.layout.downloaded_item);
		fetchDownloadedMusiTask();
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
			if (sDownloadedMusicInfoList.size() == 0) {
				setEmptyStatus();
			}
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
		public String getFullPath() {
			return Const.sMusicDir+mFileName;
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
	
	private class refreshClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			refreshDownloadedList();
		}
	}
	
	private class retryClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			refreshDownloadedList();
		}
	}
	
	private class sortClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			showDialog(DIALOG_SORT);
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
	
	private void setEmptyStatus() {
		sProgressBar.setVisibility(View.GONE);
		sLoadingMessage.setVisibility(View.VISIBLE);
		sRetryButton.setVisibility(View.GONE);
		sLoadingMessage.setText(R.string.downloaded_empty);
	}
	
	private void sortByName(boolean isAscending) {
		if (sDownloadedMusicInfoList == null) {
			return;
		}
		for (int i=0; i<sDownloadedMusicInfoList.size()-1; i++) {
			for (int j=i+1; j<sDownloadedMusicInfoList.size(); j++) {
				if (isAscending) {
					if (sDownloadedMusicInfoList.get(i).getFileName().compareTo(sDownloadedMusicInfoList.get(j).getFileName()) > 0) {
						swapMusicInfos(i, j);
					} 
				} else {
					if (sDownloadedMusicInfoList.get(i).getFileName().compareTo(sDownloadedMusicInfoList.get(j).getFileName()) < 0) {
						swapMusicInfos(i, j);
					}
				}
			}
		}
		mAdapter.notifyDataSetChanged();
	}
	
	private void sortByDate(boolean isAscending) {
		if (sDownloadedMusicInfoList == null) {
			return;
		}
		for (int i=0; i<sDownloadedMusicInfoList.size()-1; i++) {
			for (int j=i+1; j<sDownloadedMusicInfoList.size(); j++) {
				if (isAscending) {
					if (sDownloadedMusicInfoList.get(i).getLastModified() > sDownloadedMusicInfoList.get(j).getLastModified()) {
						swapMusicInfos(i, j);
					}
				} else {
					if (sDownloadedMusicInfoList.get(i).getLastModified() < sDownloadedMusicInfoList.get(j).getLastModified()) {
						swapMusicInfos(i, j);
					}
				}
			}
		}
		mAdapter.notifyDataSetChanged();
	}
	
	private void swapMusicInfos(int i, int j) {
		DownloadedMusicInfo temp = null;
		temp = sDownloadedMusicInfoList.get(i);
		sDownloadedMusicInfoList.set(i, sDownloadedMusicInfoList.get(j));
		sDownloadedMusicInfoList.set(j, temp);
	}
}
