package com.popczar.music;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.text.TextUtils;

public class NetUtils {
	
	private static final int CONNECT_TIMEOUT = 15000;  // 20s
	private static final int INITIAL_BUFFER_SIZE = 16000;  // 16K
	private static final int BUFFER_SIZE = 4096;
	private static String sCookie;

	public static String fetchHtmlPage(String link, String coding) throws IOException {
		URL url = new URL(link);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setRequestProperty("User-Agent", Constants.USER_AGENT);
		connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		connection.setRequestProperty("Accept-Language", "en-us,en;q=0.5");
		connection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		if (sCookie != null) {
			connection.setRequestProperty("Cookie", sCookie);
		}
		
		connection.setConnectTimeout(CONNECT_TIMEOUT);
		connection.connect();
		
		String cookie = connection.getHeaderField("Set-Cookie");
		if (!TextUtils.isEmpty(cookie)) {
			sCookie = cookie;
		}

		StringBuilder builder = new StringBuilder(INITIAL_BUFFER_SIZE);

		char[] buff = new char[BUFFER_SIZE];

		InputStreamReader is = new InputStreamReader(connection.getInputStream(), coding);

		int len;
		while ((len = is.read(buff, 0, buff.length)) > 0) {
			builder.append(buff, 0, len);
		}
		connection.disconnect();
		return builder.toString();
	}
}
