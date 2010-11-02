<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.ringtone.server.Const" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>
		<jsp:include page="/meta_data.html"></jsp:include>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<% String query_key = request.getParameter(Const.QUREY); %>
		<title><%= query_key %></title>
	</head>
	
	<body>
		<jsp:include page="/header.jsp"></jsp:include>
		<h1>Ringtones search for: <%= query_key %></h1>
		<%  %>
		<jsp:include page="/footer.jsp"></jsp:include>
	</body>
</html>