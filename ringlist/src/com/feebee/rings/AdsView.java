package com.feebee.rings;
import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;

import com.feebe.lib.Const;
import com.feebee.rings.R;
import com.google.ads.AdSenseSpec;
import com.google.ads.AdSenseSpec.AdType;
import com.qwapi.adclient.android.data.Ad;
import com.qwapi.adclient.android.data.Status;
import com.qwapi.adclient.android.requestparams.AdRequestParams;
import com.qwapi.adclient.android.requestparams.AnimationType;
import com.qwapi.adclient.android.requestparams.DisplayMode;
import com.qwapi.adclient.android.requestparams.MediaType;
import com.qwapi.adclient.android.requestparams.Placement;
import com.qwapi.adclient.android.view.AdEventsListener;
import com.qwapi.adclient.android.view.QWAdView;

public class AdsView extends com.feebe.lib.AdsView {
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

  public static void createQWAd(Activity activity){
  	if(!mShowQuattroAd)
  		return;
  	ViewGroup parentView = (ViewGroup)activity.findViewById(R.id.AdsView);
  	QWAdView adView = new QWAdView(activity,
  			Const.QWName,
  			Const.QWID,
  			MediaType.banner,
  			Placement.bottom,
  			DisplayMode.autoRotate,30,
  			AnimationType.slide,
  			new QWAdEventsListener(parentView),true);
  	parentView.addView(adView);
  }
  

  private static class QWAdEventsListener implements AdEventsListener {
  	private ViewGroup mAdsView = null;
      
  	public QWAdEventsListener(ViewGroup view) {
          mAdsView = view;
  	}
  	
  	public void onAdClick(Context ctx,Ad ad) {
  		//Log.i("Snake","onAdClick for Ad: " + ad.getAdType() + " : " + ad.getId());
  		if(mAdsView!=null){
  			//mAdsView.setVisibility(View.GONE);
  		}
  		//mShowQuattroAd = false;
  	}

  	public void onAdRequest(Context ctx,AdRequestParams params) {
  		//Log.i("Snake","onAdRequest for RequestParams: " + params.toString());
  	}

  	public void onAdRequestFailed(Context ctx,AdRequestParams params, Status status) {
  		//Log.i("Snake","onAdRequestFailed for RequestParams: " + params.toString() + " : " + status);
  	}

  	public void onAdRequestSuccessful(Context ctx,AdRequestParams params, Ad ad) {
  		//Log.i("Snake","onAdRequestSuccessful for RequestParams: " + params.toString() + " : Ad: " + ad.getAdType() + " : " + ad.getId());
  	}

  	public void onDisplayAd(Context ctx,Ad ad) {
  		//Log.i("Snake","onDisplayAd for Ad: " + ad.getAdType() + " : " + ad.getId());
  	}
  }

}
