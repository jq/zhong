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
				ArrayList<String> fakeLinkList = new ArrayList<String>();
				fakeLinkList.add(m.group(1));
				info.setDownloadUrl(fakeLinkList);
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
		try {
			String key = info.getDownloadUrl().get(0);
			if (Utils.isUrl(key)) {
				Utils.D("isUrl: "+Utils.isUrl(key));
				return;
			} else {
				String html = NetUtils.fetchHtmlPagePost(Const.QQ_Link_Url, processData(key), CODING);
				info.setDownloadUrl(getLinksFromHtml(html));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private ArrayList<String> getLinksFromHtml(String html) {
		ArrayList<String> links = new ArrayList<String>();
		final Pattern PATTERN_LINK = Pattern.compile("'(http://[^']*)'\\);");
		Matcher matcher = PATTERN_LINK.matcher(html);
		while(matcher.find()) {
			boolean is_in = false;
			for (String link : links) {
				if (link.equalsIgnoreCase(matcher.group(1))) {
					is_in = true;
					break;
				}
			}
			if (!is_in) {
				links.add(matcher.group(1));
			}
			Utils.D("Links: "+matcher.group(1));
		}
		return links;
	}
	
	private static String processData(String data) {
		String[] pieces = data.split("@@");
		String post_data = pieces[0];
		for (String piece : pieces) {
			System.out.println(piece);
		}
		post_data = pieces[0];
		for (int i=1; i<pieces.length-3; i++) {
			post_data += "@@" + pieces[i];
			if (i == 3) {
				post_data += "@@" + pieces[pieces.length-2] + "@@";
			}
		}
		post_data += "@@";
		StringBuilder sb = new StringBuilder();
		System.out.println("Post data1: "+post_data);
		for (int i=0; i<post_data.length(); i++) {
			if ((post_data.charAt(i)<'Z') && (post_data.charAt(i)>'A')) {
				
			} else {
				sb.append(post_data.charAt(i));
			}
		}
		pieces = sb.toString().split("http");
		post_data = pieces[0];
		for (int i=1; i<pieces.length; i++) {
			post_data += "@@" + "http" + pieces[i];
		}
		System.out.println("Post_DATA: " + post_data);
		return post_data;
	}
}
