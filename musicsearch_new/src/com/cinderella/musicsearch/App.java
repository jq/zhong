package com.cinderella.musicsearch;

import android.app.Application;

public class App extends Application {

	@Override
	public void onCreate() {
		Const.init(getApplicationContext());
		super.onCreate();
	}

}
