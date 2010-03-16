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
      Util.addNotification(context, intent, title, Const.app_name, Const.notification_text_finish, Const.app_name, Const.notification_text_finish);
      dlProgress.dismiss();
    }
    
    public void onDownloadProgress(int percentage){
    	dlProgress.setProgress(percentage);
    }
    
    public void onDownloadFail() {
    	Util.addNotification(context, intent, title, Const.app_name, Const.notification_text_failed, Const.app_name, Const.notification_text_failed);
    	dlProgress.dismiss();
    }
}
