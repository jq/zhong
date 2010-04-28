package com.popczar.music;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import android.text.TextUtils;

public class NetUtils {
	
	private static final int CONNECT_TIMEOUT = 15000;  // 20s
	private static final int INITIAL_BUFFER_SIZE = 16000;  // 16K
	private static final int BUFFER_SIZE = 4096;
	private static String sCookie;

	public static String fetchHtmlPage(String link, String coding) throws IOException {
		URL url = new URL(link);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		//connection.setRequestProperty("Accept-Encoding", "gzip");
		connection.setRequestProperty("Accept", "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
		connection.setRequestProperty("User-Agent",
				"Mozilla/5.0 (Linux; U; Android 1.6; en-us; sdk Build/Donut) AppleWebKit/528.5+ (KHTML, like Gecko) " +
				"Version/3.1.2 Mobile Safari/525.20.1");
		connection.setRequestProperty("Accept-Language", "en-us");
		connection.setRequestProperty("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7");
		if (sCookie != null) {
			connection.setRequestProperty("Cookie", sCookie);
		}
		
		connection.setConnectTimeout(CONNECT_TIMEOUT);
		connection.connect();
		
		if (Utils.DEBUG) {
			Utils.D("Reply headers:");
			Map replyHeaders = connection.getHeaderFields();
			Iterator it = replyHeaders.entrySet().iterator();
			Map.Entry pairs = (Map.Entry)it.next();
			Utils.D(pairs.getKey() + " = " + pairs.getValue());
			Utils.D("End reply headers");
		}
		
		String cookie = connection.getHeaderField("Set-Cookie");
		if (!TextUtils.isEmpty(cookie)) {
			sCookie = cookie;
		}

		StringBuilder builder = new StringBuilder(INITIAL_BUFFER_SIZE);

		// char[] buff = new char[BUFFER_SIZE];

		InputStreamReader is = coding != null ? new InputStreamReader(connection.getInputStream(), coding) :
			new InputStreamReader(connection.getInputStream());
		
		BufferedReader reader = new BufferedReader(is);

		/*
		int len;
		while ((len = reader.read(buff, 0, buff.length)) > 0) {
			builder.append(buff, 0, len);
		}
		connection.disconnect();
		return builder.toString();
		*/
		String line = null;
		while ((line = reader.readLine()) != null) {
			builder.append(line + "\n");
		}
		return builder.toString();
	}
}
