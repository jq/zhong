package com.popczar.music;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;

public class SkreemrMusicSearcher implements IMusicSearcher {
	private static final String URL_SEARCH = "http://www.skreemr.com/results.jsp?q=";
	private static final Pattern PATTERN = 
		Pattern.compile(
		"<SPAN CLASS=\"title\">File: <\\/SPAN>.*?" +
		"<a href=\"(.*?)\".*?" +
		"Song:</b> (.*?)<br><b>Artist:</b> (.*?)<br>" +
		"<b>Album:</b> (.*?)<br>.*?" +
		"Details: </SPAN>.*?-.*?-(.*?)mb"
		,
		Pattern.DOTALL);
	
	int mStart;
	String mSearchUrl;
	boolean mDone;
	
	public SkreemrMusicSearcher() {
	}

	@Override
	public ArrayList<MusicInfo> getNextResultList() {
		if (mDone)
			return new ArrayList<MusicInfo>();
		
		if (mStart > 0) {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			String url = getNextUrl();
			
			Utils.D("URL = " + url);
			
			String html = NetUtils.fetchHtmlPage(url, "UTF-8");
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
			Utils.D(html);
			Utils.D("+++++++++++++++");
			
			ArrayList<MusicInfo> musicList = new ArrayList<MusicInfo>();
			Matcher m = PATTERN.matcher(html);
			while (m.find()) {
				MusicInfo info = new MusicInfo();
				info.setDownloadUrl(m.group(1).trim());
				info.setTitle(m.group(2).trim());
				info.setArtist(m.group(3).trim());
				info.setAlbum(m.group(4).trim());
				info.setDisplayFileSize(m.group(5).trim() + "M");

				musicList.add(info);
			}
			return musicList;
	}

	@Override
	public void setMusicDownloadUrl(MusicInfo info) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setQuery(String query) {
		mStart = 0;
		mDone = false;
		mSearchUrl = URL_SEARCH + URLEncoder.encode(query);
	}

}
