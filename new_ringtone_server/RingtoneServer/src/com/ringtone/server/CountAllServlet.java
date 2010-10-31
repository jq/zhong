package com.ringtone.server;

import java.io.IOException;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CountAllServlet extends HttpServlet {

	public static final int RESULTS_PER_QUERY = 1000;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {	
		if (!DebugUtils.debug) {
			return;
		}
		PersistenceManager pm = PMF.get().getPersistenceManager();
		Query query = pm.newQuery(SongEntry.class);
		int start = 0;
		boolean isFinish = false;
		
		try {
			while(!isFinish) {
				query.setRange(start, start+RESULTS_PER_QUERY);
				query.addExtension("datanucleus.appengine.datastoreReadConsistency", "EVENTUAL");
				List<SongEntry> results = (List<SongEntry>) query.execute();
				DebugUtils.D("Size per request: "+results.size());
				start += results.size();
				if (results.size() == 0) {
					isFinish = true;
				}
			}
		} finally {
			pm.close();
		}
		
		DebugUtils.D("Total SongEntry: "+start);
	}
	
}
