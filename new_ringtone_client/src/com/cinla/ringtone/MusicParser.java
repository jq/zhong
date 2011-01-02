package com.cinla.ringtone;

import java.net.URLEncoder;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public class MusicParser {

	private static final String CODING = "utf-8";
	
	private static String getEntireUrl(String url, int startPos) {
		return url+"&start="+startPos;
	}
	
	//return null if there is some errors.
	private static ArrayList<MusicInfo> getMusicList(String url) {
		ArrayList<MusicInfo> musicList = new ArrayList<MusicInfo>();
		try {
			String response = NetUtils.fetchHtmlPage(url, CODING);
			Utils.D("response: "+response);
			JSONArray entries = new JSONArray(response);
			for (int i=0; i<entries.length(); i++) {
				JSONObject entry = entries.getJSONObject(i);
				MusicInfo musicInfo = new MusicInfo();
				musicInfo.setmArtist(entry.getString(Constant.ARTIST));
				musicInfo.setmCategory(entry.getString(Constant.CATEGORY));
				musicInfo.setmDownloadCount(entry.getInt(Constant.DOWNLOAD_COUNT));
				musicInfo.setmImageUrl(entry.getString(Constant.IMAGE));
				musicInfo.setmMp3Url(entry.getString(Constant.S3URL));
				musicInfo.setmRate(entry.getDouble(Constant.AVG_RATE));
				musicInfo.setmSize(entry.getLong(Constant.SIZE));
				musicInfo.setmTitle(entry.getString(Constant.TITLE));
				musicInfo.setmUuid(entry.getString(Constant.UUID));
				musicList.add(musicInfo);
			}
		} catch (Exception e) {
			Utils.D(e.getMessage());
			return null;
		}
		return musicList;
	}
	
	//return null if there is some errors
	public static ArrayList<MusicInfo> getMusicListByQueryKey(String keyWord, int startPos) {
		String realUrl = Constant.BASE_URL + Constant.SEARCH_URL + URLEncoder.encode(keyWord);
		if (startPos != 0) {
			realUrl = getEntireUrl(realUrl, startPos);
		}
		Utils.D("real url: "+realUrl);
		return getMusicList(realUrl);
	}
	
	public static ArrayList<MusicInfo> getMusicListByCategory(String keyWord, int startPos) {
		String realUrl = Constant.BASE_URL + Constant.SEARCH_URL + URLEncoder.encode(keyWord) + "&type=" + Constant.CATEGORY;
		if (startPos != 0) {
			realUrl = getEntireUrl(realUrl, startPos);
		}
		Utils.D("real url: "+realUrl);
		return getMusicList(realUrl);
	}
	
	public static ArrayList<MusicInfo> getMusicListByArtist(String keyWord, int startPos) {
		String realUrl = Constant.BASE_URL + Constant.SEARCH_URL + URLEncoder.encode(keyWord) + "&type=" + Constant.ARTIST;
		if (startPos != 0) {
			realUrl = getEntireUrl(realUrl, startPos);
		}
		Utils.D("real url: "+realUrl);
		return getMusicList(realUrl);
	}
	
	public static ArrayList<MusicInfo> getMusicListByDownloadCount(int startPos) {
		String realUrl = Constant.BASE_URL + Constant.SEARCH_URL + "&type=" + Constant.DOWNLOAD_COUNT;
		if (startPos != 0) {
			realUrl = getEntireUrl(realUrl, startPos);
		}
		Utils.D("real url: "+realUrl);
		return getMusicList(realUrl);
	}
	
	public static ArrayList<MusicInfo> getMusicListByAddDate(int startPos) {
		String realUrl = Constant.BASE_URL + Constant.SEARCH_URL + "&type=" + Constant.ADD_DATE;
		if (startPos != 0) {
			realUrl = getEntireUrl(realUrl, startPos);
		}
		Utils.D("real url: "+realUrl);
		return getMusicList(realUrl);
	}
}
