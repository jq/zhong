package com.popczar.music;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import org.apache.commons.io.comparator.LastModifiedFileComparator;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ViewDownloadedActivity extends ListActivity {
	
	private static File[] sFiles;
	private static ViewDownloadedActivity sActivity;
	private static ListDownloadedFilesTask sTask;
	
	private FileListAdapter mAdapter;
	private ProgressBar mProgress;
	private TextView mMessage;
	
	private static File BASE_DIR = new File("/sdcard/music_wizard/mp3");
	
	public static void listFiles() {
		if (sTask != null)
			sTask.cancel(true);
		sTask = new ListDownloadedFilesTask();
		sTask.execute();
	}
	
	@Override
	protected void onListItemClick(ListView listView, View view, int position, long id) {
		if (sFiles != null && position < sFiles.length) {
			try {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				//intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.setDataAndType(Uri.parse("file://" + sFiles[position].getAbsolutePath()), "audio");
				startActivity(intent);
			} catch (android.content.ActivityNotFoundException e) {
				e.printStackTrace();
				Toast.makeText(ViewDownloadedActivity.this,
						getString(R.string.no_playing_activity), Toast.LENGTH_LONG).show();
			}
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
        mAdapter = new FileListAdapter(ViewDownloadedActivity.this, R.layout.music_item);
		setListAdapter(mAdapter);
		
		if (sFiles == null || sFiles.length == 0) {
			if (sTask != null) {
				mProgress.setVisibility(View.VISIBLE);
	    		mMessage.setText(getString(R.string.loading));
			} else {
				mProgress.setVisibility(View.GONE);
				mMessage.setText(getString(R.string.no_downloaded_files));
			}
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		sActivity = this;
		setContentView(R.layout.music_list);
		
		mProgress = (ProgressBar)findViewById(R.id.list_progress);
		mMessage = (TextView)findViewById(R.id.list_message);
		
		Button returnButton = (Button)findViewById(R.id.return_button);
		returnButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				finish();
			}
			
		});
	}
	
	private void showFiles(File[] files) {
		sFiles = files;
		
		if (mAdapter == null) {
	        mAdapter = new FileListAdapter(ViewDownloadedActivity.this, R.layout.music_item);
			setListAdapter(mAdapter);
		}
		
		if (files == null || files.length == 0) {
			mAdapter.notifyDataSetInvalidated();
			mProgress.setVisibility(View.GONE);
			mMessage.setText(getString(R.string.no_downloaded_files));
		} else {
			mAdapter.notifyDataSetChanged();
		}
	}
	
	private static class ListDownloadedFilesTask extends AsyncTask<Void, Void, File[]> {
		
		private class Filter implements FilenameFilter {
			@Override
			public boolean accept(File dir, String filename) {
				return filename != null && filename.endsWith(".mp3");
			}
		}
		
		Filter mFilter = new Filter();
		
		@Override
		protected void onPostExecute(File[] files) {
			sFiles = files;
			sTask = null;
			if (sActivity != null)
				sActivity.showFiles(files);
		}

		@Override
		protected File[] doInBackground(Void... params) {
			File[] files = BASE_DIR.listFiles(mFilter);
			if (files != null)
				Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
			return files;
		}
	}
	
	
    private final class FileListAdapter extends BaseAdapter {

    	private int mResource;
    	private LayoutInflater mInflater;
    	
    	public FileListAdapter(Context context, int resource) {
    		mResource = resource;
    		mInflater = (LayoutInflater)context.getSystemService(
    				Context.LAYOUT_INFLATER_SERVICE);
    	}
    	
    	@Override
    	public int getCount() {
    		if (sFiles == null)
    			return 0;
    		return sFiles.length;
    	}
    	
		@Override
		public Object getItem(int position) {
			if (sFiles == null)
				return null;
			
			if (position < sFiles.length)
				return sFiles[position];
			return null;
		}

		@Override
		public long getItemId(int position) {
			if (sFiles == null)
				return -1;
			
			if (position < sFiles.length)
				return (long)position;
			
			return -1;
		}
		
		@Override
	    public int getViewTypeCount() {
			return 1;
	    }

	    @Override
	    public int getItemViewType(int position) {
	    	return 0;
	    }

		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			String item = sFiles[position].getName();
			if (convertView == null) {
				v = mInflater.inflate(mResource, parent, false);
			} else {
				v = convertView;
			}

			TextView filename = (TextView)v.findViewById(R.id.filename);
			filename.setText(item);
			
			return v;
		}
    }
}
