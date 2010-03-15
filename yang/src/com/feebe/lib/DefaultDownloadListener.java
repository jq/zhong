package com.feebe.lib;

import java.io.File;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class DefaultDownloadListener {
	ProgressDialog dlProgress;
	Context context;
	Intent intent;
	String title;
	
	public DefaultDownloadListener(Context context, Intent intent, String title) {
		this.context = context;
		this.intent = intent;
		this.title = title;
	}
	
	public void onStart() {
		dlProgress = new ProgressDialog(context);
      	dlProgress.setTitle(Const.dlprogress_title);
      	dlProgress.setMessage(context.getString(Const.dlprogress_message));
      	dlProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      	dlProgress.setIndeterminate(false);
      	dlProgress.setMax(100);
      	dlProgress.setProgress(0);
      	dlProgress.setCancelable(false);
      	dlProgress.show();
	}
	
    public void onDownloadFinish(File file, Uri u) {
  	  int icon = Const.icon;
	  String tickerText = "\""+title+"\""+ context.getString(Const.notification_text_finish);
	  long when = System.currentTimeMillis();
	  Notification notification = new Notification(icon, tickerText, when);
	  Context context = this.context.getApplicationContext();
	  String expandedText = "\""+title+"\""+ context.getString(Const.notification_text_finish);
	  String expandedTitle = context.getString(Const.notification_title);
	  //Intent intent = new Intent(RingActivity.this, RingdroidSelectActivity.class);
	  PendingIntent launchIntent = PendingIntent.getActivity(context, 0, intent, 0);
      notification.setLatestEventInfo(context, expandedTitle, expandedText, launchIntent);
      notification.flags |= Notification.FLAG_AUTO_CANCEL;
      NotificationManager notificationManager;
      notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
      int notificationRef = 1;
      notificationManager.notify(notificationRef++, notification);
      dlProgress.dismiss();
    }
    
    public void onDownloadProgress(int percentage){
    	dlProgress.setProgress(percentage);
    }
    
    public void onDownloadFail() {
    	int icon = Const.icon;
    	String tickerText ="\""+title+"\""+  context.getString(Const.notification_text_failed);
    	long when = System.currentTimeMillis();
    	Notification notification = new Notification(icon, tickerText, when);
    	Context context = this.context.getApplicationContext();
    	String expandedText ="\""+title+"\""+  context.getString(Const.notification_text_failed);
    	String expandedTitle = context.getString(Const.notification_title);
    	//Intent intent = new Intent(RingActivity.this, RingdroidSelectActivity.class);
    	PendingIntent launchIntent = PendingIntent.getActivity(context, 0, intent, 0);
        notification.setLatestEventInfo(context, expandedTitle, expandedText, launchIntent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        NotificationManager notificationManager;
        notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationRef = 1;
        notificationManager.notify(notificationRef++, notification);
    	dlProgress.dismiss();
    }
}
