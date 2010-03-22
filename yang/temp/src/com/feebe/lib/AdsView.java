package com.feebe.lib;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;


import com.qwapi.adclient.android.data.Ad;
import com.qwapi.adclient.android.data.Status;
import com.qwapi.adclient.android.requestparams.AdRequestParams;
import com.qwapi.adclient.android.requestparams.AnimationType;
import com.qwapi.adclient.android.requestparams.DisplayMode;
import com.qwapi.adclient.android.requestparams.MediaType;
import com.qwapi.adclient.android.requestparams.Placement;
import com.qwapi.adclient.android.view.AdEventsListener;
import com.qwapi.adclient.android.view.QWAdView;
/*

public class AdsView implements AdView.AdListener{
    private AdView mAd;

    public boolean show = false;
    public AdsView(Activity activity){
        mAd = (AdView) activity.findViewById(R.id.ad);       
        mAd.setListener(this);
    }
    
    public void hide() {
      mAd.setVisibility(View.GONE);
      show = false;
    }
    
    public void show() {
      mAd.setVisibility(View.VISIBLE);
      mAd.requestFreshAd();
      show = true;
    }
    public void onNewAd() {
      if (mAd.getVisibility() != View.VISIBLE ) {               
        mAd.setVisibility(View.VISIBLE);
      }
    }
    
}

*/


public class AdsView {
    public static boolean mShowQuattroAd = true;
    
    public static void createAdMobAd(Activity activity,String keywords, int adsViewId){
    	com.admob.android.ads.AdView adView = new com.admob.android.ads.AdView(activity);
    	adView.setBackgroundColor(0);
    	adView.setTextColor(0xFFFFFFFF);
    	adView.setKeywords(keywords);
    	ViewGroup parentView = (ViewGroup)activity.findViewById(Const.AdsViewID);
    	parentView.addView(adView);
    }
    
    
    public static void createQWAd(Activity activity){
    	if(!mShowQuattroAd)
    		return;
    	ViewGroup parentView = (ViewGroup)activity.findViewById(Const.AdsViewID);
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
