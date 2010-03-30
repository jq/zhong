package com.macrohard.musicbug;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

public class App extends Application {
	public static JTellaAdapter jta;

	@Override
    public void onCreate() {
		// TODO enable p2p
		// jta = new JTellaAdapter();	
    }
}


