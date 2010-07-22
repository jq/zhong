package com.feebe.lib;

import android.app.Activity;
import android.os.Build;
import android.view.ViewGroup;
import android.widget.RelativeLayout.LayoutParams;
 
import com.adwhirl.AdWhirlLayout;
import com.feebe.rings.R;

public class AdListener {
  private static final boolean blackscreen = isBlackScreen();
  
  private static boolean isBlackScreen() {
    // http://since2006.com/blog/google-io2010-android-devices/
    return Build.VERSION.SDK.equalsIgnoreCase("3");

  }
  public static void createAds(Activity activity) {
    //Log.e("model", Build.MODEL + " " + Build.DEVICE);
    int w;
    if (blackscreen) {
      w = 48;
    } else {
      w = LayoutParams.WRAP_CONTENT;
    }
    AdWhirlLayout adWhirlLayout = new AdWhirlLayout(activity, "9e817eff582a444cbb34c339e2523693");
    LayoutParams adWhirlLayoutParams = new LayoutParams(LayoutParams.FILL_PARENT, w);
    ViewGroup layout = (ViewGroup) activity.findViewById(R.id.ads_view);
    layout.addView(adWhirlLayout, adWhirlLayoutParams);
  }
}