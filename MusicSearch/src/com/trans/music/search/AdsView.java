package com.trans.music.search;


import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
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

public class AdsView {
	public final static String KW_POKER="poker, online poker, texas holdem, poker games, poker tournaments, poker room reviews, poker rules, casino, online casino,casino game,gamble game,gamble";
	public final static String KW_GIFTCARD="Gift card, gift cards, gift card program, online gift card, prepaid gift card, gift credit card, best buy gift card, card fulfillment gift marketing, discount gift cards, buy gift card, gift cards online";
	public final static String KW_DATING="adult dating, adult personals, chat, chat online, chat room, contact, dating, dating chat, dating service, dating site, free chat, free dating online, friend finder, friends, friendship, gay, girl, girl friend, girlfriend, guy, hot girl, hot guy, hot woman, internet dating, internet personal, lady, lesbian, love, lover, man, match maker, meet singles, online dating, online personals, penpal, personal web site, personals, romance, romance online, romantic, senior dating, sexy girl, sexy woman, single connection, single man, single man seeking woman, single romance, singles, soul mate, woman";
	public final static String KW_FEMAIL="prescription drugs,health,beauty,green,natural,pet,pet products,food,gourmet,toys";
	public final static String KW_CAR="cars, used cars, car dealers, auto dealers, buy a car, used trucks, used auto, buy new car, buy car online, sell car, sell my car, sell used car, auto sale, sell used cars, selling a car, auto loan calculator, used car prices, car reviews, car loan calculator, used car pricing";


	public final static String KW_MOSTVALUE="game, job, shop, computer, girl, music, car";
	
	
	// Replace with your own AdSense client ID.
    private static final String CLIENT_ID = "ca-mb-app-pub-6828968228153650";
    // Replace with your own company name.
    private static final String COMPANY_NAME = "Waf.yang";
    // Replace with your own application name.
    private static final String APP_NAME = "Quark Ringtones";
    // Replace with your own keywords used to target Google ad.
    // Join multiple words in a phrase with '+' and join multiple phrases with ','.
    private static final String KEYWORDS = "ringtones,search+ringtone,free+downloads+ringtone,mobile+ringtone,phones+ringtone";
    // Replace with your own AdSense channel ID.
    public static final String CHANNEL_ID = "0039771652";

    
	//private AdView mAd = null;
    public static boolean mShowQuattroAd = true;
    
    public static void createAdMobAd(Activity activity,String keywords){
    	com.admob.android.ads.AdView adView = new com.admob.android.ads.AdView(activity);
    	adView.setBackgroundColor(0);
    	adView.setTextColor(0xFFFFFFFF);
    	adView.setKeywords(keywords);
    	ViewGroup parentView = (ViewGroup)activity.findViewById(R.id.AdsView);
    	parentView.addView(adView);
    }
    
    
    public static void createQWAd(Activity activity){
    	//如果用户点击之后，则不再显示广告
    	if(!mShowQuattroAd)
    		return;
    	ViewGroup parentView = (ViewGroup)activity.findViewById(R.id.AdsView);
    	QWAdView adView = new QWAdView(activity,
    			"MusicSearch-g4qbie2h",
    			"79055569649a445d90cb15a3568f7db1",
    			MediaType.banner,
    			Placement.bottom,
    			DisplayMode.autoRotate,30,
    			AnimationType.slide,
    			new QWAdEventsListener(parentView),true);
    	parentView.addView(adView);
    }
    

/*
	private static final int AD_MSG_SHOW = 1;
	//private static final int AD_MSG_HIDE = 2;	
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
			Log.e("OnlineMusic", "Handler : " + mAd.hasAd());
            switch (msg.what) {
                case AD_MSG_SHOW:
					Log.e("OnlineMusic", "Handler ad : " + mAd.hasAd());
					if(mAd.hasAd() == false){
						Log.i("OnlineMusic", "Handler 1");
                        
                        //if(mShowQuattroAd){
                        //    AdEventsListener listener = new MyAdEventsListener(AdsView.this); 
                        //    adView = new QWAdView(mActivity,"iMusic-g2zumsvj","df023fe8eaec4bdabd6e92763defb756",MediaType.banner,Placement.bottom,DisplayMode.autoRotate,30,AnimationType.slide,listener,true);
                        //    mActivity.addContentView(adView,new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
                        //}
                        
						mAd.setVisibility(View.GONE);
    					//mAdsenseView.setVisibility(View.VISIBLE);
					}
                    break;  

                default:
                    break;
            }

        }
    };
*/    


    private static class QWAdEventsListener implements AdEventsListener {
    	private ViewGroup mAdsView = null;
        
    	public QWAdEventsListener(ViewGroup view) {
            mAdsView = view;
    	}
    	
    	public void onAdClick(Context ctx,Ad ad) {
    		Log.i("Snake","onAdClick for Ad: " + ad.getAdType() + " : " + ad.getId());
    		if(mAdsView!=null){
    			//mAdsView.setVisibility(View.GONE);
    		}
    		//mShowQuattroAd = false;
    	}

    	public void onAdRequest(Context ctx,AdRequestParams params) {
    		Log.i("Snake","onAdRequest for RequestParams: " + params.toString());
    	}

    	public void onAdRequestFailed(Context ctx,AdRequestParams params, Status status) {
    		Log.i("Snake","onAdRequestFailed for RequestParams: " + params.toString() + " : " + status);
    	}

    	public void onAdRequestSuccessful(Context ctx,AdRequestParams params, Ad ad) {
    		Log.i("Snake","onAdRequestSuccessful for RequestParams: " + params.toString() + " : Ad: " + ad.getAdType() + " : " + ad.getId());
    	}

    	public void onDisplayAd(Context ctx,Ad ad) {
    		Log.i("Snake","onDisplayAd for Ad: " + ad.getAdType() + " : " + ad.getId());
    	}
    }
    
}

