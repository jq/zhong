package com.ringtone.music;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import org.apache.commons.io.comparator.LastModifiedFileComparator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
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
	
	private static final int DIALOG_LIBRARY_ITEM_OPTIONS = 1;
	private static final int DIALOG_DELETE_CONFIRMATION = 2;
	
	private static final int MUSIC_OPTION_PLAY = 0;
	private static final int MUSIC_OPTION_DELETE = 1;
	
	private static File[] sFiles;
	private static ViewDownloadedActivity sActivity;
	private static ListDownloadedFilesTask sTask;
	
	private File mCurrentFile;
	
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
			mCurrentFile = sFiles[position];
			showDialog(DIALOG_LIBRARY_ITEM_OPTIONS);
		}
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case DIALOG_DELETE_CONFIRMATION: {
			if (mCurrentFile != null)
				dialog.setTitle("Are you sure to delete " + mCurrentFile.getName() + "?");
			return;
		}
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_LIBRARY_ITEM_OPTIONS:
            return new AlertDialog.Builder(this)
                .setTitle(R.string.options)
                .setItems(R.array.music_library_item_options, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if (mCurrentFile == null)
								return;
							
							switch (which) {
							case MUSIC_OPTION_PLAY:
								try {
									Intent intent = new Intent(Intent.ACTION_VIEW);
									//intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									intent.setDataAndType(Uri.parse("file://" + mCurrentFile.getAbsolutePath()), "audio");
									startActivity(intent);
								} catch (android.content.ActivityNotFoundException e) {
									e.printStackTrace();
									Toast.makeText(ViewDownloadedActivity.this,
											getString(R.string.no_playing_activity), Toast.LENGTH_LONG).show();
								}
								break;
							case MUSIC_OPTION_DELETE:
								showDialog(DIALOG_DELETE_CONFIRMATION);
								break;
							}
						}
                })
                .create();
            
			
		case DIALOG_DELETE_CONFIRMATION:
            return new AlertDialog.Builder(ViewDownloadedActivity.this)
            .setIcon(R.drawable.alert_dialog_icon)
            .setTitle(R.string.delete_confirmation)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	if (mCurrentFile != null) {
                		mCurrentFile.delete();
                		listFiles();
                		
                		// Delete from db. This may take long.
                		Uri musics = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                		ContentResolver cr = getContentResolver();
                		int n = 0;
                		if (!mCurrentFile.getAbsolutePath().startsWith("/mnt")) {
	                		cr.delete(musics, MediaStore.MediaColumns.DATA + "=?", new String[] { "/mnt" + mCurrentFile.getAbsolutePath() });
                		}
                		if (n == 0) {
                			cr.delete(musics, MediaStore.MediaColumns.DATA + "=?", new String[] { mCurrentFile.getAbsolutePath() });
                		}
                	}
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	// Do nothing.
                }
            })
            .create();
		}
		return null;
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
		Utils.addAds(this);
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
