package com.ringtone.server;

import java.util.Date;

import javax.jdo.PersistenceManager;

public class SongUtils {
	public static String insertSong(String uuid, String title, String artist, String category, int downloadCount, float avgRate, long size, String fileName, String image, String s3Url) {
		SongEntry songEntry = new SongEntry(uuid, title, artist, category, downloadCount, avgRate, size, fileName, image, s3Url);
		songEntry.setAdd_date(new Date());
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			pm.makePersistent(songEntry);
		} finally {
			pm.close();
		}
		return songEntry.getUuid();
	}
}
