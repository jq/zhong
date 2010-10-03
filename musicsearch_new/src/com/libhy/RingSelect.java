package com.libhy;

import android.content.res.Configuration;
import android.os.Bundle;

import com.ringdroid.RingdroidSelectActivity;

public class RingSelect extends RingdroidSelectActivity {
	@Override
	public void onCreate(Bundle icicle) {
	    super.EDITOR = "com.libhy.RingEditActivity";
	    super.onCreate(icicle);
	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
}
