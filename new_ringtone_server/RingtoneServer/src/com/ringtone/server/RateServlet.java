package com.ringtone.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RateServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String uuid = req.getParameter(Const.UUID);
		Double rateValue = Double.parseDouble(req.getParameter(Const.RATE));
		SongUtils.updateRate(uuid, rateValue);
	}

}
