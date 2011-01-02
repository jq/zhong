package com.cinla.ringtone;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
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
	
	public static Bitmap getBitmapFromUrl(String url) {
		Bitmap bitmap = null;
		
		return bitmap;
	}
	
    // TODO: we could probably improve performance by re-using connections instead of closing them
	// after each and every download
	public static Bitmap downloadImage(String imageUrl) {

		try {
			byte[] imageData = retrieveImageData(imageUrl);

			return BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

		} catch (Throwable e) {
			e.printStackTrace();
			SystemClock.sleep(DEFAULT_RETRY_HANDLER_SLEEP_TIME);
		}

		return null;
	}

    private static byte[] retrieveImageData(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // determine the image size and allocate a buffer
        int fileSize = connection.getContentLength();
        byte[] imageData = new byte[fileSize];

        // download the file
        BufferedInputStream istream = new BufferedInputStream(connection.getInputStream());
        int bytesRead = 0;
        int offset = 0;
        while (bytesRead != -1 && offset < fileSize) {
            bytesRead = istream.read(imageData, offset, fileSize - offset);
            offset += bytesRead;
        }

        // clean up
        istream.close();
        connection.disconnect();

        return imageData;
    }
    
    
    private static File downloadFile(String filePath) {
    	File file = new File(filePath);
    	
    	return file;
    }

}
