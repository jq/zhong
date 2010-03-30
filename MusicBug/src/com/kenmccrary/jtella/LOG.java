package com.kenmccrary.jtella;

public class LOG {
  public static void error(String msg) {
  	//android.util.Log.e("log", msg);
  }
  public static void e(String msg) {
  	android.util.Log.w("log", msg);
  }
  public static void debug(String msg) {
  	android.util.Log.d("log", msg);
  }
  public static void info(String msg) {
  	android.util.Log.i("log", msg);
  }

}
