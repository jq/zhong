<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ page import="com.ringtone.server.Const"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>
		<title>Free Ringtones For Your Phone</title>
		<jsp:include page="/iphone_meta_data.html"></jsp:include>
	</head>

	<body>
		<div id="topbar">
			<div id="title">
				Ringtones
			</div>
		</div>
		<div id="content">
			<span class="graytitle">Features</span>		
			<ul class="pageitem">
				<li class="menu">
					<a href="/ringtoneserver/mobile/search?type=download_count">
						<span class="name">
							Top Download
						</span>
						<span class="arrow"></span>
					</a>
				</li>
				<li class="menu">
					<a href="/ringtoneserver/mobile/search?type=add_date">
						<span class="name">
							Newest Added
						</span>
						<span class="arrow"></span>
					</a>
				</li>
				<li class="menu">
					<a href="/ringtoneserver/mobile/search?type=category&category=Sound Effects">
						<span class="name">
							Sound Effects
						</span>
						<span class="arrow"></span>
					</a>
				</li>
			</ul>
			<span class="graytitle">Categories</span>
			<ul class="pageitem">
				<% 	int i = 0; %>
				<% 	for (String cate : Const.HOME_CATEGORIES) { %>
						<li class="menu">
							<a href="/ringtoneserver/mobile/search?type=category&category=<%= cate%>">
								<span class="name">
									<%= cate %>
								</span>
								</span><span class="arrow"></span>
							</a>
						</li>
				<% 	} %>
				<li class="menu">
					<a href="/ringtoneserver/mobile/cateall">
						<span class="name">
							Loading more...
						</span>
					</a>
				</li>
			</ul>
		</div>
	</body>
</html>