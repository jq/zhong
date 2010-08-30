package com.fatima.life;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import com.fatima.life.R;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.chat.Chatter;
import com.limegroup.gnutella.downloader.AlreadyDownloadingException;
import com.limegroup.gnutella.downloader.FileExistsException;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.security.User;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class SearchResultActivity extends Activity {
	private static final String TAG = Utils.TAG;
	private static final int DIALOG_WAITING_FOR_SERVER = 1; 
    private static final int DIALOG_MUSIC_OPTIONS = 2;
    private static final int DIALOG_MUSIC_STREAMING = 3;
    
    private static final int MUSIC_OPTION_DOWNLOAD = 0;
    private static final int MUSIC_OPTION_PREVIEW = 1;
    
    private static final int MAX_SEARCH_DELAY = 60000;

	private SearchResult mCurrentResult;
	
	private static volatile SogouMusicSearcher sSogouMusicSearcher = new SogouMusicSearcher();
        
	
	private static FetchMp3ListTask sFetchMp3ListTask;
	private static WaitForRouterServiceTask sWaitForRouterServiceTask;
	
	private SearchBar mSearch;
	private String mQuery;
	
	private ProgressDialog mProgressDialog;
	private ProgressDialog mStreaming;

	private DownloadService mDownloadService;
	
	private byte[] mGuid;
	private Handler mHandler = new Handler();
	private ActivityCallback mListener;
	
	private ListView mListView;
	private Mp3ListAdapter mAdapter;
	private volatile SearchAdapter mSearchAdapter;
	
	private static volatile MediaPlayer sPlayer;
	private static volatile Thread sPreviewThread;
	private static volatile SearchResultActivity sSearchResultActivity;
	
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mDownloadService = ((DownloadService.LocalBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mDownloadService = null;
        }
    };
    
    
    private void download(SearchResult mp3) {
      if (mp3 instanceof P2pSearchResult) {
        P2pDownloadInfo download = new P2pDownloadInfo((P2pSearchResult) mp3);
        download.setFileName(mp3.getFileName());
        mDownloadService.insertDownload(download);
      } else if (mp3 instanceof SogouSearchResult) {
        SogouDownloadInfo download = new SogouDownloadInfo((SogouSearchResult) mp3);
        mDownloadService.insertDownload(download);
      }

      Intent intent = new Intent(SearchResultActivity.this, DownloadActivity.class);
      startActivity(intent);
    }
    
	private static String getURL(String link) throws IOException {
		URL url = new URL(link);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		connection.setConnectTimeout(1000);
		connection.connect();
		
		StringBuilder builder = new StringBuilder(1024);

		InputStreamReader is = new InputStreamReader(connection.getInputStream());
		
		BufferedReader reader = new BufferedReader(is, 1024);

		String line = null;
		while ((line = reader.readLine()) != null) {
			builder.append(line + "\n");
		}
		return builder.toString();
	}
    
    private void preview(final SearchResult mp3) {
        if (mp3 instanceof P2pSearchResult) {
          if (mDownloadService.fileBeingDownloaded((P2pSearchResult) mp3)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mStreaming != null) {
                        mStreaming.dismiss();
                    }
		            Utils.Error(SearchResultActivity.this, "You can't preivew the music while it is being downloaded");
                }
            });
            return;
          }
        } else if (mp3 instanceof SogouSearchResult) {
          if (mDownloadService.fileBeingDownloaded((SogouSearchResult) mp3)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mStreaming != null) {
                        mStreaming.dismiss();
                    }
		            Utils.Error(SearchResultActivity.this, "You can't preivew the music while it is being downloaded");
                }
            });
            return;
          }
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

                            Utils.D("isReady: " + ready);
                            Utils.D("state: " + state);
                            
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

                            Utils.D("preview url: " + previewUrl);

                            player.setDataSource(previewUrl);
                            Utils.D("+++ player data source set");

                            player.prepare();
                            Utils.D("+++ player prepared");

                            player.start();
                            Utils.D("+++ player started");

                            player.setOnCompletionListener(new OnCompletionListener () {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    sPlayer = null;
                                    Utils.D("+++ player completed");
                                    if (sPreviewThread != null)
                                sPreviewThread.interrupt();
                            sPreviewThread = null;

                            if (sSearchResultActivity != null) {
                                sSearchResultActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        sSearchResultActivity.dismissStreamingDialog();
                                    }
                                });
                            }
                                }
                            });
                            sPlayer = player;

                            state = downloader.getState();
                            while ((DownloadService.isDownloading(state) &&
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
                                Utils.D("preview state: " + state);
                            }
                            Utils.D("+++ Exit state: " + state);
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

                            Utils.D("+++ Downloader stopped");

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
		Utils.D("onCreateDialog() " + id);
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
            return new AlertDialog.Builder(SearchResultActivity.this)
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
				mStreaming = new ProgressDialog(SearchResultActivity.this);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_result_menu, menu);
        return true;
    }
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       switch (item.getItemId()) {
        case R.id.dowloads:
            Intent intent = new Intent(SearchResultActivity.this, DownloadActivity.class);
            startActivity(intent);
            return true;
        }
        return false;
    }	

	private void startQuery(Intent intent) {
	    mQuery = mSearch.getQuery();
	    
    	mAdapter.setStatus(ListStatusView.Status.SEARCHING);
    	mAdapter.notifyDataSetChanged();
	    
	    if (TextUtils.isEmpty(mQuery)) {
        	mQuery = StringUtils.removeIllegalChars(intent.getStringExtra(Constants.QUERY));
	    }
	    
		sWaitForRouterServiceTask = new WaitForRouterServiceTask(intent, mQuery);
		sWaitForRouterServiceTask.execute();
	    
	    if (!TextUtils.isEmpty(mQuery)) {
			sSogouMusicSearcher.setQuery(mQuery);
			sFetchMp3ListTask = new FetchMp3ListTask(getApplication());
			sFetchMp3ListTask.execute();
	    }
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		Utils.D("onNewIntent");
		startQuery(intent);
	}
	
	public void dismissStreamingDialog() {
		if (mStreaming != null && mStreaming.isShowing()) {
			mStreaming.dismiss();
		}
	}
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.D("onCreate");
		
		sSearchResultActivity = this;
		
		bindService(new Intent(this, DownloadService.class),
				mConnection, Context.BIND_AUTO_CREATE);
		
		setContentView(R.layout.result_list);
		Utils.addMixedAds(this);
		
		
        mListView = (ListView)findViewById(R.id.result_list);
		
        mAdapter = new Mp3ListAdapter(
	        		SearchResultActivity.this,
	        		R.layout.result_item);
		
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				if (mSearchAdapter != null && position < mSearchAdapter.displaySize()) {
					mCurrentResult = mSearchAdapter.get(position);
                    showDialog(DIALOG_MUSIC_OPTIONS);
				}
			}
		});
		
		mQuery = getIntent().getStringExtra(Constants.QUERY);
        mSearch = new SearchBar(this, mQuery);
        
        mListener = new RouterServiceListener();
        
        App.init(getApplication());
    	App.getRouterService(mListener);
    	
    	mHandler.post(new Runnable() {
			@Override
			public void run() {
				startQuery(getIntent());
			}
    	});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		//Debug.stopMethodTracing();
	}
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopQuery();
        unbindService(mConnection);
        sSearchResultActivity = null;
	}
	
	private void stopQuery() {
       if (mGuid != null) {
            RouterService.stopQuery(new GUID(mGuid));
            mGuid = null;
        }
	}
		
	private class RouterServiceListener implements ActivityCallback {
		
		public RouterServiceListener() {}

		@Override
		public void acceptChat(Chatter ctr) {
		}

		@Override
		public void addDownload(Downloader d) {
		}

		@Override
		public void addressStateChanged() {
		}

		@Override
		public void browseHostFailed(GUID guid) {
		}

		@Override
		public void chatErrorMessage(Chatter chatter, String str) {
			// TODO Auto-generated method stub
		}

		@Override
		public void chatUnavailable(Chatter chatter) {
			// TODO Auto-generated method stub
		}

		@Override
		public void componentLoading(String component) {
			// TODO Auto-generated method stub
		}

		@Override
		public void connectionClosed(Connection c) {
			// TODO Auto-generated method stub
		}

		@Override
		public void connectionInitialized(Connection c) {
			// TODO Auto-generated method stub
		}

		@Override
		public void connectionInitializing(Connection c) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void downloadsComplete() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getHostValue(String key) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public User getUserAuthenticationInfo(String host) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public void retryQueryAfterConnect() {
		    stopQuery();
		    /*
		    (new Thread(new Runnable () {
                @Override
                public void run() {
                    try {
                        while (!RouterService.isConnected()) {
                            Thread.sleep(300);
                        }
                    } catch (InterruptedException e) {
                    	e.printStackTrace();
                    }
                    SearchResultActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            startQuery(getIntent());                            
                        }
                        
                    });
                }
		        
		    })).start();
		    */
		    try {
		    	while (!RouterService.isConnected()) {
		    		Thread.sleep(300);
		    	}
		    } catch (InterruptedException e) {
		    	e.printStackTrace();
		    }
		    //SearchResultActivity.this.runOnUiThread(new Runnable() {
		    mHandler.post(new Runnable() {

		    	@Override
		    	public void run() {
		    		startQuery(getIntent());                            
		    	}

		    });
		}

		@Override
		public synchronized void handleQueryResult(
		        final RemoteFileDesc rfd, final HostData data, final Set<Endpoint> locs) {
			//Utils.D("handleQueryResult");
			if (mGuid == null)
				return;
			
			if (data == null || rfd == null) {
			    //LOG.logSp("handleQueryResult null");
				return;
			}
 			
			final byte[] replyGuid = data.getMessageGUID();
			
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					//if (!mSearchAdapter.sameGuid(replyGuid) && mSearchAdapter.size() > 3) {
					if (!mSearchAdapter.sameGuid(replyGuid)) {
						return;
					}
					
                    /*
					if (mShowNoResult != null) {
						mHandler.removeCallbacks(mShowNoResult);
						mShowNoResult = null;
					}
                    */
					
					boolean shouldRefresh = mSearchAdapter.add(rfd, data, locs);
					int oldDisplaySize = mSearchAdapter.displaySize();
					
					//Utils.D("shouldRefresh: " + shouldRefresh);
					
					if (shouldRefresh) {
						if (mSearchAdapter.isBatchFull()) {
							mAdapter.setStatus(ListStatusView.Status.LOAD_MORE);
						} else {
							mAdapter.setStatus(ListStatusView.Status.SEARCHING);
						}
						mAdapter.notifyDataSetChanged();
					} else {
						if (mSearchAdapter.displaySize() != oldDisplaySize) {
							throw new IllegalStateException("display size does not match: " + mSearchAdapter.displaySize() + " vs " + oldDisplaySize);
						}
					}
					//mAdapter.notifyDataSetChanged();
				}
			});


			if (Utils.DEBUG) {
				synchronized(System.out) {
					Utils.D("Query hit from " + rfd.getHost() + ":" + rfd.getPort() + ":");
					Utils.D("filename: " + rfd.getFileName());
					//Utils.D("filesize: " + rfd.getSize());
					//Utils.D("speed: " + rfd.getSpeed());
					//Utils.D("Quality: " + rfd.getQuality());
					//Utils.D("Wait time: " + rfd.getWaitTime());
				}
			}
		}

		@Override
		public void handleQueryString(String query) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean isQueryAlive(GUID guid) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void promptAboutCorruptDownload(Downloader dloader) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void receiveMessage(Chatter chr) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void removeDownload(Downloader d) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void restoreApplication() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setAnnotateEnabled(boolean enabled) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void showDownloads() {
			// TODO Auto-generated method stub
			
		}
	}

    private final class Mp3ListAdapter extends BaseAdapter {	
    		
		private final static int VIEW_TYPE_NORMAL = 0;
		private final static int VIEW_TYPE_FOOTER = 1;

    	private int mResource;
    	private LayoutInflater mInflater;
    	
    	private ListStatusView.Status mStatus;

    	public Mp3ListAdapter(Context context, int resource) {
    		mResource = resource;
    		mInflater = (LayoutInflater)context.getSystemService(
    				Context.LAYOUT_INFLATER_SERVICE);
    		mStatus = ListStatusView.Status.OFFLINE;
    	}
    	
    	public void setStatus(ListStatusView.Status status) {
    		mStatus = status;
    	}
    	
    	@Override
    	public int getCount() {
            boolean showFooter =
                mStatus == ListStatusView.Status.LOAD_MORE ||
                mStatus == ListStatusView.Status.SEARCHING ||
                mStatus == ListStatusView.Status.NO_RESULT;

			int footerCount = showFooter ? 1 : 0;
			int count = 0;
    		if (mSearchAdapter == null)
    			count = footerCount;
    		else
	    		count = mSearchAdapter.displaySize() + footerCount;
    		
    		//Utils.D("getCount: " + count);
    		
    		return count;
    	}
    	
		@Override
		public Object getItem(int position) {
			if (mSearchAdapter == null)
				return null;
			
			if (position < mSearchAdapter.displaySize())
				return mSearchAdapter.get(position);
			return null;  // footer.
		}

		@Override
		public long getItemId(int position) {
			if (mSearchAdapter == null)
				return -1;
			
			if (position < mSearchAdapter.displaySize())
				return position;
			return -1;  // footer.
		}
		
		@Override
	    public int getViewTypeCount() {
			return 2;
	    }

	    @Override
	    public int getItemViewType(int position) {
	    	if (mSearchAdapter == null || position == mSearchAdapter.displaySize())
	    		return VIEW_TYPE_FOOTER;
	    	return VIEW_TYPE_NORMAL;
	    }
	    
	    OnClickListener mLoadMoreListener = new OnClickListener() {

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

		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			boolean isFooter = (mSearchAdapter == null || position == mSearchAdapter.displaySize());
			
			if (isFooter) {
				ListStatusView footerView = (ListStatusView) convertView;
				if (footerView == null) {
					footerView = (ListStatusView) mInflater.inflate(
							R.layout.liststatus, null);
				}
				if (mStatus == ListStatusView.Status.LOAD_MORE) {
					footerView.setStatusLoadMore();
					footerView.setOnClickListener(mLoadMoreListener);
				} else if (mStatus == ListStatusView.Status.SEARCHING) {
					footerView.setStatusSearching();
					footerView.setOnClickListener(null);
				} else if (mStatus == ListStatusView.Status.NO_RESULT) {
					footerView.setStatusNoResult();
					footerView.setOnClickListener(null);
				}
				return footerView;
			}

			View v;
			Object item = mSearchAdapter.get(position);
			if (convertView == null) {
				v = mInflater.inflate(mResource, parent, false);
			} else {
				v = convertView;
			}

			SearchResult rs = (SearchResult)item;
			((TextView)v.findViewById(R.id.name)).setText(rs.getFileName());
			((TextView)v.findViewById(R.id.size)).setText(Utils.displaySizeInMB(rs.getFileSize()));
			
			//Utils.D(rs.getFileName() + " : " + rs.getSize());

			return v;
		}
    }

    private class WaitForRouterServiceTask extends AsyncTask<Void, Void, Integer> {

        Intent mIntent;
        String mQuery;
        
        public WaitForRouterServiceTask(Intent intent, String query) {
            mIntent = intent;
            mQuery = query;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (sWaitForRouterServiceTask != this) {
                return;
            }
            
            sWaitForRouterServiceTask = null;

            String xml = mIntent == null ? null : mIntent.getStringExtra(Constants.XML);
            if (xml == null)
            	xml = "";

            if (!TextUtils.isEmpty(mQuery) ||
            	!TextUtils.isEmpty(xml)) {
            	stopQuery();
            	mGuid = RouterService.newQueryGUID();
            	mSearchAdapter = new SearchAdapter(mGuid);
            	mAdapter.notifyDataSetChanged();
        		RouterService.query(mGuid, mQuery, xml, MediaType.TYPE_MP3);
            }
        }

        @Override
        protected Integer doInBackground(Void... params) {
        	try {
		        while (!RouterService.isConnected()) {
		        	Utils.D("RouterService not ready");
					Thread.sleep(500);
		        }
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
            return 0;
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
    		if (sSearchResultActivity != null) {
    			sSearchResultActivity.handleSogouSearchResult(mp3List);
    		}
    	}

    	@Override
    	protected ArrayList<SogouSearchResult> doInBackground(Void... params) {
    		return sSogouMusicSearcher.getNextResultList(mContext);
    	}
    }
    
    private void handleSogouSearchResult(ArrayList<SogouSearchResult> mp3List) {    
      ArrayList<SogouSearchResult> newList = Utils.dedup(mp3List);
      if (newList != null && mSearchAdapter != null) {
        for (Iterator<SogouSearchResult> it = newList.iterator(); it.hasNext();) {
          SogouSearchResult result = it.next();
          boolean shouldRefresh = mSearchAdapter.add(result);
          int oldDisplaySize = mSearchAdapter.displaySize();
          if (shouldRefresh) {
            if (mSearchAdapter.isBatchFull()) {
                mAdapter.setStatus(ListStatusView.Status.LOAD_MORE);
            } else {
                mAdapter.setStatus(ListStatusView.Status.SEARCHING);
            }
            mAdapter.notifyDataSetChanged();
          } else {
            if (mSearchAdapter.displaySize() != oldDisplaySize) {
                throw new IllegalStateException("display size does not match: " + mSearchAdapter.displaySize() + " vs " + oldDisplaySize);
            }
          }
        }
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
    		  Toast.makeText(SearchResultActivity.this, R.string.no_download_link, Toast.LENGTH_SHORT).show();
    		  return;
    	  }

    	  preview(mp3);
      }
    }
    
    public static void handleMp3ListIntent(Context context, String key, String keyWords) {
        Intent intent = new Intent(context, SearchResultActivity.class);
        intent.putExtra(key, keyWords);
        context.startActivity(intent);
    }    
	public static void handleMp3ListSimpleIntent(Context context, String keyWords) {
        Intent intent = new Intent(context, SearchResultActivity.class);
        intent.putExtra(Constants.QUERY, keyWords);
        context.startActivity(intent);
	}
    public static void handleMp3ListXMLIntent(Context context, String keyWords, String xml) {
        Intent intent = new Intent(context, SearchResultActivity.class);
        intent.putExtra(Constants.QUERY, keyWords);
        intent.putExtra(Constants.XML, xml);
        context.startActivity(intent);
    }
}
