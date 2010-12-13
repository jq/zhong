package com.ringtone.server;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SendMailServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String toEmail = req.getParameter(Const.EMAIL);
		String uuid = req.getParameter(Const.UUID);
		String fromEmail = Const.FROM_EMAIL;
		SongEntry songEntry = SearchUtils.getSongEntryByUUID(uuid);
		String fileName = songEntry.getFile_name();
		fileName = songEntry.getUuid()+fileName.substring(0, fileName.lastIndexOf('.'));
		fileName += ".m4r";
		String downloadLink = Const.S3_PREFIX_M4R+fileName;
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);
		String msgBody = Const.EMAIL_BODY1+"("+songEntry.getTitle()+")"+Const.EMAIL_BODY2+downloadLink+Const.EMAIL_BODY3;
		String msgSubject = Const.EMAIL_SUBJECT+"("+songEntry.getTitle()+")";
		try {
			Message msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(fromEmail, "Free Ringtone Team"));
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail, "Dear User"));
			msg.setSubject(msgSubject);
			msg.setText(msgBody);
			Transport.send(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
		resp.getOutputStream().write("ok".getBytes());
		resp.flushBuffer();
	}
}
