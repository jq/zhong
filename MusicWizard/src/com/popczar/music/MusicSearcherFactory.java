package com.popczar.music;

public class MusicSearcherFactory {
	
	public static final int ID_SOGOU = 0;
	public static final int ID_SKREEMR = 1;
	public static final int ID_MERGED = 2;
	
	public static IMusicSearcher getInstance(int id) {
		if (id == ID_SOGOU) {
			return new SogouMusicSearcher();
		} else if (id == ID_SKREEMR){
			return new SkreemrMusicSearcher();
		} else if (id == ID_MERGED) {
			return new MergedMusicSearcher();
		}
		
		return null;
	}
}
