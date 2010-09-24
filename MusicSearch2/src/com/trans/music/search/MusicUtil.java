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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MusicUtil {
	static Pattern PATTERN_ROW = Pattern.compile("<tr(.*?)</tr>", Pattern.DOTALL);
	static Pattern PATTERN = Pattern.compile(
			"<td.*?\\btitle=\"([^\"]*)\".*?" +   // 1
			"<td.*?\\bsinger=\"([^\"]*)\".*?" +   // 2
			"<td.*?\\btitle=\"([^\"]*)\".*?" +   // 3
			"<td.*?</td>.*?" +  // Ignore
			"<td.*?\'(/down.so.*?)\'.*?" +  // 4
			// TODO(zyu): In some cases, lyrics are empty. Temporily ignore lyrics.
//			"<td.*?href=\"([^\"]*)\".*?" +  // 5
			"<td>(.*?)</td>.*?" +  // 5
			"<td.*?</td>.*?" +  // Ignore
			"<td.*?>([^<]*)<.*?" +   // 6
			"<td.*?>([^<]*)<.*?" +   // 7
			"<td.*?spdbar([0-9]?)\".*?"
			, Pattern.DOTALL);
	
	private static final Pattern PATTERN_DOWNLOAD_URL = Pattern.compile("href=\"([^\"]*)\"");
	private static final String DOWNLOAD_MARKER = "下载歌曲";
  
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
	if (mSetting == null) {
		return;
	}
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
  		if (mSetting != null) {
  			sCookie = mSetting.getString("cookie", null);
  		}
  	}
  	if (sCookie != null) {
  		urlConn.setRequestProperty("Cookie", sCookie);
  	}
  }

//Used to signal between threads.
  static class Signal {
      public boolean ready;
  };
  
  static class HtmlData {
      public String content;
  };
  
  static class MyJavaScriptInterface {  
      Signal mSignal;
      HtmlData mData;
      public MyJavaScriptInterface(Signal s, HtmlData data) {
          this.mSignal = s;
          this.mData = data;
      }
      
      @SuppressWarnings("unused")  
      public void parseHtml(String html) {  
          mData.content = html;
          mSignal.ready = true;
          synchronized(mSignal) {
              mSignal.notify();
          }
      }  
  }  
  
  private static class FetchSearchPage extends WebViewClient {
      @Override
      public void onPageFinished(WebView view, String url) {
           view.loadUrl("javascript:window.HTMLOUT.parseHtml(document.getElementsByTagName('html')[0].innerHTML);"); 
      }
  }
  
  private static boolean loadUrl(final Context context, final String url, final HtmlData data) {
      final Signal s = new Signal();
      s.ready = false;
      Const.sHandler.post(new Runnable() {
          @Override
          public void run() {
              WebView web = new WebView(context);
              web.getSettings().setJavaScriptEnabled(true);
              web.getSettings().setLoadsImagesAutomatically(false);
              web.getSettings().setBlockNetworkImage(true);
              web.addJavascriptInterface(new MyJavaScriptInterface(s, data), "HTMLOUT");
              web.setWebViewClient(new FetchSearchPage());
              web.loadUrl(url);
          }
      });
      
      synchronized(s) {
          while (!s.ready) {
              try {
                  s.wait();
              } catch (InterruptedException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                  return false;
              }
          }
          
          return true;
      }
  }
  
	public static ArrayList<MP3Info> getSogoMp3(Context context, String urlStr, int limit) {
		ArrayList<MP3Info> songs = new ArrayList<MP3Info>();
		HtmlData data = new HtmlData();
		if (!loadUrl(context, urlStr, data))
          return null;
		if (TextUtils.isEmpty(data.content))                                                                                                                                                                    
          return null;
		
		try {
			Matcher matcherRow = PATTERN_ROW.matcher(data.content);
			while (matcherRow.find()) {
				Matcher m = PATTERN.matcher(matcherRow.group(1));
				while (m.find()) {
					MP3Info info = new MP3Info();
					info.setName(m.group(1).trim());
					info.setArtist(URLDecoder.decode(m.group(2), "gb2312").trim());
					info.setAlbum(m.group(3).trim());
					info.addLink(m.group(4).trim());
					info.setFSize(String.valueOf(sizeFromStr(m.group(6).trim())));
					info.setSpeed(m.group(8).trim());
					
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
  public static int sizeInM(String sizeStr) {
    if (TextUtils.isEmpty(sizeStr)) {
      return 0;
    }
    int size = 0;
    try {
      size = Integer.parseInt(sizeStr) / (1024 * 1024);
    } catch (NumberFormatException e) {
      return 0;
    }
    return size;
  }

	// Given the link to each individual mp3, get the corresponding link by which we can actually download the music.
	public static String getLink(Context context, String req)  throws IOException{
			String request = "http://mp3.sogou.com" +req;
			Log.e("get download link: ", request);
//			URL url = new URL(request);
			
			HtmlData data = new HtmlData();
			if (!loadUrl(context, request, data))
	          return null;
	        if (TextUtils.isEmpty(data.content))                                                                                                                                                                    
	          return null;
	        int start = data.content.indexOf(DOWNLOAD_MARKER) + DOWNLOAD_MARKER.length();
	        Matcher m = PATTERN_DOWNLOAD_URL.matcher(data.content.substring(start));
	        if (m.find()) {
              return m.group(1);
            }
//			HttpURLConnection urlConn = (HttpURLConnection)url.openConnection();
//			setConnectionString(urlConn);
//			urlConn.setConnectTimeout(20000);
//			urlConn.connect();
//
//			InputStream stream = urlConn.getInputStream();
//
//			StringBuilder builder = new StringBuilder(8 * 1024);
//
//			char[] buff = new char[4096];
//			//必须在此指定编码，否则后面toString会导致乱码
//			InputStreamReader is = new InputStreamReader(stream);
//			
//			int len;
//			while ((len = is.read(buff, 0, 4096)) > 0) {
//				builder.append(buff, 0, len);
//			}
//			urlConn.disconnect();
//			String httpresponse = builder.toString();
//			int linkStartPos = data.content.indexOf("\" href=\"") + "\" href=\"".length();
//			if (linkStartPos > 0) {
//			  int linkEndPos = data.content.indexOf('>', linkStartPos)-1;Log.e("@@@@@@@@@@@@@@data.content", data.content.substring(linkStartPos, linkEndPos));
//			  if (linkEndPos > 0)
//			    return data.content.substring(linkStartPos, linkEndPos);
//			}
			return null;
	}
}
