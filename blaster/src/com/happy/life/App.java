package com.happy.life;

import java.util.Set;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.ErrorCallback;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.chat.Chatter;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.security.User;
import com.limegroup.gnutella.settings.SharingSettings;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class App extends Application {

	private static RouterService sRouterService;
	private static boolean sInitialized = false;
	
	public static RouterService getRouterService(ActivityCallback callback) {
		sRouterService.setCallback(callback);
		return sRouterService;
	}
	
	ErrorCallback sErrorCallback = new ErrorCallback() {

		@Override
		public void error(Throwable t) {
			// TODO Auto-generated method stub
			t.printStackTrace();
		}

		@Override
		public void error(Throwable t, String msg) {
			// TODO Auto-generated method stub
			t.printStackTrace();
			Log.e(Utils.TAG, msg);
		}
	};
	
	public static void init(Context context) {
		if (sInitialized)
			return;
		
        // TODO: Check if sdcard is available and if space is full.
        SharingSettings.setSaveDirectory();
        
        Utils.copyConfigFile(context);

		// Start service
		Intent intent = new Intent(context, DownloadService.class);
		context.startService(intent);
		
		intent = new Intent(context, MiniServer.class);
		context.startService(intent);
		
		sInitialized = true;
	}
	
	private static class DummyCallback implements ActivityCallback {

		@Override
		public void acceptChat(Chatter ctr) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addDownload(Downloader d) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addressStateChanged() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void browseHostFailed(GUID guid) {
			// TODO Auto-generated method stub
			
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

        @Override
        public void handleQueryResult(RemoteFileDesc rfd, HostData data, Set<Endpoint> locs) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void retryQueryAfterConnect() {
            // TODO Auto-generated method stub
        }
	};
	
	@Override
    public void onCreate() {
		super.onCreate();
        Constants.init(this);
        
        init(this);
        
        new Thread(new Runnable() {
			@Override
			public void run() {
				sRouterService = new RouterService(new DummyCallback());
				Utils.D("Starting router service.");
				sRouterService.start();
			}
        }).start();
    }
}


