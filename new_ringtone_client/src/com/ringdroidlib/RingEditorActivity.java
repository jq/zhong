package com.ringdroidlib;

import android.os.Bundle;

import com.cinla.ringtone.AdListener;
import com.ringdroid.R;
import com.ringdroid.RingdroidEditActivity;

public class RingEditorActivity extends RingdroidEditActivity {
	@Override
	  public void onCreate(Bundle icicle) {
	      super.onCreate(icicle);
	      AdListener.createAds(this, R.id.mediaselect);
	  }
}
