<%@ page contentType="text/html;charset=UTF-8" language="java" %>
  
<% 
  String userAgent = request.getHeader("user-agent");
  if (true) {
//  if ((userAgent.indexOf("iPhone") != -1) || (userAgent.indexOf("iPod") != -1)) {
//    response.sendRedirect("iphone_index.html");
	response.sendRedirect("iphone_index.html");
  } else {
    response.sendRedirect("/ringtoneserver/mobile/home");
  }  
%>