package com.fatima.life.updater;

import org.json.JSONException;
import org.json.JSONObject;

import com.fatima.life.Constants;

import android.text.TextUtils;

public class UpdateInfo {
	private String mUrl;
	private String mVersion;
	private String mMessage;
	private int mSeq;
	
	// Construct from json string.
	// An example:
	// {"url": "http://test-url", "message": "test message", "version": "1.0", "seq": 0}
	public UpdateInfo(String json) throws JSONException {
		if (TextUtils.isEmpty(json)) {
			throw new IllegalArgumentException("Update Json string can't be empty");
		}
		
		JSONObject update = new JSONObject(json);
		mUrl = update.getString(Constants.UPDATE_URL);
		mVersion = update.getString(Constants.UPDATE_VERSION);
		mMessage = update.getString(Constants.UPDATE_MESSAGE);
		mSeq = update.getInt(Constants.UPDATE_SEQ);
	}
	
	public String getUrl() {
		return mUrl;
	}
	
	public String getVersion() {
		return mVersion;
	}
	
	public String getMessage() {
		return mMessage;
	}
	
	public int getSeq() {
		return mSeq;
	}
}
