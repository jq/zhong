package com.ringtone.music.download;

import java.io.File;
import java.util.ArrayList;

import com.admob.android.ads.AdView;
import com.ringtone.music.App;
import com.ringtone.music.R;
import com.ringtone.music.Utils;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
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
    private DownloadService mDownloadService;
    
	private static final int MENU_CLEAR = Menu.FIRST;
	private static final int MENU_PLAY = Menu.FIRST + 1;
	private static final int MENU_STOP = Menu.FIRST + 2;
	private static final int MENU_RESUME = Menu.FIRST + 3;
	private static final int MENU_DELETE = Menu.FIRST + 4;
	private static final int MENU_ASSIGN = Menu.FIRST + 5;
	private static final int MENU_EDIT = Menu.FIRST + 6;
	
    private static final int REQUEST_CODE_EDIT = 1;

    
    private DownloadListAdapter mAdapter;
    private Handler mHandler = new Handler();
    
    private boolean mShowFinished;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		setContentView(R.layout.download);
		
		mShowFinished=false;
		
		Utils.addAds(this);
		bindService(new Intent(this, DownloadService.class),
				mConnection, Context.BIND_AUTO_CREATE);
		
		Button clearFinishedButton = (Button)findViewById(R.id.clear_finished_button);
		clearFinishedButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mShowFinished=false;
				synchronized(mHandler){
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							if (mDownloadService != null) {
								mDownloadService.clearFinished();
							}
							showList();
						}
					});
				}
			}
		});
		
		Button showFinishedButton = (Button)findViewById(R.id.show_finished_button);
		showFinishedButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mShowFinished=true;
				synchronized(mHandler){
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							showList();
						}
					});
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
					menu.add(0, MENU_DELETE, 0, R.string.delete);
					menu.add(0, MENU_ASSIGN, 0, R.string.assign);
					menu.add(0, MENU_EDIT, 0, R.string.edit);
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
			synchronized(mHandler){
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						if (mDownloadService != null) {
							showList();
							// Utils.D("observer onChange: " + mData.size());
						}
					}
				});
			}
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
			synchronized(mHandler){
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						showList();
					}
				});
			}
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mDownloadService = null;
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
    
    private void assignToContact(DownloadInfo info){
    	if (info == null)
    		return;
    	try{
    		Uri currentFileUri = Uri.parse(info.getTarget());
    		Intent intent = new Intent();
    		intent.setData(currentFileUri);
    		intent.setClass(this, com.ringdroid.ChooseContactActivity.class);
    		startActivity(intent);
    	} catch (android.content.ActivityNotFoundException e) {
    		e.printStackTrace();
    	}
    }
    
    private void startRingdroidEditor(DownloadInfo info) {
    	if (info == null)
    		return;
        try {
            Intent intent = new Intent(Intent.ACTION_EDIT,
                                       Uri.parse(info.getTarget()));
            intent.putExtra("was_get_content_intent",true);
            intent.setClassName(
                this,
                "com.ringdroid.RingdroidEditActivity");
            startActivityForResult(intent, REQUEST_CODE_EDIT);
        } catch (android.content.ActivityNotFoundException e) {
    		e.printStackTrace();
    	}
    }
	
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item
				.getMenuInfo();
		DownloadInfo d = mData.get(menuInfo.position);
		switch (item.getItemId()) {
		case MENU_CLEAR: {
			if (mDownloadService != null)
				mDownloadService.removeDownload(d);
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
			if (mDownloadService != null) {
				synchronized(d) {
					d.setStatus(DownloadInfo.STATUS_PENDING);
				}
				mAdapter.notifyDataSetChanged();
				mDownloadService.resumeDownload(d);
			}
			break;
		}
		case MENU_DELETE: {
			if (mDownloadService != null) {
				mDownloadService.removeDownload(d);
				File file = new File(d.getTarget());
				if (file.exists()) {
					file.delete();
					if (d.getTarget().endsWith(".mp3")){
						try{
							String selection="(_DATA = ?)";
							ArrayList<String> args = new ArrayList<String>();
							args.add(d.getTarget());
							String[] argsArray = args.toArray(new String[args.size()]);
							Cursor c=managedQuery(
						            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
						            EXTERNAL_COLUMNS,
						            selection,
						            argsArray,
						            MediaStore.Audio.Media.DATE_ADDED + " DESC");
							startManagingCursor(c);
							c.moveToFirst();
							String itemUri = c.getString(c.getColumnIndexOrThrow("\"" + MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "\""))
						   + "/" + c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
							getContentResolver().delete(Uri.parse(itemUri), null, null);
						}catch(Exception e){
						}
					}
				}
				
				file = new File(d.getTarget() + ".tmp");
				if (file.exists()) {
					file.delete();
				}
				mData.remove(d);
				mAdapter.notifyDataSetChanged();
			}
			break;
		}
		case MENU_ASSIGN: {
			assignToContact(d);
			break;
		}
		case MENU_EDIT: {
			startRingdroidEditor(d);
			break;
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
    
    private void showList(){
		if (mShowFinished==true){
			if (mDownloadService != null) {
				mDownloadService.clearFinished();
			}
			mData = mDownloadService.getDownloadInfos();
			File[] file = (new File(App.getMp3Path())).listFiles();
			for (int i = 0; i < file.length; i++) {
				if (file[i].isFile()) {
					String fname = file[i].getName();
					if (fname.endsWith(".mp3")){
						DownloadInfo downloadinfo=new DownloadInfo("", App.getMp3Path()+"/"+fname);
						int size=(int) file[i].length();
						downloadinfo.setCurrentBytes(size);
						downloadinfo.setTotalBytes(size);
						downloadinfo.setStatus(DownloadInfo.STATUS_FINISHED);
						mData.add(downloadinfo);
					}
				}
			}
		} else {
			mData = mDownloadService.getDownloadInfos();
		}
		mAdapter.notifyDataSetChanged();
    }
    private static final String[] EXTERNAL_COLUMNS = new String[] {
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.IS_RINGTONE,
        MediaStore.Audio.Media.IS_ALARM,
        MediaStore.Audio.Media.IS_NOTIFICATION,
        MediaStore.Audio.Media.IS_MUSIC,
        "\"" + MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "\""
    };
}
