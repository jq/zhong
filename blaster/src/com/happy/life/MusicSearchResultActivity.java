package com.happy.life;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.downloader.AlreadyDownloadingException;
import com.limegroup.gnutella.downloader.FileExistsException;
import com.limegroup.gnutella.settings.SharingSettings;
import com.util.Constants;
import com.util.DownloadInfo;
import com.util.ListStatusView;
import com.util.P2pSearchResult;
import com.util.SearchAdapter;
import com.util.SearchResult;
import com.util.SearchResultActivity;

public class MusicSearchResultActivity extends SearchResultActivity {
  
  private static final String TAG = Utils.TAG;
  private static final int DIALOG_MUSIC_STREAMING = 3;
  
  private static final int MUSIC_OPTION_DOWNLOAD = 0;
  private static final int MUSIC_OPTION_PREVIEW = 1;
    
  private static volatile SogouMusicSearcher sSogouMusicSearcher = new SogouMusicSearcher();
  
  private static FetchMp3ListTask sFetchMp3ListTask;
  
  private static volatile MediaPlayer sPlayer;
  
  protected MusicSearchBar mMusicSearch;
  
  MusicMp3ListAdapter mMusicAdapter;
  
  private ProgressDialog mStreaming;
  private static volatile Thread sPreviewThread;
  
  protected static volatile MusicSearchResultActivity sMusicSearchResultActivity;
  
  protected SearchAdapter getSearchAdapter() {
    return new MusicSearchAdapter(mGuid);
  }
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    sMusicSearchResultActivity = this;
    super.sSearchResultActivity = sMusicSearchResultActivity;
    super.onCreate(savedInstanceState);
    App.init(getApplication());
    
    mMusicSearch = new MusicSearchBar(this);
    super.mSearch = mMusicSearch;
    
    mMusicAdapter = new MusicMp3ListAdapter(
        MusicSearchResultActivity.this,
        R.layout.result_item);
    super.mAdapter = mMusicAdapter;
    super.mListView.setAdapter(mMusicAdapter);
    
  }

  
  private void download(SearchResult mp3) {
    mDownloadService.insertDownload(mp3.createDownloadInfo());
    Intent intent = new Intent(MusicSearchResultActivity.this, MusicDownloadActivity.class);
    startActivity(intent);
  }

  private void preview(final SearchResult mp3) {
    if (mDownloadService.fileBeingDownloaded(mp3)) {
      runOnUiThread(new Runnable() {
          @Override
          public void run() {
              if (mStreaming != null) {
                  mStreaming.dismiss();
              }
              Utils.Error(MusicSearchResultActivity.this, "You can't preivew the music while it is being downloaded");
          }
      });
      return;
    }

    
    if (sPreviewThread != null) {
        sPreviewThread.interrupt();
        sPreviewThread = null;
    }
    
    if (mp3 instanceof P2pSearchResult) {
        sPreviewThread = new Thread(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                boolean previewError = false;
                Downloader downloader = null;
                MediaPlayer player = null;

                try {
                    RemoteFileDesc rfd = ((P2pSearchResult)mp3).getFirstRFD();
                    if (rfd == null) {
                        previewError = true;
                        Log.e(TAG, "No rfd for preview.");
                        return;
                    }

                    RemoteFileDesc rfds[] = new RemoteFileDesc[] { rfd };

                    player = sPlayer;

                    if (player != null) {
                        player.release();
                    }

                    downloader = RouterService.download(rfds,
                            Collections.EMPTY_SET,
                            true,
                            new GUID(((P2pSearchResult)mp3).getGuid()));

                    if (downloader == null) {
                        previewError = true;
                        Log.e(TAG, "No downloader for preview.");
                        return;
                    }

                    String targetFile = downloader.getFile().getName();

                    int state = downloader.getState();
                    // Wait until we start to download.
                    while (state != Downloader.DOWNLOADING) {
                        // But if we can't continue, break the loop.
                        if (state == Downloader.ABORTED ||
                            state == Downloader.GAVE_UP ||
                            state == Downloader.DISK_PROBLEM ||
                            state == Downloader.CORRUPT_FILE ||
                            state == Downloader.WAITING_FOR_USER ||
                            state == Downloader.RECOVERY_FAILED ||
                            state == Downloader.PAUSED) {
                            previewError = true;
                            Log.e(TAG, "Invalid preview state: " + state);
                            break;
                        }
                        Thread.sleep(1000);
                        state = downloader.getState();
                    }

                    while (!previewError) {
                        String readyUrl = "http://localhost:" + Constants.MINI_SERVER_PORT +
                            "/cmd=ready&file=" + URLEncoder.encode(targetFile);

                        String ready = getURL(readyUrl).trim();

                        state = downloader.getState();

                        com.util.Utils.D("isReady: " + ready);
                        com.util.Utils.D("state: " + state);
                        
                            if (ready.equals("true")) {
                                break;
                            }
                        Thread.sleep(1000);
                    }

                    if (!previewError) {
                        if (TextUtils.isEmpty(mp3.getFileName())) {
                            previewError = true;
                            Log.e(TAG, "Empty preview filename.");
                            return;
                        }

                        String previewUrl = "http://localhost:" + Constants.MINI_SERVER_PORT +
                            "/file=" + URLEncoder.encode(downloader.getFile().getName()) +
                            "&dwfile=" + URLEncoder.encode(mp3.getFileName());

                        player = new MediaPlayer();
                        player.reset();

                        com.util.Utils.D("preview url: " + previewUrl);

                        player.setDataSource(previewUrl);
                        com.util.Utils.D("+++ player data source set");

                        player.prepare();
                        com.util.Utils.D("+++ player prepared");

                        player.start();
                        com.util.Utils.D("+++ player started");

                        player.setOnCompletionListener(new OnCompletionListener () {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                sPlayer = null;
                                com.util.Utils.D("+++ player completed");
                                if (sPreviewThread != null)
                            sPreviewThread.interrupt();
                        sPreviewThread = null;

                        if (sSearchResultActivity != null) {
                            sSearchResultActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    MusicSearchResultActivity.this.dismissStreamingDialog();
                                }
                            });
                        }
                            }
                        });
                        sPlayer = player;

                        state = downloader.getState();
                        while ((DownloadInfo.isDownloading(state) &&
                                    state != Downloader.WAITING_FOR_USER) ||
                                state == Downloader.COMPLETE) {
                            if (state == Downloader.ABORTED ||
                                state == Downloader.GAVE_UP ||
                                state == Downloader.DISK_PROBLEM ||
                                state == Downloader.CORRUPT_FILE ||
                                state == Downloader.WAITING_FOR_USER ||
                                state == Downloader.RECOVERY_FAILED ||
                                state == Downloader.PAUSED) {
                                previewError = true;
                                Log.e(TAG, "Invalid preview state: " + state);
                                break;
                            }
                            Thread.sleep(1000);
                            state = downloader.getState();
                            com.util.Utils.D("preview state: " + state);
                        }
                        com.util.Utils.D("+++ Exit state: " + state);
                    }
                } catch (FileExistsException e) {
                    previewError = true;
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    previewError = true;
                    e.printStackTrace();
                } catch (AlreadyDownloadingException e) {
                    previewError = true;
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    previewError = true;
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    previewError = true;
                    e.printStackTrace();
                } catch (IOException e) {
                    previewError = true;
                    e.printStackTrace();
                } finally {
                    sPlayer = null;
                    if (player != null)
                        player.release();

                    // Delete the target file.
                    if (downloader != null) {
                        downloader.stop();

                        com.util.Utils.D("+++ Downloader stopped");

                        if (downloader.getFile() == null)
                            return;

                        String targetFile = downloader.getFile().getName();
                        if (targetFile == null)
                            return;

                        File file = new File(SharingSettings.INCOMPLETE_DIRECTORY, targetFile);
                        if (file.exists()) {
                            file.delete();
                        }
                        downloader = null;
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mStreaming != null) {
                                mStreaming.dismiss();
                            }
                        }
                    });

                    if (previewError) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplication(),
                                    "Something was wrong", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    sPreviewThread = null;
                }
            }
        });
        sPreviewThread.start();
    } else if (mp3 instanceof SogouSearchResult) {
      final SogouSearchResult sogouMp3 = (SogouSearchResult)mp3;
      if (sogouMp3.getDownloadUrl() != null && sogouMp3.getDownloadUrl().startsWith("http:")) {
          sPreviewThread = new Thread(new Runnable() {
              @Override
              public void run() {
                  try {
                      MediaPlayer player = sPlayer;
                      if (player != null) {
                          player.release();
                      }

                      sPlayer = new MediaPlayer();
                      player = sPlayer;
                      player.reset();
                      player.setDataSource(sogouMp3.getDownloadUrl());
                      player.prepare();

                      player.start();
                      player.setOnCompletionListener(new OnCompletionListener () {
                          @Override
                          public void onCompletion(MediaPlayer mp) {
                              sPlayer = null;
                              runOnUiThread(new Runnable() {
                                  @Override
                                  public void run() {
                                      if (mStreaming != null) {
                                          mStreaming.dismiss();
                                      }
                                  }
                              });
                          }
                      });
                      player.setOnErrorListener(new OnErrorListener() {
                          @Override
                          public boolean onError(MediaPlayer mp, int what, int extra) {
                              onPlayError();
                              return true;
                          }

                      });

                      if (sPlayer == null) {
                          // Someone requested us to stop.
                          player.release();
                      }
                  } catch (IllegalArgumentException e) {
                      onPlayError();
                      e.printStackTrace();
                  } catch (IllegalStateException e) {
                      //onPlayError();
                      e.printStackTrace();
                  } catch (IOException e) {
                      onPlayError();
                      e.printStackTrace();
                  } finally {
                      sPreviewThread = null;
                  }
              }
          });
          sPreviewThread.start();
      }
    }
}

  private void onPlayError() {
    MediaPlayer player = sPlayer;
    sPlayer = null;
    if (player != null)
        player.release();
    
    runOnUiThread(new Runnable() {
        @Override
        public void run() {
            if (mStreaming != null) {
                mStreaming.dismiss();
            }
            Toast.makeText(getApplication(), "Streaming error", Toast.LENGTH_LONG).show();
        }
    });
  }
  
  @Override
  protected Dialog onCreateDialog(int id) {
      com.util.Utils.D("onCreateDialog() " + id);
      switch (id) {
      case DIALOG_WAITING_FOR_SERVER: {
          if (mProgressDialog == null) {
              mProgressDialog = new ProgressDialog(this);
              mProgressDialog.setMessage(getString(R.string.wait));
              mProgressDialog.setIndeterminate(true);
              mProgressDialog.setCancelable(true);
          }
          return mProgressDialog;
      }
      
      case DIALOG_MUSIC_OPTIONS:
          return new AlertDialog.Builder(MusicSearchResultActivity.this)
              .setItems(R.array.music_item_options, new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) {
                      switch(which) {
                      case MUSIC_OPTION_DOWNLOAD:
                          if (mCurrentResult == null)
                              return;
                          download(mCurrentResult);
                          break;
                       
                      case MUSIC_OPTION_PREVIEW:
                          if (mCurrentResult == null)
                              return;
                          if (mCurrentResult instanceof SogouSearchResult) {
                              SogouSearchResult sogouResult = (SogouSearchResult)mCurrentResult;
                              mHandler.post(new Runnable() {
                                  @Override
                                  public void run() {
                                      showDialog(DIALOG_MUSIC_STREAMING);
                                  }
                              });

                              if (sogouResult.getDownloadUrl() == null) {
                                  new FetchMp3LinkTaskForPreview().execute(sogouResult);
                                  break;
                              }
                          } else {
                              mHandler.post(new Runnable() {
                                  @Override
                                  public void run() {
                                      showDialog(DIALOG_MUSIC_STREAMING);
                                  }
                              });
                              preview(mCurrentResult);
                              break;
                          }
                      }
                  }
              })
              .create();
          
      case DIALOG_MUSIC_STREAMING:
          if (mStreaming == null) {
              mStreaming = new ProgressDialog(MusicSearchResultActivity.this);
              mStreaming.setTitle("Streaming music...");
              mStreaming.setMessage(getString(R.string.wait));
              mStreaming.setIndeterminate(true);
              mStreaming.setCancelable(false);
              mStreaming.setButton(getString(R.string.stop), new DialogInterface.OnClickListener() {          
                          @Override
                  public void onClick(DialogInterface dialog, int which) {
                              if (mStreaming != null) {
                                  mStreaming.dismiss();
                                  mStreaming = null;
                              }
                              
                              MediaPlayer player = sPlayer;
                              sPlayer = null;
                              if (player != null) {
                                  player.release();
                              }
                              
                              if (sPreviewThread != null) {
                                  sPreviewThread.interrupt();
                                  sPreviewThread = null;
                              }
                          }
                      });
          }
          return mStreaming;
      }
      return null;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
     switch (item.getItemId()) {
      case R.id.dowloads:
          Intent intent = new Intent(MusicSearchResultActivity.this, MusicDownloadActivity.class);
          startActivity(intent);
          return true;
      }
      return false;
  }   
  
  @Override
  protected void startQuery(Intent intent) {
    super.startQuery(intent);
    if (!TextUtils.isEmpty(mQuery)) {
        sSogouMusicSearcher.setQuery(mQuery);
        sFetchMp3ListTask = new FetchMp3ListTask(getApplication());
        sFetchMp3ListTask.execute();
    }
  }
  
  public void dismissStreamingDialog() {
    if (mStreaming != null && mStreaming.isShowing()) {
        mStreaming.dismiss();
    }
  }
  
  private void handleSogouSearchResult(ArrayList<SogouSearchResult> mp3List) {    
    ArrayList<SogouSearchResult> newList = Utils.dedup(mp3List);
    if (newList != null && mSearchAdapter != null) {
      for (Iterator<SogouSearchResult> it = newList.iterator(); it.hasNext();) {
        SogouSearchResult result = it.next();
        boolean shouldRefresh = ((MusicSearchAdapter)mSearchAdapter).add(result);
        int oldDisplaySize = mSearchAdapter.displaySize();
        if (shouldRefresh) {
          if (mSearchAdapter.isBatchFull()) {
              mMusicAdapter.setStatus(ListStatusView.Status.LOAD_MORE);
          } else {
              mMusicAdapter.setStatus(ListStatusView.Status.SEARCHING);
          }
          mMusicAdapter.notifyDataSetChanged();
        } else {
          if (mSearchAdapter.displaySize() != oldDisplaySize) {
              throw new IllegalStateException("display size does not match: " + mSearchAdapter.displaySize() + " vs " + oldDisplaySize);
          }
        }
      }
    }
  }
  
  protected class MusicMp3ListAdapter extends Mp3ListAdapter {

    OnClickListener mLoadMoreListener2 = new OnClickListener() {

      @Override
          public void onClick(View v) {
              if (mStatus == ListStatusView.Status.LOAD_MORE) {
                  mSearchAdapter.nextBatch();
                  if (mSearchAdapter.isBatchFull()) {
                      // If the next page has enough results (full), we
                      // are ok, waiting for user to load more.
                      mAdapter.setStatus(ListStatusView.Status.LOAD_MORE);
                  } else {
                      // Otherwise we don't have enough result.
                      mAdapter.setStatus(ListStatusView.Status.SEARCHING);
                      sFetchMp3ListTask = new FetchMp3ListTask(getApplication());
                      sFetchMp3ListTask.execute();
                  }
                  notifyDataSetChanged();
              }
          }
      
    };
    
    public MusicMp3ListAdapter(Context context, int resource) {
      super(context, resource);
      super.mLoadMoreListener = mLoadMoreListener2;
    }
    
  }

  private class FetchMp3LinkTaskForPreview extends AsyncTask<SogouSearchResult, Void, SogouSearchResult> {
    protected SogouSearchResult doInBackground(SogouSearchResult... mp3s) {
      SogouSearchResult mp3 = mp3s[0];
      SogouMusicSearcher.setMusicDownloadUrl(getApplication(), mp3);
      return mp3;
    }

    protected void onPostExecute(SogouSearchResult mp3) {
        if (mp3.getDownloadUrl() == null) {
            if (mStreaming != null) {
                mStreaming.dismiss();
            }
            Toast.makeText(MusicSearchResultActivity.this, R.string.no_download_link, Toast.LENGTH_SHORT).show();
            return;
        }

        preview(mp3);
    }
  }
  
  private static class FetchMp3ListTask extends AsyncTask<Void, Void, ArrayList<SogouSearchResult>> {
    Context mContext;
    public FetchMp3ListTask(Context context) {
        super();
        mContext = context;
    }

    @Override
    protected void onPostExecute(ArrayList<SogouSearchResult> mp3List) {
        if (sFetchMp3ListTask != this) {
            // Another query is going on.
            return;
        }
        sFetchMp3ListTask = null;
        if (sMusicSearchResultActivity != null) {
          sMusicSearchResultActivity.handleSogouSearchResult(mp3List);
        }
    }

    @Override
    protected ArrayList<SogouSearchResult> doInBackground(Void... params) {
        return sSogouMusicSearcher.getNextResultList(mContext);
    }
  }
  
  public static void handleMp3ListIntent(Context context, String key, String keyWords) {
    Intent intent = new Intent(context, MusicSearchResultActivity.class);
    intent.putExtra(key, keyWords);
    context.startActivity(intent);
  }    
  public static void handleMp3ListSimpleIntent(Context context, String keyWords) {
    Intent intent = new Intent(context, MusicSearchResultActivity.class);
    intent.putExtra(Constants.QUERY, keyWords);
    context.startActivity(intent);
  }
  public static void handleMp3ListXMLIntent(Context context, String keyWords, String xml) {
    Intent intent = new Intent(context, MusicSearchResultActivity.class);
    intent.putExtra(Constants.QUERY, keyWords);
    intent.putExtra(Constants.XML, xml);
    context.startActivity(intent);
  }
}
