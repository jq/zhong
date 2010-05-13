package com.popczar.music;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;

public class BDSearcher implements IMusicSearcher {
	
	private static final String BASE_URL = "http://221.195.40.105:10008/m?f=ms&tn=baidump3&ct=134217728&lf=&rn=&lm=0&word=";
	private static final Pattern PATTERN = Pattern.compile(
			"<tr>\\s*"+
			"<td class=tdn>.*?</td>\\s*" +    
			"<td class=d><a href=\"(.*?)\".*?target=\"_blank\">(.*?)</a></td>\\s*" +
			//"<td>.*?<a href=.*?target=\"_blank\">(.*?)</a>.*?</td>\\s*" +
			"<td>(.*?)</td>\\s*" +
			"<td.*?</td>\\s*" +
			"<td.*?</td>\\s*" +
			"<td.*?</td>\\s*" +
			"<td.*?</td>\\s*" +
			"<td>(.*?)</td>.*?" +
			"</tr>" +
			""
			,
			Pattern.DOTALL
			);
	
	private static final Pattern PATTERN_ARTIST = Pattern.compile(
			"<a href=.*?target=\"_blank\">(.*?)</a>",
			Pattern.DOTALL);
	
	private int mStart = 0;
	private String mSearchUrl;
	
	private String getNextUrl() {
		return mStart == 0 ? mSearchUrl : mSearchUrl + "&pn=" + mStart;
	}

	@Override
	public ArrayList<MusicInfo> getNextResultList() {
		try {
			String url = getNextUrl();
			
			Utils.D("URL = " + url);
			
			String html = NetUtils.fetchHtmlPage(url, "gb2312");
			if (TextUtils.isEmpty(html))
				return null;
			ArrayList<MusicInfo> musicList = getMusicInfoListFromHtml(html);
			if (musicList.size() > 0) {
				mStart += musicList.size();
			}
			return musicList;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	private ArrayList<MusicInfo> getMusicInfoListFromHtml(String html) {
			Utils.D("+++++++++++++++");
			Utils.printD(html);
			Utils.D("---------------");
			
			ArrayList<MusicInfo> musicList = new ArrayList<MusicInfo>();
			Matcher m = PATTERN.matcher(html);
			while (m.find()) {
				MusicInfo info = new MusicInfo();
				info.setDownloadUrl(Utils.trimTag(m.group(1).trim()));
				info.setTitle(Utils.trimTag(m.group(2).trim()));
				info.setDisplayFileSize(Utils.trimTag(m.group(4).trim()));
				
				Matcher m2 = PATTERN_ARTIST.matcher(m.group(3));
				if (m2.find()) {
					info.setArtist(Utils.trimTag(m2.group(1).trim()));
				}
				
				/*
				System.out.println("====================");
				System.out.println(matcherBlock.group(1));
				System.out.println(matcherBlock.group(2));
				System.out.println(matcherBlock.group(3));
				System.out.println(matcherBlock.group(4));
				*/
				musicList.add(info);
			}
			return musicList;
	}

	@Override
	public void setMusicDownloadUrl(MusicInfo info) {
	}

	@Override
	public void setQuery(String query) {
		// TODO Auto-generated method stub
		mStart = 0;
		mSearchUrl = BASE_URL + URLEncoder.encode(query);
	}

}
