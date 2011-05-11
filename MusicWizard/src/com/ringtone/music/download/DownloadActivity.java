package com.ringtone.music.download;

import java.io.File;
import java.util.ArrayList;

import com.ringtone.music.R;
import com.ringtone.music.Utils;
import com.ringtone.music.download.DownloadService.ServiceToken;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
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
	
	private ArrayList<DownloadInfo> mData;
    
	private static final int MENU_CLEAR = Menu.FIRST;
	private static final int MENU_PLAY = Menu.FIRST + 1;
	private static final int MENU_STOP = Menu.FIRST + 2;
	private static final int MENU_RESUME = Menu.FIRST + 3;
	private static final int MENU_DELETE = Menu.FIRST + 4;
    
    private DownloadListAdapter mAdapter;
    private Handler mHandler = new Handler();
    private ServiceToken mToken;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		setContentView(R.layout.download);
		Utils.addAds(this);
		
        mToken = DownloadService.bindToService(this, mConnection);
		
		Button clearFinishedButton = (Button)findViewById(R.id.clear_finished_button);
		clearFinishedButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (DownloadService.sService != null) {
					DownloadService.sService.clearFinished();
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
				if (d.getStatus() == DownloadInfo.STATUS_FINISHED) {
					playDownloadedMusic(d);
				} else if (d.getStatus() == DownloadInfo.STATUS_FAILED) {
				} else if (d.getStatus() == DownloadInfo.STATUS_DOWNLOADING) {
				} else if (d.getStatus() == DownloadInfo.STATUS_PENDING) {
				} else if (d.getStatus() == DownloadInfo.STATUS_STOPPED) {
				}
				Toast.makeText(DownloadActivity.this,
						getString(R.string.music_option_prompt), Toast.LENGTH_LONG).show();
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
				if (d.getStatus() == DownloadInfo.STATUS_FINISHED) {
					menu.add(0, MENU_PLAY, 0, R.string.play);
					menu.add(0, MENU_CLEAR, 0, R.string.clear);
				} else if (d.getStatus() == DownloadInfo.STATUS_FAILED) {
					menu.add(0, MENU_RESUME, 0, R.string.retry);
					menu.add(0, MENU_DELETE, 0, R.string.delete);
				} else if (d.getStatus() == DownloadInfo.STATUS_DOWNLOADING) {
					menu.add(0, MENU_STOP, 0, R.string.stop);
				} else if (d.getStatus() == DownloadInfo.STATUS_PENDING) {
					menu.add(0, MENU_CLEAR, 0, R.string.cancel);
				} else if (d.getStatus() == DownloadInfo.STATUS_STOPPED) {
					menu.add(0, MENU_RESUME, 0, R.string.resume);
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
					if (DownloadService.sService != null) {
						mData = DownloadService.sService.getDownloadInfos();
						// Utils.D("observer onChange: " + mData.size());
						mAdapter.notifyDataSetChanged();
					}
				}
			});
		}
	};
	
	@Override
	protected void onDestroy() {
        DownloadService.unbindFromService(mToken);
		super.onDestroy();
	}
	
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	if (DownloadService.sService != null) {
				DownloadService.sService.registerDownloadObserver(mObserver);
				mAdapter = new DownloadListAdapter(DownloadActivity.this, R.layout.download_item);
				setListAdapter(mAdapter);
				mData = DownloadService.sService.getDownloadInfos();
				mAdapter.notifyDataSetChanged();
        	}
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
			if (DownloadService.sService != null) {
				DownloadService.sService.unregisterObserver(mObserver);
	        }
	    }
    };
    
    
    private void playDownloadedMusic(DownloadInfo info) {
    	if (info == null)
    		return;
    	
    	try {
    		Intent intent = new Intent(Intent.ACTION_VIEW);
    		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    		intent.setDataAndType(Uri.parse("file://" + info.getTarget()), "audio");
    		startActivity(intent);
    	} catch (android.content.ActivityNotFoundException e) {
    		e.printStackTrace();
			Toast.makeText(DownloadActivity.this,
					getString(R.string.no_playing_activity), Toast.LENGTH_LONG).show();
    	}
    }
	
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item
				.getMenuInfo();
		DownloadInfo d = mData.get(menuInfo.position);
		switch (item.getItemId()) {
		case MENU_CLEAR: {
			if (DownloadService.sService != null)
				DownloadService.sService.removeDownload(d);
			break;
		}
		case MENU_PLAY: {
			playDownloadedMusic(d);
			break;
		}
		case MENU_STOP: {
			synchronized(d) {
				d.setStatus(DownloadInfo.STATUS_STOPPED);
			}
			d.getThread().interrupt();
			mAdapter.notifyDataSetChanged();
			break;
		}
		case MENU_RESUME: {
			if (DownloadService.sService != null) {
				synchronized (d) {
					d.setStatus(DownloadInfo.STATUS_PENDING);
				}
				mAdapter.notifyDataSetChanged();
				DownloadService.sService.resumeDownload(d);
			}
			break;
		}
		case MENU_DELETE: {
			if (DownloadService.sService != null) {
				DownloadService.sService.removeDownload(d);
				File file = new File(d.getTarget());
				if (file.exists()) {
					file.delete();
				}
				
				file = new File(d.getTarget() + ".tmp");
				if (file.exists()) {
					file.delete();
				}
			}
		}
		}

		return true;
	}

    
    private final class DownloadListAdapter extends BaseAdapter {

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
			musicInfo.setText((new File(info.getTarget())).getName());
			
			ProgressBar progress = (ProgressBar)v.findViewById(R.id.progress);
			
			int percent = info.getTotalBytes() == 0 ? 0 : 100 * info.getCurrentBytes() / info.getTotalBytes();
			
			// Max can't be 0. 
			progress.setProgress(percent);
			
			/*
			Utils.D("position = " + position);
			Utils.D("currentBytes = " + info.getCurrentBytes());
			Utils.D("totalBytes = " + info.getTotalBytes());
			*/
			
			// Set status.
			TextView musicStatus = (TextView)v.findViewById(R.id.music_status);
			TextView bytesInfo = (TextView)v.findViewById(R.id.download_bytes);
			TextView error = (TextView)v.findViewById(R.id.error);
			if (info.getStatus() == DownloadInfo.STATUS_FINISHED) {
				musicStatus.setText(R.string.finished);
				musicStatus.setTextColor(getResources().getColor(R.color.download_finished));
				bytesInfo.setVisibility(View.GONE);
				error.setVisibility(View.GONE);
				Utils.assertD(info.getCurrentBytes() == info.getTotalBytes());
			} else if (info.getStatus() == DownloadInfo.STATUS_FAILED) {
				musicStatus.setText(R.string.failed);
				musicStatus.setTextColor(getResources().getColor(R.color.download_failed));
				bytesInfo.setVisibility(View.GONE);
				error.setVisibility(View.VISIBLE);
				error.setText("Error: " + info.getError());
			} else if (info.getStatus() == DownloadInfo.STATUS_DOWNLOADING){
				musicStatus.setText(R.string.downloading);
				musicStatus.setTextColor(getResources().getColor(R.color.download_pending));
				bytesInfo.setVisibility(View.VISIBLE);
				error.setVisibility(View.GONE);
				bytesInfo.setText("" + percent + "%");
			} else if (info.getStatus() == DownloadInfo.STATUS_PENDING) {
				musicStatus.setText(R.string.pending);
				musicStatus.setTextColor(getResources().getColor(R.color.download_pending));
				bytesInfo.setVisibility(View.GONE);
				error.setVisibility(View.GONE);
			} else if (info.getStatus() == DownloadInfo.STATUS_STOPPED) {
				musicStatus.setText(R.string.stopped);
				musicStatus.setTextColor(getResources().getColor(R.color.download_pending));
				bytesInfo.setVisibility(View.GONE);
				error.setVisibility(View.GONE);
			}
			return v;
		}

    }
}
