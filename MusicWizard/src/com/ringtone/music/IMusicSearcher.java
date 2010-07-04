package com.ringtone.music;

import java.util.ArrayList;

import android.content.Context;

public interface IMusicSearcher {
	public void setQuery(String query);
	public ArrayList<MusicInfo> getNextResultList();
	public void setMusicDownloadUrl(Context context, MusicInfo info);
}
