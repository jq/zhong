package com.ringtone.music;

import java.io.IOException;
import android.os.Handler;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class RealmMusicSearcher implements IMusicSearcher {
	private static final String URL_SEARCH = "http://mp3realm.org/search?q=";
	private static final String URL_SEARCH_PROXY = "http://chaowebs.appspot.com/msearch/music.so?pf=mp3&query=";
	private static final String REALM_MP3 = "http://mp3realm.org/";
	private static final Pattern PATTERN_ROW = Pattern.compile("<li>(.*?)<li>", Pattern.DOTALL);
	private static final Pattern PATTERN = Pattern.compile(
	        ".*?href=\"(.*?)\">.*?<font.*?>(.+?)</font>.*?" + // 1 url 2 title
			"<div.*?Artist.*?</h4>[<a.*Href=.*?>]?([^<]*)<.*?" +   // 3 artist
			"<div.*?Album.*?</h4>[<a.*Href=.*?>]?([^<]*)<.*?" +   // 4 album
			"<div.*?Year.*</h4>([^<]*)<.*?" +   // 5 year
			"<div.*?Genre.*</h4>([^<]*)<.*?" +  // 6 Ignore
			"<div.*?BitRate.*</h4>([^<]*)<.*?" +  // 7 Ignore
			"<div.*?Playtime.*</h4>([^<]*)<.*?" +  // 8 Ignore
			"<div.*?<i>.*?</i>[^0-9]*([^a-zA-Z]*M)B.*?" +   // 9 size
			""
			, Pattern.DOTALL);
	private static final Pattern PATTERN_ARTIST_ALBUM = Pattern.compile(".*?href=\".*?\">(.+)", Pattern.DOTALL);
	
	private static final Pattern PATTERN_DOWNLOAD_URL = Pattern.compile(".*?href=\"([^\"]*)\">.*?\n.*?<font.*?>.*?\n.*?<b>Download</b>");
	
	private static final String DOWNLOAD_MARKER = "Link Status";
	
	private String mSearchUrl;
	private int mPage;  // Next page to fetch.
	
	private volatile Handler mHandler = new Handler();
	private static int sNumQueries = 0;
	
	public RealmMusicSearcher() {
	}
	
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
	
	
	private ArrayList<MusicInfo> getMusicInfoListFromHtml(String html) throws UnsupportedEncodingException {
			Utils.D("+++++++++++++++");
			Utils.D(html);
			Utils.D("+++++++++++++++");
			
			ArrayList<MusicInfo> musicList = new ArrayList<MusicInfo>();
			Matcher matcherRow = PATTERN_ROW.matcher(html);
			while (matcherRow.find()) {
				Matcher m = PATTERN.matcher(matcherRow.group(1));
				while (m.find()) {
					MusicInfo info = new MusicInfo();
					info.addUrl(REALM_MP3+m.group(1).trim());
					String title = m.group(2).trim();
					title = title.replace("<b>", "");
					title = title.replace("</b>", "");
					info.setTitle(title);
					String artist = m.group(3).trim();
                    Matcher matcherArtist = PATTERN_ARTIST_ALBUM.matcher(artist);
                    while (matcherArtist.find()) {
                      artist = matcherArtist.group(1);
                    }
					info.setArtist(artist);
					String album = m.group(4).trim();
                    Matcher matcherAlbum = PATTERN_ARTIST_ALBUM.matcher(album);
                    while (matcherAlbum.find()) {
                      album = matcherAlbum.group(1);
                    }
					info.setAlbum(album);
					String displayFileSize = m.group(9).trim();
					if (displayFileSize.equals("unknown"))
						displayFileSize = "Unknown size";
					info.setDisplayFileSize(displayFileSize);
					//info.setType(m.group(7).trim());
					
					musicList.add(info);
				}
			}
			Utils.D("Exit getMusicInfoListFromHtml");
			return musicList;
	}
	
	// Used to signal between threads.
	class Signal {
		public boolean ready;
	};
	
	class HtmlData {
		public String content;
	};
	
	class MyJavaScriptInterface {  
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
	
	
	private class FetchSearchPage extends WebViewClient {
		@Override
		public void onPageFinished(WebView view, String url) {
			 view.loadUrl("javascript:window.HTMLOUT.parseHtml(document.getElementsByTagName('html')[0].innerHTML);"); 
		}
	}
	
	private boolean loadUrl(final Context context, final String url, final HtmlData data) {
		final Signal s = new Signal();
		s.ready = false;
		mHandler.post(new Runnable() {
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
	@Override
	public ArrayList<MusicInfo> getNextResultList(final Context context) {
		sNumQueries++;
		HtmlData data = new HtmlData();
		if (!loadUrl(context, getNextUrl(), data))
			return null;
		
        if (TextUtils.isEmpty(data.content))                                                                                                                                                                    
            return null;
        ArrayList<MusicInfo> musicList;
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

	
	@Override
	public void setMusicDownloadUrl(Context context, MusicInfo info) {
		try {
			HtmlData data = new HtmlData();
			loadUrl(context, info.getUrl(), data);
			
			int start = data.content.indexOf(DOWNLOAD_MARKER) + DOWNLOAD_MARKER.length();
			Matcher m = PATTERN_DOWNLOAD_URL.matcher(data.content.substring(start));
			//Matcher m = PATTERN_DOWNLOAD_URL.matcher(data.content);
			if (m.find()) {
				info.addDownloadUrl(m.group(1));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
