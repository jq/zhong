package com.happy.life;

import java.io.IOException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
public class SogouMusicSearcher {
	private static final String URL_SEARCH = "http://mp3.sogou.com/music.so?pf=mp3&query=";
	private static final String SOGOU_MP3 = "http://mp3.sogou.com";
	private static final Pattern PATTERN_ROW = Pattern.compile("<tr(.*?)</tr>", Pattern.DOTALL);
	private static final Pattern PATTERN = Pattern.compile(
			"<td.*?\\btitle=\"([^\"]*)\".*?" +   // 1
			"<td.*?\\bsinger=\"([^\"]*)\".*?" +   // 2
			"<td.*?\\btitle=\"([^\"]*)\".*?" +   // 3
			"<td.*?</td>.*?" +  // Ignore
			"<td.*?\'(/down.so.*?)\'.*?" +  // 4
			"<td.*?href=\"([^\"]*)\".*?" +  // 5
			"<td.*?</td>.*?" +  // Ignore
			"<td.*?>([^<]*)<.*?" +   // 6
			"<td.*?>([^<]*)<" +   // 7
			""
			, Pattern.DOTALL);
	
	private static final Pattern PATTERN_DOWNLOAD_URL = Pattern.compile("href=\"([^\"]*)\"");
	private static final String DOWNLOAD_MARKER = "下载歌曲";
	
	private String mSearchUrl;
	private int mPage;  // Next page to fetch.
	
	private static volatile Handler sHandler = new Handler();
	private static int sNumQueries = 0;
	
	public void setQuery(String query) {
		mPage = 1;
		try {
			mSearchUrl = URL_SEARCH + URLEncoder.encode(query, "gb2312");
		} catch (UnsupportedEncodingException e) {
			mSearchUrl = URL_SEARCH + URLEncoder.encode(query);
		}
	}
	
	private String getNextUrl() {
		return mPage == 1 ? mSearchUrl : mSearchUrl + "&page=" + mPage;
	}
	
	
	private ArrayList<SogouSearchResult> getMusicInfoListFromHtml(String html) throws UnsupportedEncodingException {
			Utils.D("+++++++++++++++");
			Utils.D(html);
			Utils.D("+++++++++++++++");
			
			ArrayList<SogouSearchResult> musicList = new ArrayList<SogouSearchResult>();
			Matcher matcherRow = PATTERN_ROW.matcher(html);
			while (matcherRow.find()) {
				Matcher m = PATTERN.matcher(matcherRow.group(1));
				while (m.find()) {
				  SogouSearchResult searchResult = new SogouSearchResult();
				  searchResult.setTitle(StringEscapeUtils.unescapeHtml(m.group(1).trim()));
				  searchResult.setArtist(StringEscapeUtils.unescapeHtml(URLDecoder.decode(m.group(2), "gb2312").trim()));
				  searchResult.setAlbum(StringEscapeUtils.unescapeHtml(m.group(3).trim()));
				  searchResult.addUrl(SOGOU_MP3 + m.group(4).trim());
				  searchResult.setLyricUrl(SOGOU_MP3 + m.group(5).trim());
				  String displayFileSize = m.group(6).trim();
				  long  fileSize = 0;
				  if (displayFileSize.equals("未知")) {
				    displayFileSize = "Unknown size";
				  } else {
				    fileSize = Utils.sizeFromStr(displayFileSize);
				  }
				  searchResult.setDisplayFileSize(displayFileSize);
				  searchResult.setFileSize(fileSize);
				  searchResult.setType(m.group(7).trim());
					
				  musicList.add(searchResult);
				}
			}
			Utils.D("Exit getMusicInfoListFromHtml");
			return musicList;
	}
	
	// Used to signal between threads.
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
		sHandler.post(new Runnable() {
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

	// Returns null when something wrong happens.
	public ArrayList<SogouSearchResult> getNextResultList(final Context context) {
		sNumQueries++;
		HtmlData data = new HtmlData();
		if (!loadUrl(context, getNextUrl(), data))
			return null;
		
        if (TextUtils.isEmpty(data.content))                                                                                                                                                                    
            return null;
        ArrayList<SogouSearchResult> musicList;
		try {
			musicList = getMusicInfoListFromHtml(data.content);
			if (musicList.size() == 0 && mPage == 1 && sNumQueries <= 1) {
				Log.i(Utils.TAG, "Retry " + sNumQueries);
				return getNextResultList(context);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
		if (musicList.size() > 0) {
			mPage++;
		}
		return musicList;
	}

	
  public static void setMusicDownloadUrl(Context context, SogouSearchResult info) {
    if (info.getUrlIndex() < info.getUrls().size()) {
      String url = info.getUrls().get(info.getUrlIndex());
      try {
        HtmlData data = new HtmlData();
        loadUrl(context, url, data);
        String html = data.content;

        int start = html.indexOf(DOWNLOAD_MARKER) + DOWNLOAD_MARKER.length();
        Matcher m = PATTERN_DOWNLOAD_URL.matcher(html.substring(start));
        if (m.find()) {
          info.setDownloadUrl(m.group(1));
        }
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      info.setUrlIndex((info.getUrlIndex() + 1) % info.getUrls().size() );
    }

  }
}
