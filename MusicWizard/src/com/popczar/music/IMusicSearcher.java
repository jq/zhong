package com.popczar.music;

import java.util.ArrayList;

public interface IMusicSearcher {
	public void setQuery(String query);
	public ArrayList<MusicInfo> getNextResultList();
	public void setMusicDownloadUrl(MusicInfo info);
}
