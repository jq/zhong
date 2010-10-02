package com.ringtone.joke;

import java.net.URISyntaxException;

import com.feebe.rings.R;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

public class TabSDKCreator {
	  public static void createCustomerTab(Context c, TabHost tabHost, Intent intent,  String tabName, int resourceId) {
	      LayoutInflater factory = LayoutInflater.from(c);
	      View tabHeader = factory.inflate(R.layout.tabheader, null);
	      TextView textView = (TextView) tabHeader.findViewById(R.id.tab_label);
	      ImageView imageView = (ImageView) tabHeader.findViewById(R.id.tab_image);
	      textView.setText(tabName);
	      imageView.setBackgroundResource(resourceId);
	      tabHost.addTab(tabHost.newTabSpec(tabName).setIndicator(tabHeader).setContent(intent));
	  }
}
