package com.util;

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

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.R;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.chat.Chatter;
import com.limegroup.gnutella.downloader.AlreadyDownloadingException;
import com.limegroup.gnutella.downloader.FileExistsException;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.security.User;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.StringUtils;
import com.util.DownloadInfo;
import com.util.DownloadService;
import com.util.ListStatusView;
import com.util.P2pSearchResult;
import com.util.SearchResult;
import com.util.SearchBar;

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
	protected static final int DIALOG_WAITING_FOR_SERVER = 1; 
	protected static final int DIALOG_MUSIC_OPTIONS = 2;    
    
    private static final int MAX_SEARCH_DELAY = 60000;

	protected SearchResult mCurrentResult;        
	
	protected static WaitForRouterServiceTask sWaitForRouterServiceTask;
	
	protected SearchBar mSearch;
	protected String mQuery;
	
	protected ProgressDialog mProgressDialog;

	protected DownloadService mDownloadService;
	
	protected byte[] mGuid;
	protected Handler mHandler = new Handler();
	protected ActivityCallback mListener;
	
	protected ListView mListView;
	protected Mp3ListAdapter mAdapter;
	protected volatile SearchAdapter mSearchAdapter;
	
	protected static volatile SearchResultActivity sSearchResultActivity;
	
    protected ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mDownloadService = ((DownloadService.LocalBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mDownloadService = null;
        }
    };
    
    protected SearchAdapter getSearchAdapter() {
      return new SearchAdapter(mGuid);
    }
    private void download(SearchResult mp3) {
      mDownloadService.insertDownload(mp3.createDownloadInfo());
      Intent intent = new Intent(SearchResultActivity.this, DownloadActivity.class);
      startActivity(intent);
    }
    
	protected static String getURL(String link) throws IOException {
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

	protected void startQuery(Intent intent) {
	    mQuery = mSearch.getQuery();
	    
    	mAdapter.setStatus(ListStatusView.Status.SEARCHING);
    	mAdapter.notifyDataSetChanged();
	    
	    if (TextUtils.isEmpty(mQuery)) {
        	mQuery = StringUtils.removeIllegalChars(intent.getStringExtra(Constants.QUERY));
	    }
	    
		sWaitForRouterServiceTask = new WaitForRouterServiceTask(intent, mQuery);
		sWaitForRouterServiceTask.execute();
	    
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		com.util.Utils.D("onNewIntent");
		startQuery(intent);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		com.util.Utils.D("onCreate");
		
		sSearchResultActivity = this;
		
		bindService(new Intent(this, DownloadService.class),
				mConnection, Context.BIND_AUTO_CREATE);
		
		setContentView(R.layout.result_list);
		//Utils.addMixedAds(this);
		
		
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
		
        mSearch = new SearchBar(this);
        
        mListener = new RouterServiceListener();
        
        //App.init(getApplication());
    	P2PApp.getRouterService(mListener);
    	
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
		
	protected class RouterServiceListener implements ActivityCallback {
		
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
			//com.util.Utils.D("handleQueryResult");
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
					
					//com.util.Utils.D("shouldRefresh: " + shouldRefresh);
					
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


			if (com.util.Utils.DEBUG) {
				synchronized(System.out) {
					com.util.Utils.D("Query hit from " + rfd.getHost() + ":" + rfd.getPort() + ":");
					com.util.Utils.D("filename: " + rfd.getFileName());
					//com.util.Utils.D("filesize: " + rfd.getSize());
					//com.util.Utils.D("speed: " + rfd.getSpeed());
					//com.util.Utils.D("Quality: " + rfd.getQuality());
					//com.util.Utils.D("Wait time: " + rfd.getWaitTime());
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

    protected class Mp3ListAdapter extends BaseAdapter {	
    		
		private final static int VIEW_TYPE_NORMAL = 0;
		private final static int VIEW_TYPE_FOOTER = 1;

    	private int mResource;
    	private LayoutInflater mInflater;
    	
    	protected ListStatusView.Status mStatus;

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
    		
    		//com.util.Utils.D("getCount: " + count);
    		
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
	    
	    protected OnClickListener mLoadMoreListener = new OnClickListener() {

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
                            //sFetchMp3ListTask = new FetchMp3ListTask(getApplication());
                            //sFetchMp3ListTask.execute();
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
			
			//com.util.Utils.D(rs.getFileName() + " : " + rs.getSize());

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
            	mSearchAdapter = getSearchAdapter();
            	mAdapter.notifyDataSetChanged();
        		RouterService.query(mGuid, mQuery, xml, MediaType.TYPE_MP3);
            }
        }

        @Override
        protected Integer doInBackground(Void... params) {
        	try {
		        while (!RouterService.isConnected()) {
		        	com.util.Utils.D("RouterService not ready");
					Thread.sleep(500);
		        }
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
            return 0;
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
