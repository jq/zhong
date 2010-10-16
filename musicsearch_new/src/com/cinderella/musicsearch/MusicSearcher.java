package com.cinderella.musicsearch;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class MusicSearcher {
	private static final String URL_SEARCH = Const.Search_Url_Base;
	private static final String URL_DOWNLINK = Const.Link_Url_Base;
	private static final String CODING = "utf-8";
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
		return mPage == 1 ? mSearchUrl : mSearchUrl + "&page=" + mPage;
	}
	
	private ArrayList<MusicInfo> getMusicInfoListFromHtml(String html) {
		ArrayList<MusicInfo> musicList = new ArrayList<MusicInfo>();
		try {
			JSONArray entries = new JSONArray(html);
			for (int i=0; i<entries.length(); i++) {
				JSONObject entry = entries.getJSONObject(i);
				MusicInfo info = new MusicInfo();
				info.setTitle(entry.getString(Const.JSON.Songname));
				info.setArtist(entry.getString(Const.JSON.Singer));
				JSONArray downlinkEntries = entry.getJSONArray(Const.JSON.Downlink);
				info.setDownloadUrl(getLinkListFromJsonArray(downlinkEntries));
				info.setAlbum(entry.getString(Const.JSON.Album));
				info.setDisplayFileSize(entry.getString(Const.JSON.Size));
				info.setType("mp3Test");
				musicList.add(info);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return musicList;
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
	
	public void setMusicDownloadUrl(MusicInfo info) {
		try {
			String key = info.getDownloadUrl().get(0);
			if (Utils.isUrl(key)) {
				Utils.D("isUrl: "+Utils.isUrl(key));
				return;
			} else {
				String html = NetUtils.fetchHtmlPage(mCookie_id, URL_DOWNLINK+key, CODING);
				JSONArray entries = new JSONArray(html);
				info.setDownloadUrl(getLinkListFromJsonArray(entries));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private ArrayList<String> getLinkListFromJsonArray(JSONArray entries) throws JSONException {
		ArrayList<String> downlinkList = new ArrayList<String>();
		for (int j=0; j<entries.length(); j++) {
			downlinkList.add(entries.getString(j));
		}
		return downlinkList;
	}
	
	public int getCurPage() {
		return mPage-1;
	}
}
