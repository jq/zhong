package com.cinderella.musicsearch;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.text.TextUtils;

public class QQMusicSearcher {
	private static final Pattern PATTERN_ROW = Pattern.compile("<tr\\b(.*?)</tr>", Pattern.DOTALL);
	private static final Pattern PATTERN = Pattern.compile(
			"<td\\s*class=\"data\">([^<]*)</td>.*?" +   // 1 data
 			"<td\\s*class=\"song\">\\s*<a.*?>(.*)</a>.*?" +   // 2 song
 			"(<td\\s*class=\"singer\".*?title=\"([^\"]*)\".*?|<td\\s*class=\"singer\">\\s*([.]*)</td>.*?)" + // 4 singer
 			"(<td\\s*class=\"ablum\">\\s*[^>]*title=\"([^\"]*).*?|<td\\s*class=\"ablum\">\\s*([.]*)</td>.*?)" +  //7 is the ablum name
 			"<td>.*?</td>.*?" +
 			"<td>.*?</td>.*?" +
 			"<td>.*?</td>.*?" +
 			"<td>.*?</td>.*?" +
 			"<td>.*?</td>.*?" +
			"<td\\s*class=\"format\">\\s*?([^<]*)</td>.*?" +			//9 format
			"<td\\s*class=\"size\">\\s*([^<]*)</td>.*?" +				//10 size
 			""
			, Pattern.DOTALL);
	
	private static final String URL_SEARCH = Const.QQ_Search_Url_Base;
	private static final String CODING = "gb2312";
	private int mCookie_id = 0;
	private int mPage = 1;
	private String mSearchUrl;
	
	public void setQuery(String query) {
		mPage = 1;
		try {
			mSearchUrl = URL_SEARCH + URLEncoder.encode(query, CODING);
		} catch (UnsupportedEncodingException e) {
			mSearchUrl = URL_SEARCH + URLEncoder.encode(query);
		}
	}
	
	private String getNextUrl() {
		return mPage == 1 ? mSearchUrl : mSearchUrl + "&p=" + mPage;
	}
	
	private ArrayList<MusicInfo> getMusicInfoListFromHtml(String html) {
		ArrayList<MusicInfo> musicList = new ArrayList<MusicInfo>();
		Matcher matcherRow = PATTERN_ROW.matcher(html);
		while (matcherRow.find()) {
			Matcher m = PATTERN.matcher(matcherRow.group(1));
			while (m.find()) {
				MusicInfo info = new MusicInfo();
				info.setDownloadUrl(getLinksFromData(m.group(1)));
				Utils.D("Data: "+m.group(1));
				for (String link : info.getDownloadUrl()) {
					Utils.D("Links: "+link);
				}
				info.setTitle(getInnerText(m.group(2)));
				info.setArtist(m.group(4));
				info.setAlbum(m.group(7));
				info.setType(m.group(9));
				info.setDisplayFileSize(m.group(10));
				musicList.add(info);
			}
		}
		return musicList;
	}
	
	public String getInnerText(String html) {
		final Pattern PATTERN_TAG = Pattern.compile("\\s*<.+?>\\s*");
		Matcher matcher = PATTERN_TAG.matcher(html);
		String innerText = matcher.replaceAll(" ");
		return innerText;
	}
	
	public ArrayList<String> getLinksFromData(String data) {
		final Pattern PATTERN_LINK = Pattern.compile(".*?(http://[^;]*).*?");
		ArrayList<String> links = new ArrayList<String>();
		Matcher matcher = PATTERN_LINK.matcher(data);
		while(matcher.find()) {
			links.add(matcher.group(1));
		}
		return links;
	}
	
	// Returns null when something wrong happens.
	public ArrayList<MusicInfo> getNextResultList() {
		if (mPage > 0) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {
			String html = NetUtils.fetchHtmlPage(mCookie_id, getNextUrl(), CODING);
			if (TextUtils.isEmpty(html))
				return null;
			ArrayList<MusicInfo> musicList = getMusicInfoListFromHtml(html);
			if (musicList.size() > 0) {
				mPage++;
				return musicList;
			}
			return musicList;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	// Returns null when something wrong happens.
	public ArrayList<MusicInfo> getPrevResultList() {
		if (mPage == 0) {
			return null;
		}
		if (mPage > 0) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {
			String html = NetUtils.fetchHtmlPage(mCookie_id, getNextUrl(), CODING);
			if (TextUtils.isEmpty(html))
				return null;
			ArrayList<MusicInfo> musicList = getMusicInfoListFromHtml(html);
			if (musicList.size() > 0) {
				mPage--;
				return musicList;
			}
			return musicList;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public int getCurPage() {
		return mPage-1;
	}
	
	public void setMusicDownloadUrl(MusicInfo info) {

	}
}
