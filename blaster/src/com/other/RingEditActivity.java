package com.other;

import android.os.Bundle;

import com.happy.life.R;
import com.happy.life.Utils;
import com.ringdroid.RingdroidEditActivity;

public class RingEditActivity extends RingdroidEditActivity {
  @Override
  public void onCreate(Bundle icicle) {
      super.onCreate(icicle);
      Utils.addMixedAds(this, R.id.ringeditor);
  }
}
