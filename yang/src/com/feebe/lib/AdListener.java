package com.feebe.lib;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
 
import com.adwhirl.AdWhirlLayout;
import com.feebe.rings.R;

public class AdListener {
	public static void createAds(Activity activity) {
        AdWhirlLayout adWhirlLayout = new AdWhirlLayout(activity, "9e817eff582a444cbb34c339e2523693");
        RelativeLayout.LayoutParams adWhirlLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        ViewGroup layout = (ViewGroup) activity.findViewById(R.id.ads_view);
        layout.addView(adWhirlLayout, adWhirlLayoutParams);
	}
	public static void createAds(Activity activity, int id) {
        AdWhirlLayout adWhirlLayout = new AdWhirlLayout(activity, "9e817eff582a444cbb34c339e2523693");
        RelativeLayout.LayoutParams adWhirlLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        ViewGroup layout = (ViewGroup) activity.findViewById(id);
        layout.addView(adWhirlLayout, adWhirlLayoutParams);
	}
}