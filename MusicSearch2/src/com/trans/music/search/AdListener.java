package com.trans.music.search;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
 
import com.admob.android.ads.AdView;
import com.adwhirl.AdWhirlLayout;
import com.qwapi.adclient.android.data.Ad;
import com.qwapi.adclient.android.data.Status;
import com.qwapi.adclient.android.requestparams.AdRequestParams;
import com.qwapi.adclient.android.view.AdEventsListener;
import com.qwapi.adclient.android.view.QWAdView;

public class AdListener {
	public static void createAds(Activity activity, int id) {
        AdWhirlLayout adWhirlLayout = new AdWhirlLayout(activity, "8af9f3794ffb444e836191039e606cb9");
        RelativeLayout.LayoutParams adWhirlLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        ViewGroup layout = (ViewGroup) activity.findViewById(id);
        layout.addView(adWhirlLayout, adWhirlLayoutParams);
	}
    public static void createAds(Activity activity) {
      AdWhirlLayout adWhirlLayout = new AdWhirlLayout(activity, "8af9f3794ffb444e836191039e606cb9");
      RelativeLayout.LayoutParams adWhirlLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
      ViewGroup layout = (ViewGroup) activity.findViewById(R.id.ads_view);
      layout.addView(adWhirlLayout, adWhirlLayoutParams);
  }
}