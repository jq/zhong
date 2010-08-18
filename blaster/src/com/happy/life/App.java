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
import com.util.P2PApp;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class App extends P2PApp {
	public static void init(Context context) {
		// Start service
	  if (P2PApp.initP2P(context)) {
  		Intent intent = new Intent(context, DownloadService.class);
  		context.startService(intent);
  		
  		intent = new Intent(context, MiniServer.class);
  		context.startService(intent);
	  }
	}
	
	@Override
  public void onCreate() {
    Constants.init(this);
    init(this);
    // the order has to be like this, since super.onCreate depends on this init
		super.onCreate();
	}
}


