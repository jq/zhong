package com.macrohard.musicbug;

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

public class Ads {        
    public static void createQWAd(Activity activity){
    	ViewGroup parentView = (ViewGroup)activity.findViewById(R.id.AdsView);
    	QWAdView adView = new QWAdView(activity,
    			"Music-g7gon821",
    			"a7beb24f184b4305804013cbc447f678",
    			MediaType.banner,
    			Placement.bottom,
    			DisplayMode.autoRotate, 30,
    			AnimationType.slide,
    			new QWAdEventsListener(),true);
    	parentView.addView(adView);
    }

    private static class QWAdEventsListener implements AdEventsListener {
            	
    	public void onAdClick(Context ctx,Ad ad) {
    	}

    	public void onAdRequest(Context ctx,AdRequestParams params) {
    	}

    	public void onAdRequestFailed(Context ctx,AdRequestParams params, Status status) {
    	}

    	public void onAdRequestSuccessful(Context ctx,AdRequestParams params, Ad ad) {
    	}

    	public void onDisplayAd(Context ctx,Ad ad) {
    	}
    }
    
}

