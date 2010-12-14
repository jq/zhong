package com.ringtone.music;

import java.net.URLDecoder;
import java.util.ArrayList;

import org.apache.commons.lang.StringEscapeUtils;

import android.content.Context;

public class LocalSearcher implements IMusicSearcher {

	private int iCount=1;
	@Override
	public ArrayList<MusicInfo> getNextResultList(Context context) {
		// TODO Auto-generated method stub
		ArrayList<MusicInfo> musicList = new ArrayList<MusicInfo>();

		for(int i=1;i<=10;i++){
			MusicInfo info = new MusicInfo();
			info.setTitle("title");
			info.setArtist("artist");
			info.setAlbum("album");
			info.addUrl("url");
			info.setLyricUrl("lyricurl");
			info.setDisplayFileSize(Integer.toString(iCount++));
			info.setType("mp3");
			musicList.add(info);
		}
		
		return musicList;
	}

	@Override
	public void setMusicDownloadUrl(Context context, MusicInfo info) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setQuery(String query) {
		// TODO Auto-generated method stub
		
	}
}