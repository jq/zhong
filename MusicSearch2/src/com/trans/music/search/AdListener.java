package com.trans.music.search;

import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import com.adwhirl.AdWhirlLayout;

public class AdListener {
	private static final boolean blackscreen = isBlackScreen();
	
	private static boolean isBlackScreen() {
        // http://since2006.com/blog/google-io2010-android-devices/
		return Build.VERSION.SDK.equalsIgnoreCase("3");
	}
	/*
	public static void createAds(Activity activity, int id) {
        AdWhirlLayout adWhirlLayout = new AdWhirlLayout(activity, "50f8d6150f4541d49f372f98693aaef6");
        LayoutParams adWhirlLayoutParams = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        ViewGroup layout = (ViewGroup) activity.findViewById(id);
        layout.addView(adWhirlLayout, adWhirlLayoutParams);
	}
	*/
	//8af9f3794ffb444e836191039e606cb9 seems causing crash on MM ads.
	// 50f8d6150f4541d49f372f98693aaef6 seems ok with MM ads.
    public static void createAds(Activity activity) {
    	//Log.e("model", Build.MODEL + " " + Build.VERSION.SDK);
    	int w;
        if (blackscreen) {
            w = 48;
        } else {
            w = LayoutParams.WRAP_CONTENT;
        }
      AdWhirlLayout adWhirlLayout = new AdWhirlLayout(activity, "50f8d6150f4541d49f372f98693aaef6");
      LayoutParams adWhirlLayoutParams = new LayoutParams(LayoutParams.FILL_PARENT, w);
      //activity.addContentView(adWhirlLayout, adWhirlLayoutParams);
      ViewGroup layout = (ViewGroup) activity.findViewById(R.id.ads_view);
      layout.addView(adWhirlLayout, adWhirlLayoutParams);
  }
}