package com.feebe.rings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.json.JSONArray;

import com.feebe.lib.AdListener;
import com.feebe.rings.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
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
import android.widget.AdapterView.OnItemLongClickListener;
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

	private static final int Music_Play = 0;
	private static final int Music_Edit = 1;
	private static final int Music_Delete = 2;

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
		try {
			txtCurMusic.setText(getString(R.string.current_music)+":  "+mLocalStrings.get(mLocalMp3index));
		} catch (Exception e) {
		}
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
		
        AdListener.createAds(this,R.id.l1);

		// Local Playlist UI
		mLocalList = (ListView) findViewById(R.id.local_playlist);
		mLocalAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, mLocalStrings);
		mLocalList.setAdapter(mLocalAdapter);
		mLocalList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView parent, View v, int position,
					long id) {
				final int position_final = position;
				AlertDialog.Builder builder = new AlertDialog.Builder(local.this);
				builder.setTitle(R.string.select_opration);
				builder.setItems(R.array.local_music_option, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				        switch (item) {
						case Music_Play:
							mLocalMp3index = position_final;
							playSong();
							break;
						case Music_Edit:
					        startRingdroidEditor(position_final);
							break;
						case Music_Delete:
							AlertDialog.Builder builder = new AlertDialog.Builder(local.this);
							builder.setMessage(getString(R.string.delete_warning)+":  "+mLocalStrings.get(position_final)+"?")
							       .setCancelable(false)
							       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
							           public void onClick(DialogInterface dialog, int id) {
							        	   deleteMusicFile(position_final);
							        	   updateDownloadList();
							           }
							       })
							       .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
							           public void onClick(DialogInterface dialog, int id) {
							                dialog.cancel();
							           }
							       });
							AlertDialog alert = builder.create();
							alert.show();
							break;
						default:
							break;
						}
				    }
				});
				AlertDialog alert = builder.create();
				alert.show();
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
							  if (!local.this.isFinishing()) {
								  mController.show();
							  }
							}
						});
					}
				}).start();
				return false;
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			mPlayer.stop();
			mPlayer.release();
		} catch (Exception e) {
		}
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
		switch (item.getItemId()) {
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
		default:
			break;
		}

		return false;
	}

	private void updateDownloadList() {
		try {
			File[] file = (new File(Const.contentDir)).listFiles();
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
			prevIndex = (--curIndex + mLocalStrings.size())%mLocalStrings.size();
			break;
		case Mode_Single_Repeat:
			prevIndex = curIndex;
			break;
		case Mode_Shuffling:
			prevIndex = new Random().nextInt(mLocalStrings.size());
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
			nextIndex = new Random().nextInt(mLocalStrings.size());
			break;
		default:
			break;
		}
		return nextIndex;
	}
	
	private void playSong() {
		if (mLocalStrings.size() == 0) return;
		boolean isSucc = false;
		int retryCount = 0;
		do {
			retryCount++;
			mPlayer.reset();
			txtCurMusic.setText(getString(R.string.current_music)+": "+mLocalStrings.get(mLocalMp3index));
			String fileLocal = Const.contentDir
					+ mLocalStrings.get(mLocalMp3index);
			try {
				//// Log.e("DataSource: ", fileLocal);
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
		} while(!isSucc && mPlayMode!=Mode_Single_Repeat && retryCount<=mLocalStrings.size());
		if (retryCount > mLocalStrings.size()) {
			Toast.makeText(local.this, R.string.have_no_valide_music, Toast.LENGTH_SHORT).show();
		}
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
			if (mLocalStrings.size() > 0)
			  playNext();
		}
	};
	
	private void playNext() {
		mLocalMp3index = getNextIndex(mLocalMp3index);
		playSong();
	}
	
    private void startRingdroidEditor(int index) {
    	String fileLocal = Const.contentDir + mLocalStrings.get(index);
        try {
            Intent intent = new Intent(Intent.ACTION_EDIT,
                                       Uri.parse(fileLocal));
            intent.putExtra("was_get_content_intent",
                            false);
            intent.setClassName(
            	this,
                "com.lib.RingEditor");
            startActivity(intent);
        } catch (Exception e) {
            // Log.e("Ringdroid", "Couldn't start editor");
        }
    }
    
    private void deleteMusicFile(int index) {
      String fileLocal = Const.contentDir + mLocalStrings.get(index);
      //delete from MediaStore
      try {
        ContentResolver mContentResolver = this.getContentResolver();
        Cursor cursor = mContentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        while(cursor.moveToNext()) {
          String url = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
          if(url.equals(fileLocal)) {
            String itemUri = MediaStore.Audio.Media.getContentUriForPath(url).toString() + "/" + cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
            mContentResolver.delete(Uri.parse(itemUri), null, null);
            //// Log.e("itemUri:", itemUri.toString());
          }
        }
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      }
      //delete from sdcard
      try {
        mPlayer.stop();
        mPlayer.reset();       
        File mp3 = new File(fileLocal);
        if (mp3.exists()) {
            mp3.delete();
            updateDownloadList();
            mChooseItem = false;
        }
      } catch (Exception e) {
          e.printStackTrace();
      }
    }
}
