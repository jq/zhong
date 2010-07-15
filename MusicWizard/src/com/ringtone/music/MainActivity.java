package com.ringtone.music;

import com.admob.android.ads.AdManager;
import com.admob.android.ads.AdView;
import com.ringdroid.RingdroidSelectActivity;
import com.ringtone.music.R;
import com.ringtone.music.download.DownloadActivity;
import com.ringtone.music.updater.AppUpdater;
import com.ringtone.music.updater.UpdateInfo;
import com.qwapi.adclient.android.view.QWAdView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
	private SearchBar mSearch;
	
	private static Activity sActivity;
	private static boolean sFeedsAndUpdateChecked = false;
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sActivity = this;
        
        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(this));
        
        setContentView(R.layout.main); 
		Utils.addAds(this);

		if (!EulaActivity.checkEula(this)) {
			return;
		}
        
        mSearch = new SearchBar(this);
        
        TextView downloads = (TextView)findViewById(R.id.downloads);
        downloads.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, DownloadActivity.class);
				startActivity(intent);
			}
        });
        
        TextView viewMusic = (TextView)findViewById(R.id.music_library);
        viewMusic.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				ViewDownloadedActivity.listFiles();
				Intent intent = new Intent(MainActivity.this, ViewDownloadedActivity.class);
				startActivity(intent);
			}
        });
        
        TextView ringtone = (TextView)findViewById(R.id.ringtone);
        ringtone.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
	        	Intent i = new Intent(MainActivity.this, RingdroidSelectActivity.class);
	        	startActivity(i);
			}
        });
        
        checkFeedsAndUpdate();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    }
    
    
    @Override
    protected Dialog onCreateDialog(int id) {
      if (id == Feed.DOWNLOAD_APP_DIG) {
        return Feed.createDownloadDialog(this); 
      }
      return null;
    }    
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	sActivity = null;
    	Bookmark.addBookmark(this, getContentResolver());
    }
    
    private void checkFeedsAndUpdate() {
    	if (sFeedsAndUpdateChecked)
    		return;
    	
    	sFeedsAndUpdateChecked = true;
        if (!Feed.runFeed(this, R.raw.feed)) {
            // Check update only when feed is not shown. We don't want to annoy users too much.
        	checkUpdate();
        }
    }
    
    private void checkUpdate() {
    	Utils.D("Checking update");
    	new CheckUpdateTask().execute();
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.ringtone:
        	Intent i = new Intent(this, RingdroidSelectActivity.class);
        	this.startActivity(i);
        	break;
        case R.id.share_app:
        	String url = AppUpdater.getNewUpdateUrl(this);
        	if (TextUtils.isEmpty(url)) {
        		url = "market://search?q=pname:" + getPackageName();
        	}
        	StringBuilder sb = new StringBuilder();
        	final String prefix = "market://search?q=";
        	
        	if (url.startsWith(prefix)) {
        		sb.append("<html><body>");
        		sb.append("Hi,<br><br>I recently found a cool music search app on Android based smartphones and I strongly recommend it. Just search the following in Android Market:<br><br>");
        		sb.append(url.substring(prefix.length(), url.length()));
        		sb.append("<br><br>Cheers,<br><br>");
        		sb.append("</body></html>");
        	} else {
        		sb.append("<html><body>");
        		sb.append("Hi,<br><br>I recently found a cool music search app on Android based smartphones and I strongly recommend it. Just use the following link to download:<br><br>");
        		sb.append("<a href=\"" + url + "\">" + url + "</a>");
        		sb.append("<br><br>Cheers,<br><br>");
        		sb.append("</body></html>");
        	}
        	
        	Intent sendIntent = new Intent(Intent.ACTION_SEND);
        	sendIntent.setType("text/html");
        	sendIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(sb.toString()));
        	sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_email_subject));
        	sendIntent.setType("message/rfc822");
        	startActivity(Intent.createChooser(
        			sendIntent, "Choose an email application:"));
        	return true;
        	
        case R.id.about:
        	new AlertDialog.Builder(this)
        	.setTitle(R.string.about)
            .setIcon(R.drawable.ic_dialog_info)
        	.setMessage(VersionUtils.getApplicationName(this) + " v" +
        			VersionUtils.getVersionNumber(this)).setPositiveButton("Update",
        			new DialogInterface.OnClickListener() {
        		@Override
        		public void onClick(DialogInterface dialog,
        				int whichButton) {
        			String url = AppUpdater.getNewUpdateUrl(getApplication());
        			
        			if (url != null) {
        				// Start the new activity
        				try {
        					Uri uri = Uri.parse(url);
        					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				    		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        					startActivity(intent);
        				} catch (Exception ex) {
        					ex.printStackTrace();
        				}
        			} else {
        				Utils.Info(MainActivity.this, "You app is update to date");
        			}
        		}
        	}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        		@Override
        		public void onClick(DialogInterface dialog, int which) {
        		}
        	}).create().show();
        	return true;
        }
        
        return false;
    }
    
    private class CheckUpdateTask extends AsyncTask<Void, Void, UpdateInfo> {
		@Override
		protected UpdateInfo doInBackground(Void... params) {
			return AppUpdater.checkUpdate(getApplication());
		}
		
		@Override
		protected void onPostExecute(final UpdateInfo update) {
			if (update == null)
				return;
			
			if (sActivity == null)
				return;
			
			new AlertDialog.Builder(sActivity)
			.setIcon(R.drawable.alert_dialog_icon)
			.setTitle(R.string.updater_dialog_title)
			.setMessage(update.getMessage()).setPositiveButton("Download",
					new DialogInterface.OnClickListener() {
        		@Override
        		public void onClick(DialogInterface dialog,
        				int whichButton) {
			        // Start the new activity
			        try {
			        	Uri uri = Uri.parse(update.getUrl());
			        	Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			        	startActivity(intent);
			        } catch (Exception ex) {
			        	ex.printStackTrace();
			        }
        		}
        	}).setNegativeButton("Cancel",
        			new DialogInterface.OnClickListener() {
        		@Override
        		public void onClick(DialogInterface dialog,
        				int whichButton) {
        		}
        	}).create().show();
		}
    }
}
