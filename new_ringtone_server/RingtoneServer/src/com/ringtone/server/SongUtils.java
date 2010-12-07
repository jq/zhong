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
	
	public static void increaseDownloadCount(String uuid) {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		SongEntry songEntry = null;
		try {
			songEntry = (SongEntry)pm.getObjectById(SongEntry.class, uuid);
			if (songEntry == null) {
				return;
			}
			songEntry.setDownload_count(songEntry.getDownload_count()+1);
		} finally {
			pm.close();
		}
	}
	
	public static void updateRate(String uuid, double rate) {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		SongEntry songEntry = null;
		int rateCount = 0;
		double rateValue = 0;
		try {
			songEntry = (SongEntry)pm.getObjectById(SongEntry.class, uuid);
			if (songEntry == null) {
				return;
			}
			rateCount = songEntry.getRate_count()+1;
			songEntry.setRate_count(rateCount);
			rateValue = songEntry.getAvg_rate();
			rateValue = (rate+rateValue*(rateCount-1))/rateCount;
			songEntry.setAvg_rate((float)rateValue);
		} finally {
			pm.close();
		}
	}
}
