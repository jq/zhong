package com.cinla.ringtone;

import android.app.Activity;
import android.os.Build;
import android.view.ViewGroup;
import android.widget.RelativeLayout.LayoutParams;
 
import com.adwhirl.AdWhirlLayout;
import com.adwhirl.AdWhirlTargeting;

public class AdListener {
  private static final boolean blackscreen = isBlackScreen();
  public static final String key = "492746e7df3b4cf7a163063a0474b7fd";
//  public static final String key = "e383f83acfec4f34b591486a93c4da96";
  private static boolean isBlackScreen() {
    // http://since2006.com/blog/google-io2010-android-devices/
    return Build.VERSION.SDK.equalsIgnoreCase("3");
  }
  public static final String keywords = "game music sex gambling girl news cell house car computer laptop";
  static {
    AdWhirlTargeting.setKeywords(keywords);
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
    AdWhirlLayout adWhirlLayout = new AdWhirlLayout(activity, key);
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