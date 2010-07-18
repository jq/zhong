package com.feebe.rings;
import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;

import com.feebe.lib.Const;
import com.google.ads.AdSenseSpec;
import com.google.ads.AdSenseSpec.AdType;
public class AdsView {
  // Replace with your own AdSense client ID.
  private static final String CLIENT_ID = "ca-mb-app-pub-1063898187057934";
  // Replace with your own company name.
  private static final String COMPANY_NAME = "Feebe Mobile";
  // Replace with your own application name.
  private static final String APP_NAME = "Feebe Ringtone";
  // Replace with your own keywords used to target Google ad.
  // Join multiple words in a phrase with '+' and join multiple phrases with ','.
  private static final String KEYWORDS = "ringtones,search+ringtone,free+downloads+ringtone,mobile+ringtone,phones+ringtone";
  // Replace with your own AdSense channel ID.
  public static final String CHANNEL_ID = "9815443938";
  public static final String CHANNEL_ID_2 = "3228384568";
  
  public static void createAdsenseAds(Activity activity , String chanID){
    com.google.ads.GoogleAdView adsense = new com.google.ads.GoogleAdView(activity);
    AdSenseSpec adSenseSpec =
            new AdSenseSpec(CLIENT_ID)     // Specify client ID. (Required)
            .setCompanyName(COMPANY_NAME)  // Set company name. (Required)
            .setAppName(APP_NAME)          // Set application name. (Required)
            .setKeywords(KEYWORDS)         // Specify keywords.
            .setChannel(chanID)        // Set channel ID.
            .setAdType(AdType.TEXT_IMAGE)        // Set ad type to Text.
            .setAdTestEnabled(false);       // Keep true while testing.
    adsense.showAds(adSenseSpec);
    ViewGroup parentView = (ViewGroup)activity.findViewById(R.id.AdsView);
    parentView.setFocusable(false);
    parentView.addView(adsense);
  }

}
