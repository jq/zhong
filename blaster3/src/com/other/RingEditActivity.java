package com.other;

import android.os.Bundle;

import com.fatima.life2.Utils;
import com.fatima.life2.R;
import com.ringdroid.RingdroidEditActivity;

public class RingEditActivity extends RingdroidEditActivity {
  @Override
  public void onCreate(Bundle icicle) {
      super.onCreate(icicle);
      Utils.addMixedAds(this, R.id.ringeditor);
  }
}
