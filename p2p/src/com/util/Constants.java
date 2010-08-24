package com.util;


import android.content.Context;

public class Constants {

	public static final String LAUNCH_ACTIVITY_PACKAGE = "launch_activity_package";
	public static final String LAUNCH_ACTIVITY_CLASS = "launch_activity_class";
	
	public static final String QUERY = "QUERY";
	public static final String XML = "XML";
	
    public static final int MINI_SERVER_PORT = 9876;

	
	public static DbAdapter dbAdapter;
    // TODO: This may result in some leak since the db is never closed. Fix this.
    public static void init(Context c) {
        dbAdapter = new DbAdapter(c);
    }
}
