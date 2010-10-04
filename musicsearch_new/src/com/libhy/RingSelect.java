package com.libhy;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import com.ringdroid.RingdroidSelectActivity;

public class RingSelect extends RingdroidSelectActivity {
	@Override
	public void onCreate(Bundle icicle) {
	    super.EDITOR = "com.libhy.RingEditor";
	    super.onCreate(icicle);
	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
	
	public static void startPureEditor(Activity activity, String filename) {
		Intent intent = new Intent(Intent.ACTION_EDIT, Uri.parse(filename));
		intent.putExtra("was_get_content_intent", false);
		intent.setClassName(activity ,EDITOR);
		activity.startActivityForResult(intent, REQUEST_CODE_EDIT);    
	}
}
