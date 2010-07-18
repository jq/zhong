package com.trans.music.search;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;
import com.ringdroid.RingdroidSelectActivity;
import com.trans.music.search.IMediaPlaybackService;
import com.trans.music.search.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.*;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.AsyncTask;
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

public class MusicPage extends Activity implements
    SeekBar.OnSeekBarChangeListener {
  boolean mTrackAdapterCreated = false;
  SeekBar mSeekBar;
  static final int CONNECTING = 2;
  static final int DOWNLOAD_MP3FILE = 3;
  static final int CONNECT_ERROR = 7;
  boolean mProgressDialogIsOpen = false;
  int mSongProgress;
  long mSongPosition;
  long mSongDuration;
  ArrayList<String> mMp3Local;
  int locPointer = 0;
  boolean mDownloadFinish = false;
  boolean mIsPlaying = false;
  String mMp3Songer;
  String mMp3Title;
  //float mRate;
  String mAlbm;
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
  private MediaPlayer mPlayer;

  private void getMediaInfo(Intent intent) {
    mMp3Local = intent.getStringArrayListExtra(Const.MP3LOC);
    mMp3Title = intent.getStringExtra(Const.MP3TITLE);
    mMp3Songer = intent.getStringExtra(Const.MP3SONGER);
    mAlbm = intent.getStringExtra(Const.MP3ALBM);

    //mRate = Float.parseFloat(intent.getStringExtra(Const.MP3RATE));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // TODO Auto-generated method stub
    super.onCreate(savedInstanceState);
    this.getMediaInfo(this.getIntent());
    setContentView(R.layout.music_display);
	AdListener.createAds(this);
    
    btnPreview = (Button) findViewById(R.id.preview);
    btnPreview.setOnClickListener(previewClick);
    
    btnDownload = (Button) findViewById(R.id.download);
    btnDownload.setOnClickListener(downloadClick);
    
    btnQueue = (Button) findViewById(R.id.queue);
    btnQueue.setOnClickListener(queueClick);
    
/*    Intent serviceIntent = new Intent(this, MediaPlaybackService.class);
    startService(serviceIntent);
        bindService((new Intent()).setClass(this,
                MediaPlaybackService.class), osc, 0);*/

    
    mSeekBar = (SeekBar) findViewById(R.id.play_seek_bar);
    mSeekBar.setOnSeekBarChangeListener(this);
    mSeekBar.setMax(1000);
    SeekBarSetSecondaryProgress(1000);
    mSeekBar.setEnabled(false);

    mPlayStop = (ImageView) findViewById(R.id.play_stop);
    mPlayStop.setOnClickListener(mPlayStopListener);
    mPlayStop.setEnabled(false);

    ((TextView) findViewById(R.id.row_title)).setText(this
        .getString(R.string.title)
        + mMp3Title);
    ((TextView) findViewById(R.id.row_artist)).setText(this
        .getString(R.string.artist)
        + mMp3Songer);
    RatingBar rb = ((RatingBar) findViewById(R.id.row_small_ratingbar));
    rb.setIsIndicator(true);
    //rb.setRating(mRate);
    
    listSearchOthers = (ListView) findViewById(R.id.list_searchOthers);
    ArrayList<HashMap<String, String>> ringlist = new ArrayList<HashMap<String, String>>();
    HashMap<String, String> map1 = new HashMap<String, String>();
    HashMap<String, String> map2 = new HashMap<String, String>();
    HashMap<String, String> map3 = new HashMap<String, String>();
    map1.put("ItemTitle", this.getString(R.string.search_more) + " "
        + this.mMp3Songer);
    map2.put("ItemTitle", this.getString(R.string.search_more) + " "
        + this.mMp3Title);
    map3.put("ItemTitle", this.getString(R.string.search_more) + " "
        + this.mAlbm);

    ringlist.add(map1);
    ringlist.add(map2);
    ringlist.add(map3);

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
          intent1.setClass(MusicPage.this, SearchList.class);
          startActivityForResult(intent1, 1);
          return;
        case 1:
          Intent intent2 = new Intent();
          intent2.putExtra(Const.Key, mMp3Title);
          intent2.setClass(MusicPage.this, SearchList.class);
          startActivityForResult(intent2, 1);
          return;
        }
      }
    });
    mPlayer = new MediaPlayer();
    mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			Toast.makeText(MusicPage.this, R.string.play_error, Toast.LENGTH_SHORT).show();
			return false;
		}
	});
    mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
		@Override
		public void onCompletion(MediaPlayer mp) {
			try {
				mPlayer.stop();
				mPlayer.prepare();
			} catch (IllegalStateException e) {
			} catch (IOException e) {
			}
			SeekBarInit();
		}
	});
  }
  
  @Override
  protected void onDestroy() {
  	super.onDestroy();
/*    try {
      if(mService.isPlaying() == true){
        mService.stop();
      }
    } catch (Exception ex) {
          ;
    }
        
    unbindService(osc);*/
  	try {
  	  mPlayer.stop();
  	  mPlayer.release();
  	} catch (Exception e) {
	}
  }
  //private IMediaPlaybackService mService = null;
 /* private ServiceConnection osc = new ServiceConnection() {
    public void onServiceConnected(ComponentName classname, IBinder obj) {
      Log.e("music page service", "connected");

        mService = IMediaPlaybackService.Stub.asInterface(obj);
    }*/

    /*public void onServiceDisconnected(ComponentName classname) {
	    try {
	     mService.stop();
	    } catch (Exception ex) {
	    }

    }
};*/

  private OnClickListener mPlayStopListener = new OnClickListener() {
    public void onClick(View v) {
      try {
        if (mPaused == false) {
          //mService.pause();
          mPlayer.pause();
          mPlayStop.setImageResource(R.drawable.play);
          mPaused = true;
        } else {
          //mService.play();
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
      //long pos = mService.position();
      int pos = mPlayer.getCurrentPosition();
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
               //mService.stop();
               mPlayer.reset();
               Log.e("MusicPage","media service stopped");
               //mService.openfile(mMp3Local);
               mPlayer.setDataSource(mMp3Local.get(locPointer));
               mPlayer.prepare();
               mPlayer.start();
               Log.e("MusicPage", "media file opened");
               mHandler.sendEmptyMessage(RM_CON_DIALOG);       
               mProgressDialogIsOpen = false;
               SeekBarSetEnalbe(true);
               ButtonsSetEnalbe(true);
               mSongDuration = mPlayer.getDuration();

               long next = refreshSeekBarNow();
               queueNextRefresh(next);
             } catch (Exception e) {
               // TODO Auto-generated catch block
               showConnectDiaglog(false);
               showConnectErrorDiaglog(true);
               if(locPointer < mMp3Local.size()-1) {
                 locPointer++;
               }
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
      // mMp3Local.substring(0, 64));
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
			btnQueue.setEnabled(false);
			removeDialog(CONNECTING);
			mProgressDialogIsOpen = false;
			if (!mDownloadFinish) {
				// if(mDlService.mDownloading == false){
				Log.e("mDownloading: ", ""+mDownloading);
				Log.e("misPlaying: ", ""+mIsPlaying);
				if (mDownloading == false) {
					try {
						try {
							if (mPlayer.isPlaying() == true) {
								mPlayer.pause();
								// mPlayStop.setImageResource(R.drawable.play);
								mPaused = true;
							}
						} catch (Exception ex) {
							;
						}
						showDialog(DOWNLOAD_MP3FILE);
						m_CurDownloadFile = mMp3Title + "[" + mMp3Songer + "]"
								+ ".mp3";
						Log.e("download", mMp3Local.get(locPointer));
						new DownloadTask(false).execute(mMp3Local.get(locPointer));
						/*
						 * (new Thread() { public void run() { m_CurDownloadFile
						 * = mMp3title + "[" + mMp3songer + "]" + ".mp3"; new
						 * DownloadTask().execute(mMp3Local);
						 * //DownloadMusic(m_CurDownloadFile, mMp3Local); }
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
								//mService.openfile(mMp3Local);
								Log.e("play dataSource: ", mMp3Local.get(locPointer));
								mPlayer.reset();
								mPlayer.setDataSource(mMp3Local.get(locPointer));
								mPlayer.prepare();
								Log.e("MusicPage", "media file opened");
								//mHandler.sendEmptyMessage(RM_CON_DIALOG);
								//mService.play();
								mPlayer.start();
								mProgressDialogIsOpen = false;
								SeekBarSetEnalbe(true);
								ButtonsSetEnalbe(true);
								//mSongDuration = mService.duration();
								mSongDuration = mPlayer.getDuration();
								long next = refreshSeekBarNow();
								queueNextRefresh(next);
							} catch (Exception e) {
								// TODO Auto-generated catch block
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
        new DownloadTask(true).execute(mMp3Local.get(locPointer));
        Toast.makeText(MusicPage.this, R.string.queue_message, Toast.LENGTH_SHORT).show();
        MusicPage.this.finish();
	  } catch(Exception e) {
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

  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
    try {
      //mSongPosition = mService.position();
      mSongPosition = mPlayer.getCurrentPosition();
      long position = (mSongDuration * mSongProgress) / 1000;
      //mService.seek(position);
      mPlayer.seekTo((int)position);
    } catch (Exception ex) {
      ;
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

  private class DownloadTask extends AsyncTask<String, Integer, Integer> {
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
        String fullpathname = Const.homedir + m_CurDownloadFile;
        try {
          url = new URL(urlString);
          urlConn = (HttpURLConnection) url.openConnection();
          urlConn.setConnectTimeout(10000);
          urlConn
              .setRequestProperty(
                  "User-Agent",
                  "Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3 -Java");

          urlConn.connect();

          int downsize = urlConn.getContentLength();
          int downed = 0;

          DownloadSetMax(downsize);

          DataInputStream fileStream;
         
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
          File file = new File(fullpathname);
          file.delete();
          return 0;
        }
      } else {
    	String fullpathname = Const.homedir + "/" + m_CurDownloadFile;
    	try {
          url = new URL(urlString);
          urlConn = (HttpURLConnection) url.openConnection();
          urlConn
              .setRequestProperty( 
                  "User-Agent",
                  "Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3 -Java");
          urlConn.setConnectTimeout(10000);
          urlConn.connect();

          int downed = 0;

          DataInputStream fileStream;
          
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
          File file = new File(fullpathname);
          file.delete();
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
    	String fullpathname = Const.homedir + m_CurDownloadFile;
      if (result == 1) {
        Toast.makeText(MusicPage.this,
            m_CurDownloadFile + getString(R.string.download_finished), Toast.LENGTH_LONG).show();
        // DownloadShowMessage(m_CurDownloadFile + " download finished");
        // updateDownloadList();
        mMp3Local.set(locPointer, fullpathname);
        ScanMediafile(fullpathname);
        // showDownloadOKNotification(m_CurDownloadFile);
        if (isQueue) {
          Intent intent = new Intent(MusicPage.this ,local.class);
          Util.addNotification(MusicPage.this, intent, mMp3Title, R.string.app_name, R.string.save_success_message, R.string.app_name, R.string.save_success_message);
        } else {
          	btnDownload.setText(R.string.play);
        }
        saveArtistAndTitle();
      }
      if (result==0 && !isQueue) {
    	  removeDialog(DOWNLOAD_MP3FILE);
    	  Toast.makeText(MusicPage.this, mMp3Title+getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
    	  mDownloading = false;
          mDownloadFinish = false;
          mProgressDialogIsOpen = false;
          if(locPointer < mMp3Local.size()-1) {
            locPointer++;
          }
      }
      if (!isQueue && result==1) {
    	removeDialog(DOWNLOAD_MP3FILE);
      	mDownloading = false;
      	mDownloadFinish = true;
      	mProgressDialogIsOpen = false;
      }
      if (result == 0) {
    	try {
    	  File brokenFile = new File(fullpathname);
    	  brokenFile.delete();
    	} catch (Exception e) {
		}
      }
    }
  }

  private void saveArtistAndTitle() {
	  saveData(Const.MP3TITLE, this.mMp3Songer);
  }
  
  private void saveData(String key, String item) {
	  if (key.length() > 0) {
		  SharedPreferences s = getSharedPreferences(key, 0);
		  Editor e = s.edit();
		  e.putBoolean(item, true);
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
