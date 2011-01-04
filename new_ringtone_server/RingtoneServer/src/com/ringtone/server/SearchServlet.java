package com.ringtone.server;

import java.io.IOException;

import javax.jdo.PersistenceManager;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.repackaged.org.json.JSONArray;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String isJson = req.getParameter(Const.JSON);
		String key = req.getParameter(Const.QUERY);
		String startStr = req.getParameter(Const.START);
		String type = req.getParameter(Const.TYPE);
		int start = 0;
		if (startStr != null) {
			start = Integer.parseInt(startStr);
		}
		
		List<SongEntry> searchResults = null;
		if (type == null) {
			searchResults = SearchUtils.getResultsByKeyword(key, start);
		} else if (type.equalsIgnoreCase(Const.CATEGORY)) {
			searchResults = SearchUtils.getResultsByCategory(key, start);
		} else if (type.equalsIgnoreCase(Const.DOWNLOAD_COUNT)) {
			searchResults = SearchUtils.getResultsByDownloadCount(start);
		} else if (type.equalsIgnoreCase(Const.ADD_DATE)) {
			searchResults = SearchUtils.getResultsByDate(start);
		} else if (type.equalsIgnoreCase(Const.ARTIST)) {
			searchResults = SearchUtils.getResultsByArtist(key, start);
		}
		JSONArray jsonArray = new JSONArray();
		for (SongEntry songEntry : searchResults) {
			Map<String, String> songMap = new HashMap<String, String>();
			songMap.put(Const.UUID, songEntry.getUuid());
			songMap.put(Const.TITILE, songEntry.getTitle());
			songMap.put(Const.ARTIST, songEntry.getArtist());
			songMap.put(Const.CATEGORY, songEntry.getCategory());
			songMap.put(Const.AVG_RATE, Float.toString(songEntry.getAvg_rate()));
			songMap.put(Const.DOWNLOAD_COUNT, Integer.toString(songEntry.getDownload_count()));
			songMap.put(Const.SIZE, Integer.toString((int)songEntry.getSize()));
			songMap.put(Const.IMAGE, songEntry.getImage());
			songMap.put(Const.S3URL, songEntry.getS3_url());
			jsonArray.put(songMap);
		}
		DebugUtils.D("results size: "+searchResults.size());
		if (start==0 && type==null) {
			QueryUtils.insertQuery(key, searchResults.size());
		}
		String response = null;
		response = jsonArray.toString();
		resp.getOutputStream().write(response.getBytes());
		resp.flushBuffer();
	}

}
