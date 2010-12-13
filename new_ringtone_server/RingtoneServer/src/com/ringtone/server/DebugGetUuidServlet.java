package com.ringtone.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DebugGetUuidServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String file_name = req.getParameter(Const.FILE_NAME);
		SongEntry songEntry = SearchUtils.getSongEntryByFileName(file_name);
		if (songEntry != null) {
			resp.getOutputStream().write(songEntry.getUuid().getBytes());
		} else {
			resp.getOutputStream().write("missig".getBytes());
		}
	}
	
}
