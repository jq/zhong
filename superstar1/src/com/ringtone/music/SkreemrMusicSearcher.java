package com.ringtone.music;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

public class SkreemrMusicSearcher implements IMusicSearcher {
	private static final String URL_SEARCH = "http://www.skreemr.com/results.jsp?q=";
	private static final Pattern PATTERN_BLOCK =
		Pattern.compile("<\\!-- result table begins -->(.*?)<\\!-- result table ends -->", Pattern.DOTALL);
	private static final Pattern PATTERN = 
		Pattern.compile(
		"onclick=\"javascript:pageTracker._trackPageview\\(\'/clicks.*?>" + 
		"(.*?)</a>.*?" +  // 1
		"soundFile=(.*?)\'.*?" + // 2
		"<td>mp3\\s*[-].*?[-].*?[-](.*?)(mb|kb)"  // 3
		,
		Pattern.DOTALL);
	
	int mStart;
	String mSearchUrl;
	boolean mDone;
	
	public SkreemrMusicSearcher() {
	}

	@Override
	public ArrayList<MusicInfo> getNextResultList(Context context) {
		if (mDone)
			return new ArrayList<MusicInfo>();
		
		if (mStart > 0) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			String url = getNextUrl();
			
			Utils.D("URL = " + url);
			
			String html = NetUtils.fetchHtmlPage(MusicSearcherFactory.ID_SKREEMR, url, "UTF-8");
			if (TextUtils.isEmpty(html))
				return null;
			ArrayList<MusicInfo> musicList = getMusicInfoListFromHtml(html);
			if (musicList.size() > 0) {
				if (musicList.size() < 10) {
					mDone = true;
				}
				mStart += musicList.size();
			}
			return musicList;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private String getNextUrl() {
		return mStart == 0 ? mSearchUrl : mSearchUrl + "&l=10&s=" + mStart;
	}

	private ArrayList<MusicInfo> getMusicInfoListFromHtml(String html) {
			Utils.D("+++++++++++++++");
			Utils.printD(html);
			Utils.D("---------------");
			
			ArrayList<MusicInfo> musicList = new ArrayList<MusicInfo>();
			Matcher matcherBlock = PATTERN_BLOCK.matcher(html);
			while (matcherBlock.find()) {
				Matcher m = PATTERN.matcher(matcherBlock.group(1));
				while (m.find()) {
					MusicInfo info = new MusicInfo();
					
					String[] artistAndTitle = m.group(1).trim().split("-", 2);
					if (artistAndTitle.length == 2) {
						info.setArtist(artistAndTitle[0].trim());
						info.setTitle(artistAndTitle[1].trim());
					} else {
						info.setArtist("");
						info.setTitle(artistAndTitle[0].trim());
					}
					info.addDownloadUrl(m.group(2).trim().replaceAll("%3A", ":").replaceAll("%2F", "/"));
					String size = m.group(3).trim();
					if (m.group(4).equals("kb")) {
						info.setDisplayFileSize(size + "K");
					} else {
						info.setDisplayFileSize(size + "M");
					}
	
					musicList.add(info);
				}
			}
			return musicList;
	}

	@Override
	public void setMusicDownloadUrl(Context context, MusicInfo info) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setQuery(String query) {
		mStart = 0;
		mDone = false;
		mSearchUrl = URL_SEARCH + URLEncoder.encode(query);
	}

}
