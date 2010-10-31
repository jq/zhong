package com.ringtone.server;

import java.io.IOException;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.repackaged.org.json.JSONArray;
import com.google.appengine.repackaged.org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String key = req.getParameter(Const.QUREY);
		String pageStr = req.getParameter(Const.PAGE);
		int page = 0;
		if (pageStr != null) {
			page = Integer.parseInt(pageStr);
		}
		PersistenceManager pm = PMF.get().getPersistenceManager();
		List<SongEntry> searchResults = SearchJanitor.searchSongEntries(key, pm, page);
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
		QueryUtils.insertQuery(key, searchResults.size());
		String response = null;
		response = jsonArray.toString();
		resp.getOutputStream().write(response.getBytes());
		resp.flushBuffer();
	}

}
