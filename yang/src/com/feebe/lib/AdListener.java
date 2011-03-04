package com.feebe.lib;

import android.app.Activity;
import android.os.Build;
import android.view.ViewGroup;
import android.widget.RelativeLayout.LayoutParams;
 
import com.adwhirl.AdWhirlLayout;
import com.adwhirl.AdWhirlTargeting;
import com.feebe.rings.R;

public class AdListener {
  private static final boolean blackscreen = isBlackScreen();
  public static final String key = "6b29d50cab9641aeb3a555d3aa3dfedd";
  public final static String SKEY = "OKRGCHMZ";
  private static boolean isBlackScreen() {
    // http://since2006.com/blog/google-io2010-android-devices/
    // 6318c8e4e86d4732b08a67a89618f391 366e6bc811204d1a9861b6149f05c22f
    // bcb2a6fca76c487e9662890bd595c127
    // 6b29d50cab9641aeb3a555d3aa3dfedd
    return Build.VERSION.SDK.equalsIgnoreCase("3");
  }
  public static final String keywords =
    "game music sex gambling girl news cell house car computer laptop iphone ipad dating shopping health finance job movie sports travel";
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