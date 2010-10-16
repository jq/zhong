package com.cinderella.musicsearch;

import android.app.Activity;
import android.os.Build;
import android.view.ViewGroup;
import android.widget.RelativeLayout.LayoutParams;
 
import com.adwhirl.AdWhirlLayout;

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
    //// Log.e("model", Build.MODEL + " " + Build.DEVICE);
    int w;
    if (blackscreen) {
      w = 48;
    } else {
      w = LayoutParams.WRAP_CONTENT;
    }
    AdWhirlLayout adWhirlLayout = new AdWhirlLayout(activity, "8e3af243d20e48ebb96908c4e9ab8dee");
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