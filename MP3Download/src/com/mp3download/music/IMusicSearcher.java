package com.mp3download.music;

import java.util.ArrayList;

import android.content.Context;
import android.os.Handler;

public interface IMusicSearcher {
	public void setQuery(String query);
	public ArrayList<MusicInfo> getNextResultList(Context context);
	public void setMusicDownloadUrl(Context context, MusicInfo info);
}
