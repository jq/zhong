package com.trans.music.search;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;

import com.trans.music.search.MusicSearch.TrackListAdapter.ViewHolder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.*;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MusicPage extends Activity implements
    SeekBar.OnSeekBarChangeListener {
  boolean mTrackAdapterCreated = false;
  SeekBar mSeekBar;
  static final int SEARCHING = 1;
  static final int CONNECTING = 2;
  static final int DOWNLOAD_MP3FILE = 3;
  static final int DOWNLOAD_PRO = 4;
  static final int DOWNLOAD_APP = 5;
  static final int DONATE_APP = 6;
  static final int CONNECT_ERROR = 7;
  boolean mProgressDialogIsOpen = false;
  int mSongProgress;
  long mSongPosition;
  long mSongDuration;
  String mMp3Local;
  String mMp3Songer;
  String mMp3Title;
  String m_CurDownloadFile;
  Button btnPreview;
  Button btnDownload;
  private boolean mPaused = false, mDownloading = false;
  private static final int REFRESH = 1;
  private static final int RM_CON_DIALOG = 2;
  private ImageView mPlayStop;
  private ListView listSearchOthers;
  private TextView infoText;
  private UserTask<?, ?, ?> mTask;
  ProgressDialog mProgressDialog, mProgressDialogSearch,
      mProgressDialogPrepare, mProgressDownload;
  private String SavedPath = "/sdcard/MusicSearch";
  MediaScannerConnection mScanner;
  AlertDialog mAPPDownload;

  private void getMediaInfo(Intent intent) {
    mMp3Local = intent.getStringExtra("MP3LOC");
    mMp3Title = intent.getStringExtra("MP3TITLE");
    mMp3Songer = intent.getStringExtra("MP3SONGER");

  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // TODO Auto-generated method stub
    super.onCreate(savedInstanceState);
    this.getMediaInfo(this.getIntent());
    setContentView(R.layout.music_display);

    
    btnPreview = (Button) findViewById(R.id.preview);
    btnPreview.setOnClickListener(previewClick);
    
    btnDownload = (Button) findViewById(R.id.download);
    btnDownload.setOnClickListener(downloadClick);

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

    listSearchOthers = (ListView) findViewById(R.id.list_searchOthers);
    ArrayList<HashMap<String, String>> ringlist = new ArrayList<HashMap<String, String>>();
    HashMap<String, String> map1 = new HashMap<String, String>();
    HashMap<String, String> map2 = new HashMap<String, String>();
    // HashMap<String, String> map3 = new HashMap<String, String>();
    // HashMap<String, String> map4 = new HashMap<String, String>();
    map1.put("ItemTitle", this.getString(R.string.search_more) + " "
        + this.mMp3Songer);
    // map2.put("ItemTitle", this.getString(R.string.search_more_by) + " " +
    // author);
    // map3.put("ItemTitle", this.getString(R.string.search_more_in) + " " +
    // category);
    map2.put("ItemTitle", this.getString(R.string.search_more) + " "
        + this.mMp3Title);

    ringlist.add(map1);
    ringlist.add(map2);
    // ringlist.add(map3);
    // ringlist.add(map4);
    SimpleAdapter mSearchOthers = new SimpleAdapter(this, ringlist,
        R.layout.ring_list_item, new String[] { "ItemTitle" },
        new int[] { R.id.ringListItem1 });
    listSearchOthers.setAdapter(mSearchOthers);
    listSearchOthers.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position,
          long id) {
        // TODO Auto-generated method stub
        switch (position) {
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
          /*
           * case 2: Search.getCate(category); return; case 3:
           * Search.getTitleRing(title); return;
           */
        }
      }

    });
  }

  private OnClickListener mPlayStopListener = new OnClickListener() {
    public void onClick(View v) {
      try {
        if (mPaused == false) {
          MusicSearch.mService.pause();
          mPlayStop.setImageResource(R.drawable.play);
          mPaused = true;
        } else {
          MusicSearch.mService.play();
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
      long pos = MusicSearch.mService.position();
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
        /*
         * mButtonPlayStop.setEnabled(enable);
         * mButtonAddOnline.setEnabled(enable); if(enable == true){
         * if(mHasSDCard == true) mButtonAddLocal.setEnabled(true); }else
         * mButtonAddLocal.setEnabled(enable);
         */
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
               MusicSearch.mService.stop();
               Log.e("MusicPage","media service stopped");
               MusicSearch.mService.openfile(mMp3Local);
               Log.e("MusicPage", "media file opened");
               mHandler.sendEmptyMessage(RM_CON_DIALOG);
               MusicSearch.mService.play();
               
               mProgressDialogIsOpen = false;
               SeekBarSetEnalbe(true);
               ButtonsSetEnalbe(true);
               mSongDuration = MusicSearch.mService.duration();

               long next = refreshSeekBarNow();
               queueNextRefresh(next);
             } catch (RemoteException e) {
               // TODO Auto-generated catch block
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
    case SEARCHING: {
      mProgressDialogSearch = new ProgressDialog(MusicPage.this);
      mProgressDialogSearch.setMessage("Please wait while searching...");
      mProgressDialogSearch.setIndeterminate(true);
      mProgressDialogSearch.setCancelable(true);
      mProgressDialogIsOpen = true;
      return mProgressDialogSearch;
    }
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

    case DOWNLOAD_PRO: {
      String ver = android.os.Build.VERSION.SDK;
      if (ver.compareTo(new String("3")) == 0) {
        mAPPDownload = new AlertDialog.Builder(this)
            .setIcon(R.drawable.icon)
            .setTitle("Download MusicSearchPro")
            .setMessage(
                "If you want continue to use this app, you should download MusicSearchPro! Or Ignore it to search music once a day .")
            .setPositiveButton("Download",
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {

                    Intent intent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri
                            .parse("market://search?q=pname:com.trans.music.searchpro"));
                    startActivity(intent);
                  }
                }).setNegativeButton("Ignore",
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {
                    /*
                     * Intent intent = new Intent( Intent.ACTION_VIEW,
                     * Uri.parse(
                     * "market://search?q=pname:com.transcoder.music.searchpro"
                     * )); startActivity(intent);
                     */

                  }
                }).create();
      } else {
        mAPPDownload = new AlertDialog.Builder(MusicPage.this)
            .setIcon(R.drawable.icon)
            .setTitle("Download MusicSearchPro")
            .setMessage(
                "If you want continue to use this app, you should download MusicSearchPro! Or Ignore it to search music once a day .")
            .setPositiveButton("Download",
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {

                    Intent intent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri
                            .parse("market://search?q=pname:com.trans.music.searchpro"));
                    startActivity(intent);
                  }
                }).setNegativeButton("Ignore",
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {
                    /*
                     * Intent intent = new Intent( Intent.ACTION_VIEW,
                     * Uri.parse(
                     * "market://search?q=pname:com.transcoder.music.searchpro"
                     * )); startActivity(intent);
                     */

                  }
                }).create();
      }
      return mAPPDownload;
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
      // download app
    case DOWNLOAD_APP: {/*
                         * try { JSONObject feed =
                         * mFeedentries.getJSONObject(mFeedindex); final String
                         * uriString = feed.getString("uri"); final String
                         * descript = feed.getString("descript"); final String
                         * name = feed.getString("name");
                         * 
                         * mAPPDownload = new AlertDialog.Builder(this)
                         * .setIcon(R.drawable.icon) .setTitle("Download " +
                         * name) .setMessage(descript)
                         * .setPositiveButton("Download", new
                         * DialogInterface.OnClickListener() { public void
                         * onClick(DialogInterface dialog, int whichButton) {
                         * 
                         * Intent intent = new Intent( Intent.ACTION_VIEW,
                         * Uri.parse(uriString)); startActivity(intent); }
                         * }).setNegativeButton("Ignore", new
                         * DialogInterface.OnClickListener() { public void
                         * onClick(DialogInterface dialog, int whichButton) {
                         * mAPPDownload.dismiss(); } }).create();
                         * 
                         * 
                         * mAPPDownload = (AlertDialog)new
                         * AlertDialog.Builder(this);
                         * mAPPDownload.setIcon(R.drawable.icon);
                         * mAPPDownload.setTitle("Download " + name);
                         * mAPPDownload.setMessage(descript);
                         * mAPPDownload.setCancelable(true);
                         * mAPPDownload.setButton("Download", new
                         * DialogInterface.OnClickListener() { public void
                         * onClick(DialogInterface dialog, int whichButton) {
                         * Intent intent = new Intent( Intent.ACTION_VIEW,
                         * Uri.parse(uriString)); startActivity(intent); } });
                         * 
                         * return mAPPDownload; }catch (Exception e){
                         * Log.e("Download app", "error: " + e.getMessage(), e);
                         * }
                         */
    }
      // donate app
    case DONATE_APP: {
      try {

        mAPPDownload = new AlertDialog.Builder(MusicPage.this)
            .setIcon(R.drawable.icon)
            .setTitle("Donate MusicSearch")
            .setMessage(
                "If you want continue to use this app, you should donate it cost $7.99 or buy MusicSearch Pro cost $9.99 to get unlimited using and search more results! Or Ignore it to search music once a day .")
            .setPositiveButton("PayPal", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {

                Intent intent = new Intent(MusicPage.this, paypal.class);
                startActivity(intent);
                /*
                 * String token = PaypalDonate(); //String uriString =
                 * "https://mobile.paypal.com/wc?t=" + token; String uriString =
                 * "https://www.sandbox.paypal.com/wc?t=" + token; Intent intent
                 * = new Intent( Intent.ACTION_VIEW, Uri.parse(uriString));
                 * startActivity(intent);
                 */
              }
            }).setNeutralButton("Buy Pro",
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {
                    Intent intent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri
                            .parse("market://search?q=pname:com.trans.music.searchpro"));
                    startActivity(intent);
                  }
                }).setNegativeButton("Ignore",
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {
                    mAPPDownload.dismiss();
                  }
                }).create();

        return mAPPDownload;
      } catch (Exception e) {
        Log.e("Download app", "error: " + e.getMessage(), e);
      }
    }
    }
    return null;
  }

  OnClickListener downloadClick = new OnClickListener() {

    @Override
    public void onClick(View v) {
      // TODO Auto-generated method stub

      removeDialog(CONNECTING);
      mProgressDialogIsOpen = false;
      // if(mDlService.mDownloading == false){
      if (mDownloading == false) {
        try {
          // if(mSongs.size() > 0){
          // if(mSearchResultMp3index >= 0){
          // MP3Info mp3 = mSongs.get(mSearchResultMp3index);
          // String url = mp3.link;
          // mMp3Local = url.replaceAll("[ ]", "%20");
          // mMp3title = mp3.name;
          // mMp3songer = mp3.artist;
          try {
            if (MusicSearch.mService.isPlaying() == true) {
              MusicSearch.mService.pause();
              // mPlayStop.setImageResource(R.drawable.play);
              mPaused = true;
            }
          } catch (Exception ex) {
            ;
          }
          showDialog(DOWNLOAD_MP3FILE);
          m_CurDownloadFile = mMp3Title + "[" + mMp3Songer + "]" + ".mp3";
          mTask = new DownloadTask().execute(mMp3Local);
          /*
           * (new Thread() { public void run() { m_CurDownloadFile = mMp3title +
           * "[" + mMp3songer + "]" + ".mp3"; new
           * DownloadTask().execute(mMp3Local);
           * //DownloadMusic(m_CurDownloadFile, mMp3Local); } }).start();
           */
          // }else
          // Toast.makeText(MusicPage.this,
          // "Please select link in search results first",
          // Toast.LENGTH_SHORT).show();
          // }else
          // Toast.makeText(MusicPage.this, "Please search music first",
          // Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else {
        mProgressDownload.show();
        // Toast.makeText(MusicSearch.this,
        // "Current download not finish, please try later.",
        // Toast.LENGTH_SHORT).show();
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
      mSongPosition = MusicSearch.mService.position();
      long position = (mSongDuration * mSongProgress) / 1000;
      MusicSearch.mService.seek(position);
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

  private class DownloadTask extends UserTask<String, Integer, Integer> {
    public Integer doInBackground(String... urls) {

      URL url = null;
      HttpURLConnection urlConn = null;

      String urlString;

      urlString = urls[0];
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
        String fullpathname = SavedPath + "/" + m_CurDownloadFile;
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

    }

    public void onProgressUpdate(Integer... progress) {
      mProgressDownload.setProgress(progress[0]);
    }

    public void onPostExecute(Integer result) {

      if (result == 1) {
        Toast.makeText(MusicPage.this,
            m_CurDownloadFile + " download finished", Toast.LENGTH_LONG).show();
        // DownloadShowMessage(m_CurDownloadFile + " download finished");
        // updateDownloadList();
        String fullpathname = SavedPath + "/" + m_CurDownloadFile;
        ScanMediafile(fullpathname);
        // showDownloadOKNotification(m_CurDownloadFile);

      }

      removeDialog(DOWNLOAD_MP3FILE);
      mDownloading = false;
      mProgressDialogIsOpen = false;

    }

  }

  private void ScanMediafile(final String fullpathame) {

    mScanner = new MediaScannerConnection(this,
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
  /*
   * // public class TrackListAdapter extends BaseAdapter {
   * 
   * private ArrayList<String> mMp3Title = new ArrayList<String>(); private
   * LayoutInflater mInflater;
   * 
   * public TrackListAdapter(Context c) { mContext = c; mInflater =
   * LayoutInflater.from(c); }
   * 
   * public int getCount() { //return mPhotos.size(); //return
   * mMp3entries.length(); return mSongs.size(); }
   * 
   * public Object getItem(int position) { return position; }
   * 
   * public long getItemId(int position) { return position; }
   * 
   * public View getView(int position, View convertView, ViewGroup parent) {
   * ViewHolder holder;
   * 
   * if (convertView == null) { convertView =
   * mInflater.inflate(R.layout.track_list_item, null);
   * 
   * // Creates a ViewHolder and store references to the two children views //
   * we want to bind data to. holder = new ViewHolder();
   * 
   * ImageView iv = (ImageView) convertView.findViewById(R.id.icon);
   * iv.setVisibility(View.GONE);
   * 
   * holder.line1 = (TextView) convertView.findViewById(R.id.line1);
   * holder.line2 = (TextView) convertView.findViewById(R.id.line2);
   * holder.duration = (TextView) convertView.findViewById(R.id.duration);
   * holder.play_indicator = (ImageView)
   * convertView.findViewById(R.id.play_indicator);
   * 
   * convertView.setTag(holder); } else { // Get the ViewHolder back to get fast
   * access to the TextView // and the ImageView. holder = (ViewHolder)
   * convertView.getTag(); }
   * 
   * try{ // Bind the data efficiently with the holder. MP3Info mp3 =
   * mSongs.get(position);
   * 
   * if(mp3.bNull){ holder.line1.setText("# Upgrade to \"MusicSearch Pro\" #");
   * holder.line2.setText(" Pro can get more results\n and search faster");
   * holder.duration.setText(""); }else{
   * 
   * String mp3tile = mp3.name; String songer = mp3.artist; String album =
   * mp3.album; String size = mp3.fsize;
   * 
   * holder.duration.setText(size);
   * 
   * String songinfo = new String("");
   * 
   * holder.line1.setText(mp3tile);
   * 
   * if(album.length() > 1) songinfo = songinfo + album; else songinfo =
   * songinfo + new String("Unknown album");
   * 
   * songinfo = songinfo + new String("\n");
   * 
   * if(songer.length() > 1) songinfo = songinfo + songer; else songinfo =
   * songinfo + new String("Unknown artist");
   * 
   * 
   * holder.line2.setText(songinfo);
   * 
   * ImageView iv = holder.play_indicator; try{ Log.e("MusicSearch",
   * "play_indicator: " + mSearchResultMp3index + " - " + position +
   * " playing:  " + mService.isPlaying() ); if((mSearchResultMp3index ==
   * position) && (mService.isPlaying() == true)){
   * iv.setImageResource(R.drawable.indicator_ic_mp_playing_list);
   * iv.setVisibility(View.VISIBLE); Log.e("MusicSearch",
   * "play_indicator: true"); }else{
   * //iv.setImageResource(R.drawable.indicator_ic_mp_playing_list);
   * //iv.setVisibility(View.VISIBLE); iv.setVisibility(View.GONE);
   * Log.e("MusicSearch", "play_indicator: false"); } } catch (Exception ex) { ;
   * } } }catch(Exception e) { e.printStackTrace(); }
   * 
   * return convertView;
   * 
   * 
   * 
   * }
   * 
   * 
   * class ViewHolder { TextView line1; TextView line2; TextView duration;
   * TextView size; ImageView play_indicator;
   * 
   * }
   * 
   * private Context mContext;
   * 
   * public void clear() { mMp3Title.clear(); notifyDataSetChanged(); }
   * 
   * public void add(String name) {
   * 
   * mMp3Title.add(name); notifyDataSetChanged(); }
   * 
   * }
   */
}
