package com.other;

import android.content.res.Configuration;
import android.os.Bundle;

import com.trans.music.search.AdListener;
import com.trans.music.search.R;
import com.ringdroid.RingdroidEditActivity;
import com.ringdroid.RingdroidSelectActivity;

public class RingSelectActivity extends RingdroidSelectActivity {
  @Override
  public void onCreate(Bundle icicle) {
    super.EDITOR = "com.other.RingEditActivity";
    super.onCreate(icicle);
    AdListener.createAds(this);
  }
  @Override
	public void onConfigurationChanged(Configuration newConfig) {
  	super.onConfigurationChanged(newConfig);
  	AdListener.createAds(this);
  }
}
