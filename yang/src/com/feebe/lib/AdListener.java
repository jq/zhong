package com.feebe.lib;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
 
import com.admob.android.ads.AdView;
import com.feebe.rings.R;
import com.qwapi.adclient.android.data.Ad;
import com.qwapi.adclient.android.data.Status;
import com.qwapi.adclient.android.requestparams.AdRequestParams;
import com.qwapi.adclient.android.view.AdEventsListener;
import com.qwapi.adclient.android.view.QWAdView;

public class AdListener implements AdEventsListener {
	private QWAdView qwAdView;
	private AdView adMobView;
 
	public AdListener(Activity context) {
		qwAdView = (QWAdView) context.findViewById(R.id.QWAd);
		adMobView = (AdView) context.findViewById(R.id.adMob);
	}
 
	@Override
	public void onAdClick(Context arg0, Ad arg1) {
	}
 
	@Override
	public void onAdRequest(Context arg0, AdRequestParams arg1) {
	}
 
	@Override
	public void onAdRequestFailed(Context arg0, AdRequestParams arg1,
			Status arg2) {
		Message.obtain(hideHandler, -1).sendToTarget();
	}
 
        /** Hide Quattro and request Ads from AdMob when Quattro returns no Ads. */
	protected Handler hideHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			qwAdView.setVisibility(View.GONE);
			adMobView.requestFreshAd();
		}
	};
 
        /** Display Quattro banner when it returns an Ads. */
	protected Handler showHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			qwAdView.setVisibility(View.VISIBLE);
		}
	};
 
	@Override
	public void onAdRequestSuccessful(Context arg0, AdRequestParams arg1,
			Ad arg2) {
		Message.obtain(showHandler, -1).sendToTarget();
	}
 
	@Override
	public void onDisplayAd(Context arg0, Ad arg1) {
	}
 
}