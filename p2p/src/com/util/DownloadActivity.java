package com.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.R;
import com.limegroup.gnutella.downloader.AlreadyDownloadingException;
import com.util.DownloadInfo;
import com.util.DownloadObserver;
import com.util.DownloadService;
import com.util.P2pDownloadInfo;
import com.util.Utils;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class DownloadActivity extends ListActivity {
	
	protected ArrayList<DownloadInfo> mData;
	protected DownloadService mDownloadService;
    
	protected static final int MENU_CLEAR = Menu.FIRST;
	protected static final int MENU_STOP = Menu.FIRST + 1;
	protected static final int MENU_RESUME = Menu.FIRST + 2;
	protected static final int MENU_DELETE = Menu.FIRST + 3;
	protected static final int MENU_PAUSE = Menu.FIRST + 4;
	protected static final int MENU_RETRY = Menu.FIRST + 5;
    
	protected DownloadListAdapter mAdapter;
    private Handler mHandler = new Handler();
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		setContentView(R.layout.download);
		bindService(new Intent(this, DownloadService.class),
				mConnection, Context.BIND_AUTO_CREATE);
		
		Button clearFinishedButton = (Button)findViewById(R.id.clear_finished_button);
		clearFinishedButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mDownloadService != null) {
					mDownloadService.clearFinished();
				}
			}
		});
		
		Button hideButton = (Button)findViewById(R.id.hide_button);
		hideButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				if (mData == null || position >= mData.size())
					return;
				DownloadInfo d = mData.get(position);
				if (d == null) {
					Utils.D("No bound download info.");
					return;
				}
				// Default action.
				//int state = d.getState();
				//if (state == Downloader.COMPLETE) {
				//	playDownloadedMusic(d);
				//}
				if (d.showToastForLongPress()) {
				  Toast.makeText(DownloadActivity.this,
                      getString(R.string.music_option_prompt), Toast.LENGTH_LONG).show();
				}			
					
			}
		});
		
		getListView().setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {

			@Override
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				if (mData == null)
					return;
				
				if (!(menuInfo instanceof AdapterContextMenuInfo))
					return;
				
				int position = ((AdapterContextMenuInfo) menuInfo).position;
				if (position >= mData.size())
					return;
				
				DownloadInfo d = mData.get(position);
				if (d == null) {
					Utils.D("No bound download info.");
					return;
				}
				int state = d.getState();
				if (state == Downloader.COMPLETE) {
					//menu.add(0, MENU_PLAY, 0, R.string.play);
					menu.add(0, MENU_CLEAR, 0, R.string.clear);
				} else if (state == Downloader.CORRUPT_FILE ||
						   state == Downloader.ABORTED ||
						   state == Downloader.GAVE_UP ||
						   state == Downloader.RECOVERY_FAILED) {
					menu.add(0, MENU_RETRY, 0, R.string.retry);
					menu.add(0, MENU_DELETE, 0, R.string.delete);
				} else if (state == Downloader.DOWNLOADING ||
						   state == Downloader.CONNECTING ||
						   state == Downloader.REMOTE_QUEUED ||
						   state == Downloader.SAVING ||
						   state == Downloader.IDENTIFY_CORRUPTION ||
						   state == Downloader.WAITING_FOR_RESULTS ||
						   state == Downloader.WAITING_FOR_RETRY ||
						   state == Downloader.WAITING_FOR_CONNECTIONS ||
						   state == Downloader.ITERATIVE_GUESSING) {
					if (!d.isScheduled()) {
						menu.add(0, MENU_RETRY, 0, R.string.retry);
						menu.add(0, MENU_DELETE, 0, R.string.delete);
					} else {
						menu.add(0, MENU_PAUSE, 0, R.string.pause);
						menu.add(0, MENU_STOP, 0, R.string.stop);
					}
				} else if (state == Downloader.QUEUED) {
					// menu.add(0, MENU_STOP, 0, R.string.stop);
				} else if (state == Downloader.PAUSED ||
						   state == Downloader.WAITING_FOR_USER) {
					menu.add(0, MENU_RESUME, 0, R.string.resume);
					menu.add(0, MENU_DELETE, 0, R.string.delete);
				} else if (state == Downloader.DISK_PROBLEM){
					menu.add(0, MENU_DELETE, 0, R.string.delete);
				} else if (d.pendingFailed()) {
					menu.add(0, MENU_RETRY, 0, R.string.retry);
					menu.add(0, MENU_DELETE, 0, R.string.delete);
				}
			}
			
		});
	}
	
	
	private DownloadObserver mObserver = new DownloadObserver() {
		@Override
		public void onChange() {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					if (mDownloadService != null) {
						mData = mDownloadService.getDownloadInfos();
						// Utils.D("observer onChange: " + mData.size());
						mAdapter.notifyDataSetChanged();
					}
				}
			});
		}
	};
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mDownloadService != null)
			mDownloadService.unregisterObserver(mObserver);
		unbindService(mConnection);
	}
	
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mDownloadService = ((DownloadService.LocalBinder)service).getService();
			mDownloadService.registerDownloadObserver(mObserver);
			mAdapter = new DownloadListAdapter(DownloadActivity.this, R.layout.download_item);
			setListAdapter(mAdapter);
			mData = mDownloadService.getDownloadInfos();
			mAdapter.notifyDataSetChanged();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mDownloadService = null;
        }
    };
	
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item
				.getMenuInfo();
		DownloadInfo d = mData.get(menuInfo.position);
		switch (item.getItemId()) {
		case MENU_CLEAR: {
			if (mDownloadService != null) {
				mDownloadService.removeDownload(d);
				synchronized(d) {
				  d.setScheduled(false);
                  mDownloadService.notifyChanged();
				}
			}
			break;
		}
		case MENU_STOP: {
			synchronized(d) {
			  d.stopDownload();
			  if (mDownloadService != null)
	            mDownloadService.notifyChanged();	  
			}
			mAdapter.notifyDataSetChanged();
			break;
		}
		case MENU_RESUME: {
			if (mDownloadService != null) {
			  mDownloadService.resumeDownload(d);
			  mAdapter.notifyDataSetChanged();
			}
			break;
		}
		case MENU_DELETE: {
			if (mDownloadService != null) {
				mDownloadService.removeDownload(d);
				d.deleteDownload();
				synchronized (d) {
                  // Force existing thread to stop.
                  mDownloadService.notifyChanged();
                }
			}
			break;
		}
		case MENU_PAUSE: {
		  synchronized(d) {
		    d.pauseDownload();
		  }
		  mAdapter.notifyDataSetChanged();
		  break;
		}
		case MENU_RETRY: {
			if (mDownloadService != null) {
			    mAdapter.notifyDataSetChanged();
                mDownloadService.retryDownload(d);
			}
			break;
		}
		}

		return true;
	}

    
    protected final class DownloadListAdapter extends BaseAdapter {

    	private int mResource;
    	private LayoutInflater mInflater;
    	
    	public DownloadListAdapter(Context context, int resource) {
    		mResource = resource;
    		mInflater = (LayoutInflater)context.getSystemService(
    				Context.LAYOUT_INFLATER_SERVICE);
    	}
    	
    	@Override
    	public int getCount() {
    		if (mData == null)
    			return 0;
    		return mData.size();
    	}
    	
		@Override
		public Object getItem(int position) {
			if (mData == null)
				return null;
			
			if (position < mData.size())
				return mData.get(position);
			
			return null;
		}

		@Override
		public long getItemId(int position) {
			if (mData == null)
				return -1;
			
			if (position < mData.size())
				return position;
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
			Object item = mData.get(position);
			if (convertView == null) {
				v = mInflater.inflate(mResource, parent, false);
			} else {
				v = convertView;
			}

			DownloadInfo info = (DownloadInfo)item;
			
			TextView musicInfo = (TextView)v.findViewById(R.id.music_info);
			musicInfo.setText(info.getFileName());
			
			ProgressBar progress = (ProgressBar)v.findViewById(R.id.progress);
			
			int percent = info.getTotalBytes() == 0 ? 0 : 100 * info.getCurrentBytes() / info.getTotalBytes();
			
			// Max can't be 0. 
			progress.setProgress(percent);
			
			// Set status.
			TextView musicStatus = (TextView)v.findViewById(R.id.music_status);
			TextView bytesInfo = (TextView)v.findViewById(R.id.download_bytes);
			TextView error = (TextView)v.findViewById(R.id.error);
			
			int state = info.getState();
			
			if (state == Downloader.COMPLETE) {
				musicStatus.setText(R.string.finished);
				musicStatus.setTextColor(getResources().getColor(R.color.download_finished));
				//progress.setVisibility(View.GONE);
				bytesInfo.setVisibility(View.GONE);
				error.setVisibility(View.GONE);
				Utils.assertD(info.getCurrentBytes() == info.getTotalBytes());
			} else if (state == Downloader.GAVE_UP) {
				musicStatus.setText(R.string.failed);
				musicStatus.setTextColor(getResources().getColor(R.color.download_failed));
				bytesInfo.setVisibility(View.GONE);
				error.setVisibility(View.VISIBLE);
				error.setText("Error: maximum retries reached");
			} else if (state == Downloader.DISK_PROBLEM) {
				musicStatus.setText(R.string.failed);
				musicStatus.setTextColor(getResources().getColor(R.color.download_failed));
				bytesInfo.setVisibility(View.GONE);
				error.setVisibility(View.VISIBLE);
				error.setText("Error: disk failure");
			} else if (state == Downloader.CORRUPT_FILE) {
				musicStatus.setText(R.string.failed);
				musicStatus.setTextColor(getResources().getColor(R.color.download_failed));
				bytesInfo.setVisibility(View.GONE);
				error.setVisibility(View.VISIBLE);
				error.setText("Error: file corrupted");
			} else if (state == Downloader.WAITING_FOR_USER) {
				musicStatus.setText(R.string.failed);
				musicStatus.setTextColor(getResources().getColor(R.color.download_failed));
				bytesInfo.setVisibility(View.GONE);
				error.setVisibility(View.VISIBLE);
				error.setText("Oops, you probably can resume");
			} else if (state == Downloader.RECOVERY_FAILED) {
				musicStatus.setText(R.string.failed);
				musicStatus.setTextColor(getResources().getColor(R.color.download_failed));
				bytesInfo.setVisibility(View.GONE);
				error.setVisibility(View.VISIBLE);
				error.setText("Error: recovery failed");
			} else if (state == Downloader.DOWNLOADING ||
					   state == Downloader.CONNECTING ||
					   state == Downloader.REMOTE_QUEUED ||
					   state == Downloader.SAVING ||
					   state == Downloader.IDENTIFY_CORRUPTION ||
					   state == Downloader.WAITING_FOR_RESULTS ||
					   state == Downloader.WAITING_FOR_RETRY ||
					   state == Downloader.WAITING_FOR_CONNECTIONS ||
					   state == Downloader.ITERATIVE_GUESSING) {
				if (!info.isScheduled()) {
					musicStatus.setText(R.string.aborted);
					musicStatus.setTextColor(getResources().getColor(R.color.download_failed));
					bytesInfo.setVisibility(View.GONE);
					error.setVisibility(View.GONE);
				} else {
					musicStatus.setText(R.string.downloading);
					musicStatus.setTextColor(getResources().getColor(R.color.download_pending));
					bytesInfo.setVisibility(View.VISIBLE);
					error.setVisibility(View.GONE);
					bytesInfo.setText("" + percent + "%");
				}
			} else if (state == Downloader.QUEUED) {
				musicStatus.setText(R.string.queued);
				musicStatus.setTextColor(getResources().getColor(R.color.download_pending));
				bytesInfo.setVisibility(View.GONE);
				error.setVisibility(View.GONE);
			} else if (state == Downloader.ABORTED) {
				musicStatus.setText(R.string.aborted);
				musicStatus.setTextColor(getResources().getColor(R.color.download_failed));
				bytesInfo.setVisibility(View.GONE);
				error.setVisibility(View.GONE);
			} else if (state == Downloader.PAUSED) {
				musicStatus.setText(R.string.paused);
				musicStatus.setTextColor(getResources().getColor(R.color.download_stopped));
				bytesInfo.setVisibility(View.VISIBLE);
				error.setVisibility(View.GONE);
				bytesInfo.setText("" + percent + "%");
			} else if (info.hasFailed()) {
				musicStatus.setText(R.string.failed);
				musicStatus.setTextColor(getResources().getColor(R.color.download_failed));
				bytesInfo.setVisibility(View.GONE);
				if (!TextUtils.isEmpty(info.getError())) {
					error.setVisibility(View.VISIBLE);
					error.setText(info.getError());
				}
				musicStatus.setText(R.string.failed);
			} else if (state == DownloadInfo.PENDING) {
				musicStatus.setText(R.string.pending);
				musicStatus.setTextColor(getResources().getColor(R.color.download_pending));
				bytesInfo.setVisibility(View.GONE);
				error.setVisibility(View.GONE);
				return v;
			} else {
				Log.e(Utils.TAG, "Should not reach here");
			}
			return v;
		}

    }
}
