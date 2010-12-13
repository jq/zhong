package com.ringtone.server;

import java.util.ArrayList;
import java.util.List;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

public class SearchUtils {
	
	public static List<SongEntry> getResultsByKeyword(String key, int start) {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		List<SongEntry> searchResults = SearchJanitor.searchSongEntries(key, pm, start);
		if (searchResults != null) {
			return searchResults;
		} else {
			return new ArrayList<SongEntry>();
		}
	}
	
//	public static SongEntry getSongEntryByUUID(String uuid) {
//		PersistenceManager pm = PMF.get().getPersistenceManager();
//		Query query = pm.newQuery(SongEntry.class);
//		query.setFilter("uuid == lastParam");
//		query.setRange(0, 1);
//		query.declareParameters("String lastParam");
//		List<SongEntry> results = null;
//		SongEntry result = null;
//		try {
//			results = (List<SongEntry>)query.execute(uuid);
//			if (results.size() > 0) {
//				result = results.get(0);
//			}
//		} finally {
//			query.closeAll();
//		}
//		return result;
//	}
	
	public static SongEntry getSongEntryByFileName(String fileName) {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		Query query = pm.newQuery(SongEntry.class);
		query.setFilter("file_name == lastParam");
		query.setRange(0, 1);
		query.declareParameters("String lastParam");
		List<SongEntry> results = null;
		SongEntry result = null;
		try {
			results = (List<SongEntry>)query.execute(fileName);
			if (results.size() > 0) {
				result = results.get(0);
			}
		} finally {
			query.closeAll();
		}
		return result;
	}
	
	public static SongEntry getSongEntryByUUID(String uuid) {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		SongEntry songEntry = null;
		try {
			songEntry = (SongEntry)pm.getObjectById(SongEntry.class, uuid);
		} finally {
			pm.close();
		}
		return songEntry;
	}
	
	public static List<SongEntry> getResultsByArtist(String artist, int start) {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		Query query = pm.newQuery(SongEntry.class);
		query.setFilter("artist == lastParam");
		query.declareParameters("String lastParam");
		query.setRange(start, start+Const.MAX_RESULTS_PER_QUERY);
		List<SongEntry> results = null;
		try {
			results = (List<SongEntry>)query.execute(artist);
		} finally {
			query.closeAll();
		}
		return results;
	}
	
	public static List<SongEntry> getResultsByCategory(String category, int start) {
		System.out.println("in getByCate");
		PersistenceManager pm = PMF.get().getPersistenceManager();
		Query query = pm.newQuery(SongEntry.class);
		query.setFilter("category == lastParam");
		query.declareParameters("String lastParam");
		query.setRange(start, start+Const.MAX_RESULTS_PER_QUERY);
		List<SongEntry> results = null;
		try {
			results = (List<SongEntry>)query.execute(category);
		} finally {
			query.closeAll();
		}
		return results;
	}
	
	public static List<SongEntry> getResultsByDownloadCount(int start) {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		Query query = pm.newQuery(SongEntry.class);
		query.setOrdering("download_count desc");
		query.setRange(start, start+Const.MAX_RESULTS_PER_QUERY);
		List<SongEntry> results = null;
		try {
			results = (List<SongEntry>)query.execute();
		} finally {
			query.closeAll();
		}
		return results;
	}
	
	public static List<SongEntry> getResultsByDate(int start) {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		Query query = pm.newQuery(SongEntry.class);
		query.setOrdering("add_date desc");
		query.setRange(start, start+Const.MAX_RESULTS_PER_QUERY);
		List<SongEntry> results = null;
		try {
			results = (List<SongEntry>)query.execute();
		} finally {
			query.closeAll();
		}
		return results;
	}

}
