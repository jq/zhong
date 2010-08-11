package com.other;

import android.os.Bundle;

import com.ringdroid.R;
import com.ringdroid.RingdroidEditActivity;
import com.trans.music.search.AdListener;

public class RingEditActivity extends RingdroidEditActivity {
  @Override
  public void onCreate(Bundle icicle) {
      super.onCreate(icicle);
      AdListener.createAds(this, R.id.mediaselect);
  }
}
