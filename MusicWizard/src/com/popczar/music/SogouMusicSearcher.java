package com.popczar.music;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SogouMusicSearcher {
	private static final String URL_SEARCH = "http://mp3.sogou.com/music.so?pf=mp3&query=";
	private static final String URL_SEARCH_PROXY = "http://chaowebs.appspot.com/msearch/music.so?pf=mp3&query=";
	private static final String SOGOU_MP3 = "http://mp3.sogou.com";
	private static final Pattern PATTERN_ROW = Pattern.compile("<tr(.*?)</tr>", Pattern.DOTALL);
	private static final Pattern PATTERN = Pattern.compile(
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
	
	private static final Pattern PATTERN_DOWNLOAD_URL = Pattern.compile("href=\"([^\"]*)\"");
	private static final String DOWNLOAD_MARKER = "下载歌曲";
	
	private String mSearchUrl;
	private int mPage;  // Next page to fetch.
	
	private static boolean sUseProxy = false;
	
	public SogouMusicSearcher(String query) {
		mPage = 1;
		String url = sUseProxy ? URL_SEARCH_PROXY : URL_SEARCH;
		try {
			mSearchUrl = url + URLEncoder.encode(query, "gb2312");
		} catch (UnsupportedEncodingException e) {
			mSearchUrl = url + URLEncoder.encode(query);
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
					info.setTitle(m.group(1).trim());
					info.setArtist(URLDecoder.decode(m.group(2), "gb2312").trim());
					info.setAlbum(m.group(3).trim());
					info.setUrl(SOGOU_MP3 + m.group(4).trim());
					info.setLyricUrl(SOGOU_MP3 + m.group(5).trim());
					String displayFileSize = m.group(6).trim();
					if (displayFileSize.equals("未知"))
						displayFileSize = "Unknown size";
					info.setDisplayFileSize(displayFileSize);
					info.setType(m.group(7).trim());
					
					musicList.add(info);
				}
			}
			if (musicList.size() > 0) {
				mPage++;
			}
			return musicList;
	}

	// Returns null when something wrong happens.
	public ArrayList<MusicInfo> getMusicInfoList() {
		try {
			String html = NetUtils.fetchHtmlPage(getNextUrl(), "gb2312");
			return getMusicInfoListFromHtml(html);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	
	public void setMusicDownloadUrl(MusicInfo info) {
		try {
			String html = NetUtils.fetchHtmlPage(info.getUrl(), "gb2312");
			
			int start = html.indexOf(DOWNLOAD_MARKER) + DOWNLOAD_MARKER.length();
			Matcher m = PATTERN_DOWNLOAD_URL.matcher(html.substring(start));
			if (m.find()) {
				info.setDownloadUrl(m.group(1));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
