package com.ringtone.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.org.apache.xml.internal.serializer.utils.Utils;

public class InsertSongServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String uuid = req.getParameter(Const.UUID);
		String title = req.getParameter(Const.TITILE);
		String artist = req.getParameter(Const.ARTIST);
		String category = req.getParameter(Const.CATEGORY);
		int downloadCount = Integer.parseInt(req.getParameter(Const.DOWNLOAD_COUNT));
		float avgRate = Float.parseFloat(req.getParameter(Const.AVG_RATE));
		long size = Integer.parseInt(req.getParameter(Const.SIZE));
		String fileName = req.getParameter(Const.FILE_NAME);
		String image = req.getParameter(Const.IMAGE);
		String s3Url = req.getParameter(Const.S3URL);
		String record = req.getParameter(Const.RECORD);
		boolean isSucc = true;
		try {
			SongUtils.insertSong(uuid, title, artist, category, downloadCount, avgRate, (int)size, fileName, image, s3Url);
		} catch (Exception e) {
			System.out.println("Record: "+record+" "+"failed");
			e.printStackTrace();
			isSucc = false;
		}
		if (isSucc) {
			System.out.println("Record: "+record+" "+"Succ");
		}
	}
}
