package com.trans.music.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.json.JSONArray;

import com.admob.android.ads.AdView;
import com.qwapi.adclient.android.view.QWAdView;
import com.trans.music.search.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;

import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.ImageView;

import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.Context;

import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.net.Uri;
import android.content.ContentResolver;
import android.provider.Settings;

import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.content.ContentUris;
import android.database.Cursor;
import android.content.ContentValues;

public class local extends Activity {
	// Local Playlist
	
	private static final int Mode_All_Repeat = 0;
	private static final int Mode_Shuffling = 1;
	private static final int Mode_Single_Repeat = 2;
	private static final int Seek_Interval = 500;
	private static final int Seekbar_Max = 100;

	private int mSongProgress;
	private int mPlayMode = 0;
	
	private TextView txtCurMusic;
	private TextView txtPlayMode;
	
	ListView mLocalList;
	JSONArray mLocalMp3s = new JSONArray();
	ArrayAdapter<String> mLocalAdapter;
	ArrayList<String> mLocalStrings = new ArrayList<String>();

	private MediaController.MediaPlayerControl mMediaPlayerControl = new MediaController.MediaPlayerControl() {
		int curIndex=-1;
		@Override
		public void start() {
			if (curIndex == mLocalMp3index) {
				mPlayer.start();
			} else {
				curIndex = mLocalMp3index;
				playSong();
			}
		};
		
		@Override
		public void seekTo(int pos) {
			try {
				mPlayer.seekTo(pos);
			} catch (Exception e) {
			}
		}
		
		@Override
		public void pause() {
			mPlayer.pause();
		}
		
		@Override
		public boolean isPlaying() {
			return mPlayer.isPlaying();
		}
		
		@Override
		public int getDuration() {
			int duration = 1;
			try {
				duration = mPlayer.getDuration();
			} catch (Exception e) {
			}
			return duration;
		}
		
		@Override
		public int getCurrentPosition() {
			int pos = 0;
			try {
				pos = mPlayer.getCurrentPosition();
			} catch (Exception e) {
			}
			return pos;
		}
		
		@Override
		public int getBufferPercentage() {
			return 0;
		}
		
		@Override
		public boolean canSeekForward() {
			return true;
		}
		
		@Override
		public boolean canSeekBackward() {
			return true;
		}
		
		@Override
		public boolean canPause() {
			return true;
		}
	};

	int mLocalMp3index = 0;

	Uri mCurrentFileUri;
	private boolean mChooseItem = false;

	long mSongDuration;

	ImageView mPlayStop;

	private MediaPlayer mPlayer = new MediaPlayer();
	private MediaController mController;

	@Override
	protected void onResume() {
		super.onResume();
		updateDownloadList();
		txtCurMusic.setText(getString(R.string.current_music)+":  "+mLocalStrings.get(mLocalMp3index));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.local);
		
		txtCurMusic = (TextView)findViewById(R.id.txtCurMusic);
		txtPlayMode = (TextView)findViewById(R.id.txtPlayMode);
		txtPlayMode.setText(getString(R.string.play_mode)+":  "+getResources().getStringArray(R.array.play_mode)[mPlayMode]);
	
		View anchorView = findViewById(R.id.anchorView);
		
		mController = new MediaController(this);
		mController.setPrevNextListeners(mPlayNextClickListener, mPlayPrevClickListener);
		mController.setMediaPlayer(mMediaPlayerControl);
		mController.setEnabled(true);
		mController.setAnchorView(anchorView);

/*		AdView admob = (AdView) findViewById(R.id.adMob);
		if (admob != null) {
			admob.setGoneWithoutAd(true);
		}
		QWAdView qwAdView = (QWAdView) findViewById(R.id.QWAd);
		AdListener adListener = new AdListener(this);
		qwAdView.setAdEventsListener(adListener, false);*/

		// Local Playlist UI
		mLocalList = (ListView) findViewById(R.id.local_playlist);
		mLocalAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, mLocalStrings);
		mLocalList.setAdapter(mLocalAdapter);
		mLocalList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView parent, View v, int position,
					long id) {
				mLocalMp3index = position;
				playSong();
			}
		});
		mPlayer.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				playNext();
			}
		});
		mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				Log.e("in OnError: ", "");
				if (mPlayMode != Mode_Single_Repeat) {
					playNext();
				} else {
					Toast.makeText(local.this, R.string.play_error, Toast.LENGTH_SHORT).show();
				}
				return false;
			}
		});
		mLocalList.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
						}
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mController.show();
							}
						});
					}
				}).start();
				return false;
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the currently selected menu XML resource.
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.local, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case R.id.menu_search:
			finish();
			break;
		case R.id.menu_mode:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.select_play_mode);
			builder.setSingleChoiceItems(R.array.play_mode, mPlayMode, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	mPlayMode = item;
			    	txtPlayMode.setText(getString(R.string.play_mode)+":  "+getResources().getStringArray(R.array.play_mode)[mPlayMode]);
			    	Toast.makeText(local.this, getString(R.string.play_mode_set_to)+":  "+getResources().getStringArray(R.array.play_mode)[mPlayMode], Toast.LENGTH_SHORT).show();
			    	dialog.dismiss();
			    }
			});
			AlertDialog alert = builder.create();
			alert.show();
			break;
		case R.id.menu_local:
			loadDefaultMusicApp();
			break;
		case R.id.menu_ringtone:
			if (mLocalMp3index >= 0 && mChooseItem == true) {
				try {
					ContentResolver resolver = this.getContentResolver();
					Uri ringUri = mCurrentFileUri;

					ContentValues values = new ContentValues(2);
					values.put(MediaStore.Audio.Media.IS_RINGTONE, "1");
					values.put(MediaStore.Audio.Media.IS_ALARM, "1");
					resolver.update(ringUri, values, null, null);

					Settings.System.putString(resolver,
							Settings.System.RINGTONE, ringUri.toString());
					Toast.makeText(this,
							"This playing song has set as phone ringtone.",
							Toast.LENGTH_SHORT).show();

				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				Toast.makeText(this, "Please select one music to play.",
						Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.menu_delete:
			try {
				String fileLocal = Const.homedir
						+ mLocalStrings.get(mLocalMp3index);
				File mp3 = new File(fileLocal);
				if (mp3.exists()) {
					mp3.delete();
					updateDownloadList();
					mChooseItem = false;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;
		case R.id.menu_help:
			intent = new Intent(local.this, help.class);
			startActivity(intent);
			return true;
		default:
			break;
		}

		return false;
	}

	private void updateDownloadList() {
		try {
			File[] file = (new File(Const.homedir)).listFiles();
			mLocalAdapter.clear();
			for (int i = 0; i < file.length; i++) {
				if (file[i].isFile()) {
					String fname = file[i].getName();
					if (fname.endsWith(".mp3"))
						mLocalAdapter.add(fname);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadDefaultMusicApp() {
		try {
			Intent intent = new Intent();
			intent.setClassName("com.android.music",
					"com.android.music.MusicBrowserActivity");
			startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private int getPrevIndex(int curIndex) {
		int prevIndex = 0;
		switch (mPlayMode) {
		case Mode_All_Repeat:
			prevIndex = (--curIndex)%mLocalStrings.size()+mLocalStrings.size();
			break;
		case Mode_Single_Repeat:
			prevIndex = curIndex;
			break;
		case Mode_Shuffling:
			prevIndex = new Random().nextInt(mLocalStrings.size()+1);
			break;
		default:
			break;
		}
		return prevIndex;
	}
	
	private int getNextIndex(int curIndex) {
		int nextIndex = 0;
		switch (mPlayMode) {
		case Mode_All_Repeat:
			nextIndex = (++curIndex)%mLocalStrings.size();
			break;
		case Mode_Single_Repeat:
			nextIndex = curIndex;
			break;
		case Mode_Shuffling:
			nextIndex = new Random().nextInt(mLocalStrings.size()+1);
			break;
		default:
			break;
		}
		return nextIndex;
	}
	
	private void playSong() {
		boolean isSucc = false;
		do {
			mPlayer.reset();
			txtCurMusic.setText(getString(R.string.current_music)+": "+mLocalStrings.get(mLocalMp3index));
			String fileLocal = Const.homedir
					+ mLocalStrings.get(mLocalMp3index);
			try {
				mPlayer.setDataSource(fileLocal);
				mPlayer.prepare();
				mPlayer.start();
				Toast.makeText(local.this, "Playing:  " + fileLocal,
						Toast.LENGTH_SHORT).show();
				isSucc = true;
			} catch (Exception e) {
				Toast.makeText(local.this, R.string.play_error, Toast.LENGTH_SHORT).show();
				if (mPlayMode != Mode_Single_Repeat) {
					mLocalMp3index = getNextIndex(mLocalMp3index);
				}
			}
		} while(!isSucc && mPlayMode!=Mode_Single_Repeat);
	}
	
	private OnClickListener mPlayPrevClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			playPrev();
		}
	};
	
	private void playPrev() {
		mLocalMp3index = getPrevIndex(mLocalMp3index);
		playSong();
	}
	
	private OnClickListener mPlayNextClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			playNext();
		}
	};
	
	private void playNext() {
		mLocalMp3index = getNextIndex(mLocalMp3index);
		playSong();
	}
}
