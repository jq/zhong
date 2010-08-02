package com.other;

import android.content.res.Configuration;
import android.os.Bundle;

import com.happy.life.R;
import com.happy.life.Utils;
import com.ringdroid.RingdroidEditActivity;
import com.ringdroid.RingdroidSelectActivity;

public class RingSelectActivity extends RingdroidSelectActivity {
  @Override
  public void onCreate(Bundle icicle) {
    super.EDITOR = "com.other.RingEditActivity";
    super.onCreate(icicle);
    Utils.addMixedAds(this, R.id.mediaselect);
  }
  @Override
	public void onConfigurationChanged(Configuration newConfig) {
  	super.onConfigurationChanged(newConfig);
    Utils.addMixedAds(this, R.id.mediaselect);
  }
}