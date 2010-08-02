package com.happy.life;

import android.content.Context;

public class Constants {
	public static final String USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.3) Gecko/20100401 Firefox/3.6.3";
	public static final String QUERY = "QUERY";
    public static final String XML = "XML";
	public static final String BASE_DIR_NAME = "music_wizard";
	
	public static final String LAUNCH_ACTIVITY_PACKAGE = "launch_activity_package";
	public static final String LAUNCH_ACTIVITY_CLASS = "launch_activity_class";
	
	public static final String PREFS_UPDATE = "update";
	
	public static final String UPDATE_URL = "url";
	public static final String UPDATE_VERSION = "version";
	public static final String UPDATE_MESSAGE = "message";
	public static final String UPDATE_SEQ = "seq";
		
	public static final int MINI_SERVER_PORT = 9876;
	public static DbAdapter dbAdapter;
	// TODO: This may result in some leak since the db is never closed. Fix this.
	public static void init(Context c) {
	    dbAdapter = new DbAdapter(c);
	}
}
