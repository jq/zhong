
package com.happy.life;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import com.limegroup.gnutella.settings.SharingSettings;

public class MiniServer extends Service{
	
	public static boolean sFinished = false;
    
    private static final String rootDir = SharingSettings.INCOMPLETE_DIRECTORY.getAbsolutePath();
    
    private static SimpleWebServer server;
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.D("Mini Server: onCreate()");
        try {
            server = new SimpleWebServer(rootDir, Constants.MINI_SERVER_PORT);
        } catch (IOException e) {
        	e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (server != null) {
            server.onDestroy();
        }
    }
}
