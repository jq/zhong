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
	private ProgressDialog dlProgress;
	private Context context;
	private Intent intent;
	private String title;
	private boolean isBackground;

	public DefaultDownloadListener(Context context, Intent intent,
			String title, boolean isBackground) {
		this.context = context;
		this.intent = intent;
		this.title = title;
		this.isBackground = isBackground;
	}

	public void onStart() {
		if (!isBackground) {
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
	}

	public void onDownloadFinish(File file, Uri u) {
		if (isBackground) {
			Util.addNotification(context, intent, title, Const.app_name,
					Const.notification_text_finish, Const.app_name,
					Const.notification_text_finish);
		} else {
			try {
				dlProgress.dismiss();
			} catch (Exception e) {
				// a simple catch and do nothing.
			}
		}
	}

	public void onDownloadProgress(int percentage) {
		if (!isBackground) {
			dlProgress.setProgress(percentage);
		}
	}

	public void onDownloadFail() {
		if (!isBackground) {
			dlProgress.dismiss();
		}
	}
	
	protected boolean isBackground() {
		return isBackground;
	}
}
