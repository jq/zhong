package com.macrohard.musicbug;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import com.macrohard.musicbug.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaScannerConnection.*;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;

import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MusicPage extends Activity {
	boolean mTrackAdapterCreated = false;

	static final int CONNECTING = 2;
	static final int DOWNLOAD_MP3FILE = 3;
	static final int CONNECT_ERROR = 7;
	boolean mProgressDialogIsOpen = false;

	String mMp3Location;
	String mMp3Singer;
	String mMp3Title;

	String mDownloadFile;
	Button btnPreview;
	Button btnDownload;
	Button btnQueue;
	Button btnPlay;
	Button btnStop;
	private boolean mDownloading = false;

	private ListView listSearchOthers;
	ProgressDialog mProgressDialog;
	ProgressDialog mProgressDialogSearch;
	ProgressDialog mProgressDialogPrepare;
	ProgressDialog mProgressDownload;
	ProgressDialog mStreaming;
	MediaScannerConnection mScanner;

	private FileManager mFilesManager;

	MediaPlayer mPlayer = new MediaPlayer();;

	private void getMediaInfo(Intent intent) {
		mMp3Location = intent.getStringExtra(Const.MP3LOC);
		mMp3Title = intent.getStringExtra(Const.MP3TITLE);
		mMp3Singer = intent.getStringExtra(Const.MP3SINGER);
		mDownloadFile = intent.getStringExtra(Const.MP3DOWNLOADFILE);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getMediaInfo(getIntent());
		setContentView(R.layout.music_display);
		Ads.createQWAd(this);

		// A hack
		if (TextUtils.isEmpty(mMp3Location)) {
			finish();
			return;
		}
		
		mFilesManager = FileManager.getInstance(getApplication());

		btnPreview = (Button) findViewById(R.id.preview);
		btnPreview.setOnClickListener(previewClick);

		btnDownload = (Button) findViewById(R.id.download);
		btnDownload.setOnClickListener(downloadClick);

		btnQueue = (Button) findViewById(R.id.queue);
		btnQueue.setOnClickListener(queueClick);

		btnPlay = (Button)findViewById(R.id.play);
		btnPlay.setOnClickListener(playClick);
		
		btnStop = (Button)findViewById(R.id.stop);
		btnStop.setOnClickListener(stopClick);

		/*
		if (!TextUtils.isEmpty(mDownloadFile) && mPlayer.isPlaying()) {
			btnDownload.setVisibility(View.GONE);
			btnStop.setVisibility(View.VISIBLE);
			btnPlay.setVisibility(View.GONE);
		}
		*/

		((TextView) findViewById(R.id.row_title)).setText(mMp3Title);
		((TextView) findViewById(R.id.row_artist)).setText(mMp3Singer);

		listSearchOthers = (ListView) findViewById(R.id.list_searchOthers);
		ArrayList<HashMap<String, String>> ringlist = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> map1 = new HashMap<String, String>();
		HashMap<String, String> map2 = new HashMap<String, String>();
		map1.put("ItemTitle", this.getString(R.string.search_more) + " "
				+ this.mMp3Singer);
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
					intent1.putExtra(Const.Key, mMp3Singer);
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
			if(mMp3Location.startsWith("http:")) {
				if (mStreaming == null) {
					mStreaming  = new ProgressDialog(MusicPage.this);
					mStreaming.setTitle(R.string.mStreaming_title);
					mStreaming.setMessage(MusicPage.this.getString(R.string.mStreaming_message));
					mStreaming.setIndeterminate(true);
					mStreaming.setCancelable(true);
					mStreaming.setButton(MusicPage.this.getString(R.string.stop), new DialogInterface.OnClickListener() {			
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mPlayer.stop();
						}
					});
				}
				mStreaming.show();

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							mPlayer.reset();
							mPlayer.setDataSource(mMp3Location);
							mPlayer.prepare();
							mPlayer.start();
							mPlayer.setOnCompletionListener(new OnCompletionListener () {
								@Override
								public void onCompletion(MediaPlayer mp) {
									mStreaming.dismiss();
								}
							});
						} catch (IllegalArgumentException e) {
						} catch (IllegalStateException e) {
						} catch (IOException e) {
						}       

					}

				}).start();

			}		
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
			mProgressDownload.setMessage("Downloading mp3 file ...");
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
			btnQueue.setVisibility(View.GONE);
			removeDialog(CONNECTING);
			mProgressDialogIsOpen = false;
			if (mDownloading == false) {
				try {
					try {
						if (mPlayer.isPlaying()) {
							mPlayer.pause();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					showDialog(DOWNLOAD_MP3FILE);
					mDownloadFile = mMp3Title + "[" + mMp3Singer + "]"
					+ ".mp3";
					new DownloadTask(false).execute(mMp3Location);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				mProgressDownload.show();
			}
		}
	};

	OnClickListener queueClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			try {
				mDownloadFile = mMp3Title + "[" + mMp3Singer + "]" + ".mp3";
				new DownloadTask(true).execute(mMp3Location);
				Toast.makeText(MusicPage.this, R.string.queue_message, Toast.LENGTH_SHORT).show();
				MusicPage.this.finish();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	};
	
	OnClickListener playClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mPlayer != null) {
				btnStop.setVisibility(View.VISIBLE);
				btnPlay.setVisibility(View.GONE);
				new Thread(new Runnable() {
					public void run() {
						try {
							Log.e("MusicPage", "into new thread");
							mPlayer.stop();
							Log.e("MusicPage", "media service stopped");

							String mp3Path = mFilesManager.getHomeDir() + mDownloadFile;

							Debug.D("mp3Path = " + mp3Path);

							mPlayer.reset();
							mPlayer.setDataSource(mp3Path);
							Log.e("MusicPage", "media file opened");

							mPlayer.prepare();
							mPlayer.start();

							// Add notification.
							Intent intent = new Intent(MusicPage.this, MusicPage.class);
							Utils.addNotification(MusicPage.this, intent, mDownloadFile, getString(R.string.app_name),
									"", getString(R.string.app_name), "");
						    intent.putExtra(Const.MP3LOC, mMp3Location);
						    intent.putExtra(Const.MP3TITLE, mMp3Title);
						    intent.putExtra(Const.MP3SINGER, mMp3Singer);
						    intent.putExtra(Const.MP3DOWNLOADFILE, mDownloadFile);
						    
							mProgressDialogIsOpen = false;

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
			}
		}
	} ;
	
	OnClickListener stopClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mPlayer != null && mPlayer.isPlaying()) {
				mPlayer.stop();
				btnPlay.setVisibility(View.VISIBLE);
				btnStop.setVisibility(View.GONE);
			}
		}
	};


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
					String fullpathname = mFilesManager.getHomeDir() + mDownloadFile;
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
					String fullpathname = mFilesManager.getHomeDir() + mDownloadFile;
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
						mDownloadFile + getString(R.string.download_finished), Toast.LENGTH_LONG).show();
				// DownloadShowMessage(mDownloadFile + " download finished");
				// updateDownloadList();
				String fullpathname = mFilesManager.getHomeDir() + "/" + mDownloadFile;
				ScanMediafile(fullpathname);
				// showDownloadOKNotification(mDownloadFile);
				if (isQueue) {
					Debug.D("Sending notification");
					Intent intent = new Intent(MusicPage.this, MusicSearch.class);
					Utils.addNotification(
							getApplication(), intent, mMp3Title,
							getString(R.string.app_name),
							getString(R.string.saved),
							getString(R.string.app_name), 
							getString(R.string.saved));
				} else {
					btnDownload.setVisibility(View.GONE);
					btnPlay.setVisibility(View.VISIBLE);
					btnStop.setVisibility(View.GONE);
				}
				saveArtistAndTitle();
			}
			if (result == 0) {
				Toast.makeText(MusicPage.this, mMp3Title + getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
			}
			if (!isQueue) {
				removeDialog(DOWNLOAD_MP3FILE);
				mDownloading = false;
				mProgressDialogIsOpen = false;
			}
		}
	}

	private void saveArtistAndTitle() {
		saveData(mMp3Singer, Const.MP3SINGER);
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
