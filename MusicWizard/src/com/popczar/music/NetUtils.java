package com.popczar.music;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetUtils {
	
	private static final int CONNECT_TIMEOUT = 20000;  // 20s
	private static final int INITIAL_BUFFER_SIZE = 16000;  // 16K
	private static final int BUFFER_SIZE = 4096;

	public static String fetchHtmlPage(String link, String coding) throws IOException {
		URL url = new URL(link);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setRequestProperty("User-Agent", Constants.USER_AGENT);
		connection.setConnectTimeout(CONNECT_TIMEOUT);
		connection.connect();

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
