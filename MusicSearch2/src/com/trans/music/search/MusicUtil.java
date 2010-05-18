package com.trans.music.search;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.Log;

public class MusicUtil {
  
  //public static final String //SogouSearchBase = "http://mp3.sogou.com/music.so?pf=mp3&as=&st=&ac=1&w=02009900&query=";
  //http://mp3.sogou.com/music.so?pf=&as=&st=&ac=1&w=02009900&query=
  public static final String  SogouSearchBase = "http://mp3.sogou.com/music.so?pf=mp3&query=";
	//private static final String URL_SEARCH_PROXY = "http://feebe.appspot.com/msearch/music.so?pf=mp3&query=";

  public static String getSogouLinks(String key) {
		String reqString = null;
		try {
		  reqString = URLEncoder.encode(key, "GB2312");
		} catch (UnsupportedEncodingException e) {
		  reqString = URLEncoder.encode(key);
		} finally {
		  //Log.e("search url:", SogouSearchBase + reqString);
		  return SogouSearchBase + reqString;
		}
  }
	private static boolean sUseProxy = false;

  public static String getSogouLinks(String url, int page) {
    return url + "&page=" + page;
  }
	private static String sCookie;
  public static SharedPreferences mSetting;

public static void setStingKey(String key, String value) {
    Editor e = mSetting.edit();
    e.putString(key, value);
    e.commit();
}

  private static void setConnectionString(HttpURLConnection urlConn) {
		urlConn.setRequestProperty("User-Agent",
		"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.6; en-US; rv:1.9.0.8) Gecko/2009032608 Firefox/3.0.8 GTB6 ");
		urlConn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		urlConn.setRequestProperty("Accept-Language", "en-us,en;q=0.5");
		//Accept-Encoding: gzip,deflate
		urlConn.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		urlConn.setRequestProperty("Keep-Alive", "300");
		urlConn.setRequestProperty("Connection", "keep-alive");
  	if (sCookie == null) {
  		sCookie = mSetting.getString("cookie", null);
  	}
  	if (sCookie != null) {
  		urlConn.setRequestProperty("Cookie", sCookie);
  	}
  }

 /* 
  public static ArrayList<MP3Info> getSogoMp3(String urlStr, int limit) {
  	ArrayList<MP3Info> list = getSogoMp3Once(urlStr, limit);
  	
  	if ((list == null ||list.size() == 0) && !urlStr.contains("&page=") && !sUseProxy) {
  		//Log.e("failed to load ", "try again");
  		urlStr = urlStr.replace("mp3.sogou.com", "feebe.appspot.com/msearch");
  		sUseProxy = true;
  		list = getSogoMp3Once(urlStr, limit);
  	} else {
  		//Log.e("load ", "no problem");
  	}
  	return list;
  }
  */
  
	public static ArrayList<MP3Info> getSogoMp3(String urlStr, int limit) {
		ArrayList<MP3Info> songs = new ArrayList<MP3Info>();
		String httpresponse = null;
		try {
			Log.e("query url:", urlStr);
			URL url = new URL(urlStr);
			HttpURLConnection urlConn = (HttpURLConnection) url
			.openConnection();
			setConnectionString(urlConn);
			urlConn.setConnectTimeout(20000);
			urlConn.connect();

			String cookie = urlConn.getHeaderField("Set-Cookie");
			if (!TextUtils.isEmpty(cookie)) {
				sCookie = cookie;
				setStingKey("cookie", cookie);
			}
			InputStream stream = urlConn.getInputStream();

			StringBuilder builder = new StringBuilder(8 * 1024);

			char[] buff = new char[4096];

			InputStreamReader is = new InputStreamReader(stream, "gb2312");

			int len;
			while ((len = is.read(buff, 0, 4096)) > 0) {
				builder.append(buff, 0, len);
			}
			urlConn.disconnect();
			httpresponse = builder.toString();

			Pattern PATTERN_ROW = Pattern.compile("<tr(.*?)</tr>", Pattern.DOTALL);
			Pattern PATTERN = Pattern.compile(
			"<td.*?\\btitle=\"([^\"]*)\".*?" +   // 1
			"<td.*?\\bsinger=\"([^\"]*)\".*?" +   // 2
			"<td.*?\\btitle=\"([^\"]*)\".*?" +   // 3
			"<td.*?</td>.*?" +  // Ignore
			"<td.*?</td>.*?" +  // Ignore
			"<td.*?\'(/down.so.*?)\'.*?" +  // 6
			"<td.*?href=\"([^\"]*)\".*?" +  // 7
			"<td.*?</td>.*?" +  // Ignore
			"<td.*?>([^<]*)<.*?" +   // 9
			"<td.*?>([^<]*)<" +   // 10
			""
			, Pattern.DOTALL);
			
			
			Matcher matcherRow = PATTERN_ROW.matcher(httpresponse);
			while (matcherRow.find()) {
				Matcher m = PATTERN.matcher(matcherRow.group(1));
				while (m.find()) {
					MP3Info info = new MP3Info();
					info.setName(m.group(1).trim());
					info.setArtist(URLDecoder.decode(m.group(2), "gb2312").trim());
					info.setAlbum(m.group(3).trim());
					info.setLink(m.group(4).trim());
					info.setFSize(String.valueOf(sizeFromStr(m.group(6).trim())));
					
					songs.add(info);
				}
			}
		} catch (IOException e) {
			// ShowToastMessage("Network can not connect, please try again.");
			Log.e("getSogoMp3Once", e.getMessage());
			return null;
		}


		if (songs != null) {
			//Log.e(TAG, "song size: " + songs.size());
		}
		return songs;
	}
  public static long sizeFromStr(String sizeStr) {
  	if (TextUtils.isEmpty(sizeStr)) {
  		return 0;
  	} else {
  		if (sizeStr.endsWith("K") ||
  			sizeStr.endsWith("k")) {
  			return (long) (1024 * Float.valueOf(sizeStr.substring(0, sizeStr.length() - 1)));
  		} else if (sizeStr.endsWith("M") ||
  			sizeStr.endsWith("m")) {
  			return (long) (1024 * 1024 * Float.valueOf(sizeStr.substring(0, sizeStr.length() - 1)));
  		} else {
  			try {
      			return Long.valueOf(sizeStr);
  			} catch (java.lang.NumberFormatException e) {
  				e.printStackTrace();
  				return 0;
  			}
  		}
  	}
  }

	// Given the link to each individual mp3, get the corresponding link by which we can actually download the music.
	public static String getLink(String req)  throws IOException{
			String request = "http://mp3.sogou.com" +req;
			URL url = new URL(request);
			HttpURLConnection urlConn = (HttpURLConnection)url.openConnection();
			setConnectionString(urlConn);
			urlConn.setConnectTimeout(20000);
			urlConn.connect();

			InputStream stream = urlConn.getInputStream();

			StringBuilder builder = new StringBuilder(8 * 1024);

			char[] buff = new char[4096];
			//必须在此指定编码，否则后面toString会导致乱码
			InputStreamReader is = new InputStreamReader(stream);
			
			int len;
			while ((len = is.read(buff, 0, 4096)) > 0) {
				builder.append(buff, 0, len);
			}
			urlConn.disconnect();
			String httpresponse = builder.toString();
			int linkStartPos = httpresponse.indexOf("\" href=\"") + "\" href=\"".length();

			int linkEndPos = httpresponse.indexOf('>', linkStartPos)-1;
			return httpresponse.substring(linkStartPos, linkEndPos);
	}
/*
	public static String getLink(String request) throws IOException {
		request = "http://mp3.sogou.com" + request;
    	URL url = new URL(request);
    	HttpURLConnection urlConn = (HttpURLConnection)url.openConnection();
    	urlConn.setRequestProperty("User-Agent", "Apache-HttpClient/UNAVAILABLE (java 1.4)");
    	urlConn.setConnectTimeout(12000);
    	urlConn.connect();
    	
    	InputStream stream = urlConn.getInputStream();
		
    	StringBuilder builder = new StringBuilder(8*1024);
		
    	char[] buff = new char[4096];
    	InputStreamReader is = new InputStreamReader(stream,"gb2312");
		
		int len;
		while ((len = is.read(buff, 0, 4096)) > 0) {
			builder.append(buff, 0, len);
		}
		urlConn.disconnect();
		String httpresponse = builder.toString();
		 int linkStartPos = httpresponse.indexOf("下载歌曲\" href=\"")+"下载歌曲\" href=\"".length();
		 int linkEndPos = httpresponse.indexOf('>', linkStartPos)-1;
		return httpresponse.substring(linkStartPos, linkEndPos);
	}
*/
}
