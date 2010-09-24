package com.lib;

import java.io.File;

import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import com.feebe.lib.AdListener;
import com.feebe.lib.Const;
import com.feebe.lib.Util;
import com.feebe.rings.Search;
import com.ringdroid.R;
import com.ringdroid.RingdroidSelectActivity;

public class RingSelect extends RingdroidSelectActivity {
  @Override
  public void onCreate(Bundle icicle) {
    RingdroidSelectActivity.EDITOR = "com.lib.RingEditor";
    super.onCreate(icicle);
    AdListener.createAds(this, R.id.mediaselect);
  }
  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    AdListener.createAds(this, R.id.mediaselect);
  }
  protected void startRingdroidEditor() {
    Cursor c = mAdapter.getCursor();
    int dataIndex = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
    String filename = c.getString(dataIndex);
    try {
      // // Log.e("file", filename);
      if (filename.startsWith(Const.contentDir)) {
        String jsonFile = Const.jsondir +filename.substring(Const.contentDir.length());
        File file = new File(jsonFile);
        if (file.exists()) {
          Search.startRing(this, jsonFile);
          return;
        }
      } 
    } catch (Exception e) {
    }
    startPureEditor(filename);
  }

  private void startPureEditor(String filename) {
    Intent intent = new Intent(Intent.ACTION_EDIT,
        Uri.parse(filename));
    intent.putExtra("was_get_content_intent",
        mWasGetContentIntent);
    intent.setClassName(
        this,EDITOR);
    startActivityForResult(intent, REQUEST_CODE_EDIT);
    
  }

}
