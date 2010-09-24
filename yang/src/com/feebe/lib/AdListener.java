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
    createAds(activity, R.id.ads_view);
  }
  public static void createAds(Activity activity, int id) {
    //// // Log.e("model", Build.MODEL + " " + Build.DEVICE);
    int w;
    if (blackscreen) {
      w = 48;
    } else {
      w = LayoutParams.WRAP_CONTENT;
    }
    AdWhirlLayout adWhirlLayout = new AdWhirlLayout(activity, "bcb2a6fca76c487e9662890bd595c127");
    LayoutParams adWhirlLayoutParams = new LayoutParams(LayoutParams.FILL_PARENT, w);
    ViewGroup layout = (ViewGroup) activity.findViewById(id);
    if (layout != null) {
      try { 
        layout.addView(adWhirlLayout, adWhirlLayoutParams);
      } catch (Exception e) {
        
      }
    }
  }
}