package com.other;

import android.content.res.Configuration;
import android.os.Bundle;

import com.fatima.life2.Utils;
import com.fatima.life2.R;
import com.ringdroid.RingdroidEditActivity;
import com.ringdroid.RingdroidSelectActivity;

public class RingSelectActivity extends RingdroidSelectActivity {
  @Override
  public void onCreate(Bundle icicle) {
    RingdroidSelectActivity.EDITOR = "com.other.RingEditActivity";
    super.onCreate(icicle);
    Utils.addMixedAds(this, R.id.mediaselect);
  }
  @Override
	public void onConfigurationChanged(Configuration newConfig) {
  	super.onConfigurationChanged(newConfig);
    Utils.addMixedAds(this, R.id.mediaselect);
  }
}
