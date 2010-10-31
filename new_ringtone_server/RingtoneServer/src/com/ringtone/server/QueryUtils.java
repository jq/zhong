package com.ringtone.server;

import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

public class QueryUtils {
	public static String insertQuery(String key, int result_count) {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		Query query = pm.newQuery(QueryEntry.class);
		query.setFilter("key == keyParam");
		query.declareParameters("String keyParam");
		try {
			List<QueryEntry> result = (List<QueryEntry>)query.execute(key);
			if (result!=null && result.size()>0) {
				if (result.iterator().hasNext()) {
					QueryEntry qe = result.get(0);
					qe.setQuery_count(qe.getQuery_count()+1);
					qe.setResult_count(result_count);
					pm.makePersistent(qe);
				}
			} else {
				QueryEntry qe = new QueryEntry(key);
				qe.setResult_count(result_count);
				pm.makePersistent(qe);
			}
		} finally {
			query.closeAll();
			pm.close();
		}
		return key;
	}
}
