package com.trans.music.search;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

public class TabCreator {
  
  public static void createTab(Activity act, TabHost tabHost, Intent intent,  String tabName, int resourceId) {
    if (Const.ver > 3) {
    	TabSDKCreator.createCustomerTab(act, tabHost, intent, tabName, resourceId);
    } else {
     tabHost.addTab(tabHost.newTabSpec(tabName).setIndicator(tabName,
         act.getResources().getDrawable(resourceId)).setContent(intent));
    }
 }
  
}
