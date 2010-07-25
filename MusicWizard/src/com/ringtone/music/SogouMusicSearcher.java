package com.ringtone.music;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;


public class SogouMusicSearcher implements IMusicSearcher {
	private static final String URL_SEARCH = "http://mp3.sogou.com/music.so?pf=mp3&query=";
	private static final String URL_SEARCH_PROXY = "http://chaowebs.appspot.com/msearch/music.so?pf=mp3&query=";
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
	private String mProxyUrl;
	private int mPage;  // Next page to fetch.
	
	private static boolean sUseProxy = false;
	
	public SogouMusicSearcher() {
	}
	
	public void setQuery(String query) {
		mPage = 1;
		try {
			mSearchUrl = URL_SEARCH + URLEncoder.encode(query, "gb2312");
			mProxyUrl = URL_SEARCH_PROXY + URLEncoder.encode(query, "gb2312");
		} catch (UnsupportedEncodingException e) {
			mSearchUrl = URL_SEARCH + URLEncoder.encode(query);
			mProxyUrl = URL_SEARCH_PROXY + URLEncoder.encode(query);
		}
	}
	
	private String getNextUrl() {
		String baseUrl = sUseProxy ? mProxyUrl : mSearchUrl;
		return mPage == 1 ? baseUrl : baseUrl + "&page=" + mPage;
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
					info.setTitle(m.group(1).trim());
					info.setArtist(URLDecoder.decode(m.group(2), "gb2312").trim());
					info.setAlbum(m.group(3).trim());
					info.addUrl(SOGOU_MP3 + m.group(4).trim());
					info.setLyricUrl(SOGOU_MP3 + m.group(5).trim());
					String displayFileSize = m.group(6).trim();
					if (displayFileSize.equals("未知"))
						displayFileSize = "Unknown size";
					info.setDisplayFileSize(displayFileSize);
					info.setType(m.group(7).trim());
					
					musicList.add(info);
				}
			}
			Utils.D("Exit getMusicInfoListFromHtml");
			return musicList;
	}

	// Returns null when something wrong happens.
	public ArrayList<MusicInfo> getNextResultList() {
		if (mPage > 0) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			String html = NetUtils.fetchHtmlPage(MusicSearcherFactory.ID_SOGOU, getNextUrl(), "gb2312");
			if (TextUtils.isEmpty(html))
				return null;
			ArrayList<MusicInfo> musicList = getMusicInfoListFromHtml(html);
			if (musicList.size() > 0) {
				mPage++;
				return musicList;
			}
			/*
			else if (!sUseProxy && mPage == 1) {
				// Give it one more chance.
				sUseProxy = true;
				Log.i(Utils.TAG, "Switching to proxy mode");
				html = NetUtils.fetchHtmlPage(getNextUrl(), "gb2312");
				musicList = getMusicInfoListFromHtml(html);
				if (musicList.size() > 0) {
					mPage++;
				}
			}
			*/
			return musicList;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	
	@Override
	public void setMusicDownloadUrl(Context context, MusicInfo info) {
	  if(info.getUrlIndex() < info.getUrls().size()) {
	    String url = info.getUrls().get(info.getUrlIndex());
		try {
			String html = NetUtils.fetchHtmlPage(
					MusicSearcherFactory.ID_SOGOU, url, "gb2312");			
			int start = html.indexOf(DOWNLOAD_MARKER) + DOWNLOAD_MARKER.length();
			Matcher m = PATTERN_DOWNLOAD_URL.matcher(html.substring(start));
			if (m.find()) {
				info.setDownloadUrl(m.group(1));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		info.setUrlIndex( (info.getUrlIndex() + 1) % info.getUrls().size() );
	  }
	}
}
