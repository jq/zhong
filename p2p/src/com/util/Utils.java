package com.util;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Enumeration;

import com.limegroup.gnutella.Downloader;

import android.util.Log;

public class Utils {  
	public static String TAG = "pp";
	
    /**
     * The number of bytes in a kilobyte.
     */
    public static final long ONE_KB = 1024;

    /**
     * The number of bytes in a megabyte.
     */
    public static final long ONE_MB = ONE_KB * ONE_KB;

    /**
     * The number of bytes in a gigabyte.
     */
    public static final long ONE_GB = ONE_KB * ONE_MB;
    
	static public final boolean DEBUG = false;
    static public void D(String msg) {
      if (DEBUG) {
          Log.d(TAG, msg);
      }
    }
    static public void E(String msg) {
          Log.e(TAG, msg);
    }    
    static public void assertD(boolean b) {
        if (DEBUG)
            assert b;
    }
    public static InetAddress getLocalIpAddress() throws UnknownHostException {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress;
                    }
                }
            }
        } catch (SocketException ex) {
            LOG.error(ex.toString());
        }
        
        return InetAddress.getLocalHost();
    }

    private static CharsetEncoder sEncoder = Charset.forName("ISO-8859-1").newEncoder();
    private static CharsetDecoder sDecoder = Charset.forName("GBK").newDecoder();
    public static String convertGBK(String input) {
    	try {
    		ByteBuffer bbuf = sEncoder.encode(CharBuffer.wrap(input));
    		CharBuffer cbuf = sDecoder.decode(bbuf);
    		String output = cbuf.toString();
    		return output;
    	} catch (Exception e) {
    		//e.printStackTrace();
    		return input;
    	}
    }
    
    public static String displaySizeInMB(long size) {
      return String.format("%.2fM", size * 1.0 / ONE_MB);
    }
}
