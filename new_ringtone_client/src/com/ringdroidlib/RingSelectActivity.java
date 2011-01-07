package com.ringdroidlib;

import java.io.File;
import java.io.IOException;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import com.cinla.ringtone.Constant;
import com.cinla.ringtone.MusicInfo;
import com.cinla.ringtone.MusicPageActivity;
import com.cinla.ringtone.Utils;
import com.ringdroid.RingdroidSelectActivity;

public class RingSelectActivity extends RingdroidSelectActivity {
	
	  @Override
	  public void onCreate(Bundle icicle) {
		  RingdroidSelectActivity.EDITOR = "com.ringdroidlib.RingEditorActivity";
		  super.onCreate(icicle);
	  }

	  protected void startRingdroidEditor() {
		Utils.D("in override SelectRingdroidEditor()");
	    Cursor c = mAdapter.getCursor();
	    int dataIndex = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
	    String filename = c.getString(dataIndex);
	    String objFilePath = null;
	    try {
			objFilePath = Utils.musicPathToObjPath(filename);
			if (objFilePath != null) {
				MusicInfo musicInfo = Utils.readMusicInfoFromFile(objFilePath);
				File testMusicFile = new File(musicInfo.getFilePath());
				if (!testMusicFile.exists()) {
					throw new IOException();
				}
				MusicPageActivity.startMusicPageActivity(RingSelectActivity.this, musicInfo);
				return;
			}
		} catch (Exception e) {
		}
	    
//	    try {
//	    	Utils.D("fileName: "+filename);
//	    	if (filename.startsWith(Constant.sMusicDir)) {
//	    	MusicInfo musicInfo = Utils.readMusicInfoFromFile(path)
//	    	File file = new File(jsonFile);
//	        if (file.exists()) {
//	          Search.startRing(this, jsonFile);
//	          return;
//	        }
//	      } 
//	    } catch (Exception e) {
//	    }
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
