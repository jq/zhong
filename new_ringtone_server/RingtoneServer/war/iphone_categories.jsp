<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.ringtone.server.Const"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<title>Free Ringtones For Your Phone.</title>
		<jsp:include page="iphone_meta_data.html"></jsp:include>
	</head>
	
	<body class="musiclist">
		<div id="topbar">
			<div id="leftnav">
				<a href="/ringtoneserver/mobile/home">
					<img alt="home" src="/images/home.png"></img>
				</a>
			</div>
		</div>
		<div id="content">
			<ul>
				<%	int i=0; %>
				<%	for (String cate : Const.CATEGORIES) { %>
					<li>
						<a href="/ringtoneserver/mobile/search?type=category&category=<%=cate%>">
							<span class="number"><%= ++i %></span>
							<span class="name"><%= cate %></span>
							<span class="arrow"></span>
						</a>
					</li>
				<%	} %>
			</ul>
		</div>
	</body>
</html>