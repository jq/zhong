package com.lib;

import android.os.Bundle;

import com.feebe.lib.AdListener;
import com.ringdroid.R;
import com.ringdroid.RingdroidEditActivity;

public class RingEditor extends RingdroidEditActivity {
  @Override
  public void onCreate(Bundle icicle) {
      super.onCreate(icicle);
      AdListener.createAds(this, R.id.mediaselect);
  }
}
