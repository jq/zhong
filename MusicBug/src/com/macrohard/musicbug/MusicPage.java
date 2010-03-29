package com.macrohard.musicbug;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;

import com.ringdroid.RingdroidSelectActivity;
import com.macrohard.musicbug.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.*;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MusicPage extends Activity implements SeekBar.OnSeekBarChangeListener {
	boolean mTrackAdapterCreated = false;
	SeekBar mSeekBar;
	static final int CONNECTING = 2;
	static final int DOWNLOAD_MP3FILE = 3;
	static final int CONNECT_ERROR = 7;
	boolean mProgressDialogIsOpen = false;
	int mSongProgress;
	long mSongPosition;
	long mSongDuration;
	String mMp3Location;
	boolean mDownloadFinish = false;
	boolean mIsPlaying = false;
	String mMp3Songer;
	String mMp3Title;
	float mRate;
	String m_CurDownloadFile;
	Button btnPreview;
	Button btnDownload;
	Button btnQueue;
	private boolean mPaused = false, mDownloading = false;
	private static final int REFRESH = 1;
	private static final int RM_CON_DIALOG = 2;
	private ImageView mPlayStop;
	private ListView listSearchOthers;
	ProgressDialog mProgressDialog, mProgressDialogSearch,
	mProgressDialogPrepare, mProgressDownload;
	MediaScannerConnection mScanner;

	private FileManager mFilesManager;
	
	MediaPlayer mPlayer = new MediaPlayer();;

	private void getMediaInfo(Intent intent) {
		mMp3Location = intent.getStringExtra(Const.MP3LOC);
		mMp3Title = intent.getStringExtra(Const.MP3TITLE);
		mMp3Songer = intent.getStringExtra(Const.MP3SONGER);
		mRate = Float.parseFloat(intent.getStringExtra(Const.MP3RATE));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.getMediaInfo(this.getIntent());
		setContentView(R.layout.music_display);

		mFilesManager = FileManager.getInstance(getApplication());

		btnPreview = (Button) findViewById(R.id.preview);
		btnPreview.setOnClickListener(previewClick);

		btnDownload = (Button) findViewById(R.id.download);
		btnDownload.setOnClickListener(downloadClick);

		btnQueue = (Button) findViewById(R.id.queue);
		btnQueue.setOnClickListener(queueClick);

		mSeekBar = (SeekBar) findViewById(R.id.play_seek_bar);
		mSeekBar.setOnSeekBarChangeListener(this);
		mSeekBar.setMax(1000);
		SeekBarSetSecondaryProgress(1000);
		mSeekBar.setEnabled(false);

		mPlayStop = (ImageView) findViewById(R.id.play_stop);
		mPlayStop.setOnClickListener(mPlayStopListener);
		mPlayStop.setEnabled(false);

		((TextView) findViewById(R.id.row_title)).setText(mMp3Title);
		((TextView) findViewById(R.id.row_artist)).setText(mMp3Songer);
		RatingBar rb = ((RatingBar) findViewById(R.id.row_small_ratingbar));
		rb.setIsIndicator(true);
		rb.setRating(mRate);

		listSearchOthers = (ListView) findViewById(R.id.list_searchOthers);
		ArrayList<HashMap<String, String>> ringlist = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> map1 = new HashMap<String, String>();
		HashMap<String, String> map2 = new HashMap<String, String>();
		map1.put("ItemTitle", this.getString(R.string.search_more) + " "
				+ this.mMp3Songer);
		map2.put("ItemTitle", this.getString(R.string.search_more) + " "
				+ this.mMp3Title);

		ringlist.add(map1);
		ringlist.add(map2);
		SimpleAdapter mSearchOthers = new SimpleAdapter(this, ringlist,
				R.layout.ring_list_item, new String[] { "ItemTitle" },
				new int[] { R.id.ringListItem1 });
		listSearchOthers.setAdapter(mSearchOthers);
		listSearchOthers.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				switch (position) {
				// 0 is the singer name create in before 1 is the title.
				case 0:
					Intent intent1 = new Intent();
					intent1.putExtra(Const.Key, mMp3Songer);
					intent1.setClass(MusicPage.this, Mp3ListActivity.class);
					startActivityForResult(intent1, 1);
					return;
				case 1:
					Intent intent2 = new Intent();
					intent2.putExtra(Const.Key, mMp3Title);
					intent2.setClass(MusicPage.this, Mp3ListActivity.class);
					startActivityForResult(intent2, 1);
					return;
				}
			}

		});
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();
		/*
		if (mPlayer != null && mPlayer.isPlaying()) {
			mPlayer.stop();
		}
		*/
	}

	private OnClickListener mPlayStopListener = new OnClickListener() {
		public void onClick(View v) {
			try {
				if (mPaused == false) {
					mPlayer.pause();
					mPlayStop.setImageResource(R.drawable.play);
					mPaused = true;
				} else {
					mPlayer.start();
					mPlayStop.setImageResource(R.drawable.stop);
					mPaused = false;
					long next = refreshSeekBarNow();
					queueNextRefresh(next);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	private long refreshSeekBarNow() {
		try {
			long pos = mPlayer.getCurrentPosition();
			long remaining = 1000 - (pos % 1000);
			if ((pos >= 0) && (mSongDuration > 0)) {

				SeekBarSetProgress((int) (1000 * pos / mSongDuration));
			} else {
				SeekBarSetProgress(1000);
			}
			// return the number of milliseconds until the next full second, so
			// the counter can be updated at just the right time
			return remaining;
		} catch (Exception ex) {
			;
		}
		return 500;
	}

	private void queueNextRefresh(long delay) {
		if (!mPaused) {
			Message msg = mHandler.obtainMessage(REFRESH);
			mHandler.removeMessages(REFRESH);
			mHandler.sendMessageDelayed(msg, delay);
		}
	}

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case REFRESH:
				long next = refreshSeekBarNow();
				queueNextRefresh(next);
				break;

			case RM_CON_DIALOG:
				Log.e("MusicPage", "Connecting dialog closed");
				showConnectDiaglog(false);
				break;
			default:
				break;
			}
		}
	};

	void showConnectDiaglog(final boolean show) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				if (show == true)
					showDialog(CONNECTING);
				else
					removeDialog(CONNECTING);
			}
		});
	}

	private void ButtonsSetEnalbe(final boolean enable) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				mPlayStop.setEnabled(true);
				mPlayStop.setImageResource(R.drawable.stop);

			}
		});
	}

	void showConnectErrorDiaglog(final boolean show) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				if (show == true)
					showDialog(CONNECT_ERROR);
				else
					removeDialog(CONNECT_ERROR);
			}
		});
	}


	OnClickListener previewClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			showConnectDiaglog(true);

			new Thread(new Runnable(){
				public void run(){
					try {
						Log.e("MusicPage","into new thread");
						mPlayer.stop();
						Log.e("MusicPage","media service stopped");
						mPlayer.reset();
						mPlayer.setDataSource(mMp3Location);

						Log.e("MusicPage", "media file opened");
						mHandler.sendEmptyMessage(RM_CON_DIALOG);
						mPlayer.prepare();
						mPlayer.start();

						mProgressDialogIsOpen = false;
						SeekBarSetEnalbe(true);
						ButtonsSetEnalbe(true);
						mSongDuration = mPlayer.getDuration(); 

						long next = refreshSeekBarNow();
						queueNextRefresh(next);
					} catch (IllegalArgumentException e) {
						showConnectDiaglog(false);
						showConnectErrorDiaglog(true);

						e.printStackTrace();
					} catch (IllegalStateException e) {
						showConnectDiaglog(false);
						showConnectErrorDiaglog(true);

						e.printStackTrace();
					} catch (IOException e) {
						showConnectDiaglog(false);
						showConnectErrorDiaglog(true);

						e.printStackTrace();
					}
				}
			}).start();
			Log.e("MusicPage", "media played");

		}

	};


	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONNECTING: {
			mProgressDialog = new ProgressDialog(MusicPage.this);
			mProgressDialog.setMessage("Please wait while connect...");
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setCancelable(true);
			mProgressDialogIsOpen = true;
			return mProgressDialog;
		}
		// download progress
		case DOWNLOAD_MP3FILE: {
			mProgressDownload = new ProgressDialog(MusicPage.this);
			// mProgressDownload.setMessage("Download mp3 file : \n" +
			// mMp3Location.substring(0, 64));
			mProgressDownload.setMessage("Download mp3 file ...");
			mProgressDownload.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDownload.setMax(1000);
			mProgressDownload.setButton("Hide",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					mProgressDownload.hide();
					/* User clicked Yes so do some stuff */
				}
			});
			mProgressDialogIsOpen = true;
			return mProgressDownload;
		}

		case CONNECT_ERROR: {
			return new AlertDialog.Builder(MusicPage.this).setIcon(R.drawable.icon)
			.setTitle("Connect error ! ").setMessage(
			"This music link is invalid, please try anothor.").setCancelable(
					true).setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

						}
					}).create();

		}
		}
		return null;
	}

	OnClickListener downloadClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			btnQueue.setVisibility(View.GONE);
			removeDialog(CONNECTING);
			mProgressDialogIsOpen = false;
			if (!mDownloadFinish) {
				// if(mDlService.mDownloading == false){
				if (mDownloading == false) {
					try {
						try {
							if (mPlayer.isPlaying()) {
								mPlayer.pause();
								// mPlayStop.setImageResource(R.drawable.play);
								mPaused = true;
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						showDialog(DOWNLOAD_MP3FILE);
						m_CurDownloadFile = mMp3Title + "[" + mMp3Songer + "]"
						+ ".mp3";
						new DownloadTask(false).execute(mMp3Location);
						/*
						 * (new Thread() { public void run() { m_CurDownloadFile
						 * = mMp3title + "[" + mMp3songer + "]" + ".mp3"; new
						 * DownloadTask().execute(mMp3Location);
						 * //DownloadMusic(m_CurDownloadFile, mMp3Location); }
						 * }).start();
						 */
						// }else
						// Toast.makeText(MusicPage.this,
						// "Please select link in search results first",
						// Toast.LENGTH_SHORT).show();
						// }else
						// Toast.makeText(MusicPage.this,
						// "Please search music first",
						// Toast.LENGTH_SHORT).show();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					mProgressDownload.show();
				}
			} else {
				if (!mIsPlaying) {
					new Thread(new Runnable() {
						public void run() {
							try {
								Log.e("MusicPage", "into new thread");
								mPlayer.stop();
								Log.e("MusicPage", "media service stopped");

								String mp3Path = mFilesManager.getHomeDir() + m_CurDownloadFile;
								
								Debug.D("mp3Path = " + mp3Path);
								
								mPlayer.reset();
								mPlayer.setDataSource(mp3Path);
								Log.e("MusicPage", "media file opened");
								//mHandler.sendEmptyMessage(RM_CON_DIALOG);
								mPlayer.prepare();
								mPlayer.start();

								mProgressDialogIsOpen = false;
								SeekBarSetEnalbe(true);
								ButtonsSetEnalbe(true);
								mSongDuration = mPlayer.getDuration();

								long next = refreshSeekBarNow();
								queueNextRefresh(next);
							} catch (IllegalArgumentException e) {
								showConnectDiaglog(false);
								showConnectErrorDiaglog(true);

								e.printStackTrace();
							} catch (IllegalStateException e) {
								showConnectDiaglog(false);
								showConnectErrorDiaglog(true);

								e.printStackTrace();
							} catch (IOException e) {
								showConnectDiaglog(false);
								showConnectErrorDiaglog(true);

								e.printStackTrace();
							}
						}
					}).start();
				} else {

				}
			}
		}
	};

	OnClickListener queueClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			try {
				m_CurDownloadFile = mMp3Title + "[" + mMp3Songer + "]" + ".mp3";
				new DownloadTask(true).execute(mMp3Location);
				Toast.makeText(MusicPage.this, R.string.queue_message, Toast.LENGTH_SHORT).show();
				MusicPage.this.finish();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	};


	private void SeekBarSetSecondaryProgress(final int progress) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				mSeekBar.setSecondaryProgress(progress);
			}
		});
	}

	private void SeekBarSetProgress(final int progress) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				mSeekBar.setProgress(progress);
			}
		});
	}

	private void SeekBarInit() {
		this.runOnUiThread(new Runnable() {
			public void run() {
				mSeekBar.setMax(1000);
				mSeekBar.setProgress(0);
			}
		});
	}

	private void ProgressDiagClose() {
		this.runOnUiThread(new Runnable() {
			public void run() {
				removeDialog(CONNECTING);
				mProgressDialogIsOpen = false;

			}
		});
	}

	private void SeekBarSetEnalbe(final boolean enable) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				mSeekBar.setEnabled(enable);
			}
		});
	}

	@Override
	public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
		// TODO Auto-generated method stub
		mSongProgress = progress;
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		try {
			mSongPosition = mPlayer.getCurrentPosition(); 
				
			long position = (mSongDuration * mSongProgress) / 1000;
			mPlayer.seekTo((int)position);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void DownloadSetProgress(final int progress) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				mProgressDownload.setProgress(progress);
			}
		});
	}

	private void DownloadSetMax(final int progress) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				mProgressDownload.setMax(progress);
			}
		});
	}

	private void DownloadShowMessage(final String message) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(MusicPage.this, message, Toast.LENGTH_LONG).show();
			}
		});
	}

	private void ShowToastMessage(final String message) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(MusicPage.this, message, Toast.LENGTH_SHORT).show();
			}
		});
	}

	private class DownloadTask extends UserTask<String, Integer, Integer> {
		boolean isQueue;

		public DownloadTask(boolean isQueue) {
			super();
			this.isQueue = isQueue;
		}

		public Integer doInBackground(String... urls) {
			URL url = null;
			HttpURLConnection urlConn = null;

			String urlString;

			urlString = urls[0];
			if (!isQueue) {
				DownloadSetProgress(0);
				mDownloading = true;

				try {
					url = new URL(urlString);
					urlConn = (HttpURLConnection) url.openConnection();
					urlConn
					.setRequestProperty(
							"User-Agent",
							"Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3 -Java");

					urlConn.connect();

					int downsize = urlConn.getContentLength();
					int downed = 0;

					DownloadSetMax(downsize);

					DataInputStream fileStream;
					String fullpathname = mFilesManager.getHomeDir() + m_CurDownloadFile;
					// FileOutputStream filemp3 = openFileOutput(filename,
					// MODE_WORLD_READABLE);
					FileOutputStream filemp3 = new FileOutputStream(fullpathname);

					byte[] buff = new byte[64 * 1024];
					int len;
					fileStream = new DataInputStream(new BufferedInputStream(urlConn
							.getInputStream()));
					while ((len = fileStream.read(buff)) > 0) {
						filemp3.write(buff, 0, len);
						downed += len;
						publishProgress((int) downed);
					}

					filemp3.close();
					return 1;
				} catch (IOException e) {
					e.printStackTrace();
					return 0;
				}
			} else {
				// Queued downloading.
				try {
					url = new URL(urlString);
					urlConn = (HttpURLConnection) url.openConnection();
					urlConn
					.setRequestProperty(
							"User-Agent",
							"Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3 -Java");

					urlConn.connect();

					int downed = 0;

					DataInputStream fileStream;
					String fullpathname = mFilesManager.getHomeDir() + m_CurDownloadFile;
					// FileOutputStream filemp3 = openFileOutput(filename,
					// MODE_WORLD_READABLE);
					FileOutputStream filemp3 = new FileOutputStream(fullpathname);
					byte[] buff = new byte[64 * 1024];
					int len;
					fileStream = new DataInputStream(new BufferedInputStream(urlConn
							.getInputStream()));
					while ((len = fileStream.read(buff)) > 0) {
						filemp3.write(buff, 0, len);
						downed += len;
					}

					filemp3.close();
					return 1;
				} catch (IOException e) {
					e.printStackTrace();
					return 0;
				}
			}
		}

		public void onProgressUpdate(Integer... progress) {
			if (!isQueue) {
				mProgressDownload.setProgress(progress[0]);
			} 
		}

		public void onPostExecute(Integer result) {
			if (result == 1) {
				Debug.D("Dowload successful: " + isQueue);
				
				Toast.makeText(MusicPage.this,
						m_CurDownloadFile + getString(R.string.download_finished), Toast.LENGTH_LONG).show();
				// DownloadShowMessage(m_CurDownloadFile + " download finished");
				// updateDownloadList();
				String fullpathname = mFilesManager.getHomeDir() + "/" + m_CurDownloadFile;
				ScanMediafile(fullpathname);
				// showDownloadOKNotification(m_CurDownloadFile);
				if (isQueue) {
					Debug.D("Sending notification");
					// Intent intent = new Intent(MusicPage.this, RingdroidSelectActivity.class);
					Intent intent = new Intent(MusicPage.this, MusicSearch.class);
					Utils.addNotification(MusicPage.this, intent, mMp3Title, R.string.app_name,
							R.string.saved, R.string.app_name, R.string.saved);
				} else {
					btnDownload.setText(R.string.play);
				}
				saveArtistAndTitle();
			}
			if (result == 0) {
				Toast.makeText(MusicPage.this, mMp3Title + getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
			}
			if (!isQueue) {
				removeDialog(DOWNLOAD_MP3FILE);
				mDownloading = false;
				mDownloadFinish = true;
				mProgressDialogIsOpen = false;
			}
		}
	}

	private void saveArtistAndTitle() {
		saveData(mMp3Songer, Const.MP3SONGER);
	}

	private void saveData(String key, String item) {
		if (key.length() > 0) {
			SharedPreferences s = getSharedPreferences(item, 0);
			Editor e = s.edit();
			e.putBoolean(key, true);
			e.commit();
		}
	}

	private void ScanMediafile(final String fullpathame) {

		mScanner = new MediaScannerConnection(getApplicationContext(),
				new MediaScannerConnectionClient() {
			public void onMediaScannerConnected() {
				mScanner.scanFile(fullpathame, null /* mimeType */);
			}

			public void onScanCompleted(String path, Uri uri) {
				if (path.equals(fullpathame)) {
					mScanner.disconnect();
				}
			}

		});
		mScanner.connect();

	}
}
