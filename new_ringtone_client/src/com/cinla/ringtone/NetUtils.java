package com.cinla.ringtone;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

public class NetUtils {

	private static final int CONNECT_TIMEOUT = 10000;  // 10s
	private static final int INITIAL_BUFFER_SIZE = 16000;  // 16K
	private static final int DEFAULT_RETRY_HANDLER_SLEEP_TIME = 1000;

	public static String fetchHtmlPage(String link, String coding) throws IOException {
		URL url = new URL(link);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();

		//connection.setRequestProperty("Accept", "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
		//connection.setRequestProperty("User-Agent",
		//		"Mozilla/5.0 (Linux; U; Android 1.6; en-us; sdk Build/Donut) AppleWebKit/528.5+ (KHTML, like Gecko) " +
		//		"Version/3.1.2 Mobile Safari/525.20.1");
		//connection.setRequestProperty("Accept-Language", "en-us");
		//connection.setRequestProperty("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7");

		/*
		connection.setRequestProperty("User-Agent",
									  "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.3) Gecko/20100401 Firefox/3.6.3");
		*/


		//connection.setRequestProperty("User-Agent",
		//							  "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.3) Gecko/20100401 Firefox/3.6.3");
		//connection.setRequestProperty("Accept", "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
		//connection.setRequestProperty("Accept-Language", "en-us");
		//connection.setRequestProperty("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7");
		//connection.setRequestProperty("Keep-Alive", "300");
		//connection.setRequestProperty("Connection", "keep-alive");
		
		//if (id != -1) {
		//	if (sCookie.get(id) != null) {
		//		connection.setRequestProperty("Cookie", sCookie.get(id));
		//	}
		//}
		
		
		connection.setConnectTimeout(CONNECT_TIMEOUT);
		connection.connect();
		
//		if (Utils.DEBUG) {
//			Utils.D("Reply headers:");
//			Map replyHeaders = connection.getHeaderFields();
//			Iterator it = replyHeaders.entrySet().iterator();
//			Map.Entry pairs = (Map.Entry)it.next();
//			Utils.D(pairs.getKey() + " = " + pairs.getValue());
//			Utils.D("End reply headers");
//		}
		
//		String cookie = connection.getHeaderField("Set-Cookie");
//		
//		if (id != -1) {
//			if (!TextUtils.isEmpty(cookie)) {
//				sCookie.put(id, cookie);
//			}
//		}
		
		StringBuilder builder = new StringBuilder(INITIAL_BUFFER_SIZE);

		InputStreamReader is = coding != null ? new InputStreamReader(connection.getInputStream(), coding) :
			new InputStreamReader(connection.getInputStream());
		
		BufferedReader reader = new BufferedReader(is);

		String line = null;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
		}
		return builder.toString();
	}
	
	// with cache
	public static String fetchHtmlPage(String link, String coding, long expire) throws IOException {
		String response = null;
		response = readStringFromCache(link.trim(), expire);
		if (response != null) {
			return response;
		}
		response = fetchHtmlPage(link, coding);
		if (response!=null && response.length()>Constant.MIN_RESPONSE_LENGTH) {
			cacheStringInThread(link.trim(), response);
		}
		return response;
	}

	
	public static Bitmap getBitmapFromUrl(String url) {
		Bitmap bitmap = null;
		
		return bitmap;
	}
	
    private static File downloadFile(String filePath) {
    	File file = new File(filePath);
    	
    	return file;
    }
    
    private static void cacheStringInThread(final String url, final String content) {
    	new Thread(new Runnable() {
			@Override
			public void run() {
				String fileName = getFileNameFromUrl(url);
		    	String filePath = Constant.sCacheDir + fileName;
		    	FileOutputStream file = null;
		    	try {
		    		file = new FileOutputStream(filePath);
		    		file.write(content.getBytes());
		    		file.close();
		    	} catch (Exception e) {
		    		Utils.D("error in save cache");
		    	} 
			}
		}).start();
    	
    }
    
    public static String readStringFromCache(String url, long expire) {
    	String fileName = getFileNameFromUrl(url);
    	String filePath = Constant.sCacheDir + fileName;
    	File file = new File(filePath);

    	if (!file.exists()) {
    		return null;
    	}
    	if (System.currentTimeMillis()-file.lastModified() > expire) {
    		file.delete();
    		return null;
    	}
		try {
			InputStreamReader f = new InputStreamReader(new FileInputStream(file));
			StringBuilder builder = new StringBuilder(4096);
			char[] buff = new char[4096];
			int len;
			while ((len = f.read(buff)) > 0) {
				builder.append(buff, 0, len);
			}
			f.close();
			return builder.toString();
		} catch (Exception e) {
			Utils.D("error read string from cache");
		}
		return null;
    }
    
    public static void cacheImageInThread(final String url, final byte[] imageData) {
    	new Thread(new Runnable() {
			@Override
			public void run() {
		    	String fileName = getFileNameFromUrl(url);
		    	String filePath = Constant.sCacheDir + fileName;
		    	FileOutputStream file = null;
		    	try {
		    		file = new FileOutputStream(filePath);
		    		file.write(imageData);
		    		file.close();
		    	} catch (Exception e) {
		    		Utils.D("error in cache image");
		    	}
			}
		}).start();
    }
    
    public static byte[] readImageData(String url, long expire) {
    	String fileName = getFileNameFromUrl(url);
    	String filePath = Constant.sCacheDir + fileName;
    	File file = new File(filePath);
    	if (!file.exists()) {
    		return null;
    	}
    	if (System.currentTimeMillis()-file.lastModified() > expire) {
    		file.delete();
    		return null;
    	}
    	long fileSize = file.length();
    	byte[] imageData = new byte[(int)fileSize];
		try {
			BufferedInputStream istream = new BufferedInputStream(new FileInputStream(file));
			istream.read(imageData);
			istream.close();
			return imageData;
		} catch (Exception e) {
			Utils.D("error read string from cache");
		}
		return null;
    }

    public static boolean isInCache(String url) {
    	String fileName = getFileNameFromUrl(url);
    	String filePath = Constant.sCacheDir + fileName;
    	File file = new File(filePath);
    	if (!file.exists()) {
    		return false;
    	}
    	return true;
    }
    
    public static String getFileNameFromUrl(String url) {
        // replace all special URI characters with a single + symbol
        return url.replaceAll("[.:/,%?&=]", "+").replaceAll("[+]+", "+");
    }
    
}
