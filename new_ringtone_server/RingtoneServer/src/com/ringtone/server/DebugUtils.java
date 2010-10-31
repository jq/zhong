package com.ringtone.server;

public class DebugUtils {
	public static final boolean debug = true;
	
	public static void D(String msg) {
		if (debug) {
			System.out.println(msg);
		}
	}
	
}
