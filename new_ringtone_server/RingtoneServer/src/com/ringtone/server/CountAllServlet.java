package com.ringtone.server;

import java.io.IOException;
import java.util.List;
import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;


public class CountAllServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {	
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Entity globalStat = datastore.prepare(new Query("__Stat_Kind__").addFilter("kind_name", FilterOperator.EQUAL, "SongEntry")).asSingleEntity();
		Long totalSongEntities = (Long) globalStat.getProperty("count");
		resp.getOutputStream().write(Long.toString(totalSongEntities).getBytes());
		resp.flushBuffer();
	}
	
}
