package com.util;

public class LOG {
  public static boolean isTraceEnabled() {
  	return false;
  }
  public static boolean isWarnEnabled() {
  	return false;
  }
  public static boolean isInfoEnabled() {
  	return false;
  }
  public static boolean isDebugEnabled() {
  	return false;
  }
  public static boolean isErrorEnabled() {
  	return true;
  }
  
  public static void logSp(String log) {
	  if (com.util.Utils.DEBUG)
		  android.util.Log.i(Utils.TAG, log);
  }
  public static void logSp(String log, Throwable t) {
	  if (com.util.Utils.DEBUG)
		  android.util.Log.i(Utils.TAG, log + " : " + t.getMessage());
  }
  
  public static void logxml(String log, Throwable t) {
	  if (com.util.Utils.DEBUG)
		  android.util.Log.i(Utils.TAG, log + " : " + t.getMessage());
  }
  
  public static void logxml(String log) {
	  if (com.util.Utils.DEBUG)
		  android.util.Log.i(Utils.TAG, log);
  }

  private static void log(String log) {
	  if (com.util.Utils.DEBUG)
		  android.util.Log.i(Utils.TAG, log);
  }
  
  public static void trace(String log) {
	  if (com.util.Utils.DEBUG)
		  android.util.Log.i(Utils.TAG, log);
  }
  
  public static void trace(String log, Throwable t) {
	  if (com.util.Utils.DEBUG)
		  android.util.Log.i(Utils.TAG, log + " : " + t.getMessage());
  }
  
  public static void info(String log) {
    log(log);
  }
  
  public static void info(String msg, Throwable t) {
	  logSp(msg, t);
  }
  
  public static void warn(String log) {
  	//System.out.println(log);
    log(log);
  }
  
  public static void warn(String log, Throwable t) {
  	//System.out.println(log);
    log(log + t.getMessage());
  }

  public static void debug(String log) {
  	////System.out.println(log);
    log(log);

  }
  public static void debug(String log, Throwable t) {
  	//System.out.println(log);
    log(log + t.getMessage());

  }
  public static void error(String log) {
  	//System.out.println(log);
    // log(log);
  }
  public static void error(String log, Throwable t) {
  	//System.out.println(log);
    log(log + t.getMessage());
  }
  
  public static void callStack() {
      StackTraceElement[] elements = Thread.currentThread().getStackTrace();

      for(int i=0; i<elements.length; i++) {
          log(elements[i].toString());
      }
  }
  public static void callStack(StackTraceElement[] elements) {
      for(int i=0; i<elements.length; i++) {
          log(elements[i].toString());
      }
  }
}
