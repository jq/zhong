<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.ringtone.server.Const" %>
<%@ page import="com.ringtone.server.SearchUtils" %>
<%@page import="java.util.List"%>
<%@page import="com.ringtone.server.SongEntry"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>
	<head>
		<jsp:include page="/meta_data.html"></jsp:include>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<% 	String query_key = request.getParameter(Const.QUERY); %>
		<%	String artist = request.getParameter(Const.ARTIST); %>
		<%	String category = request.getParameter(Const.CATEGORY); %>
		<% 	String startString = request.getParameter(Const.START); %>
		<%  String typeString = request.getParameter(Const.TYPE); %>
		<% 	int start = 0;
			if (startString != null) {
				start = Integer.parseInt(startString);
			}
		%>

		<title>Get Ringtone For Your Phone!</title>
	</head>
	
	<body onorientationchange="updateOrientation();">
		<div id="page_wrapper">
			<jsp:include page="/header.jsp"></jsp:include>
			<%	String headString = null; %>
			<%  if (typeString.equalsIgnoreCase(Const.QUERY)) { headString = "Ringtone search for: "+ query_key; }
					else if (typeString.equalsIgnoreCase(Const.ARTIST)) { headString = "Ringtone of artist: "+artist;} 
					else if (typeString.equalsIgnoreCase(Const.CATEGORY)) { headString = "Ringtone of category: "+category;} 
					else if (typeString.equalsIgnoreCase(Const.DOWNLOAD_COUNT)) { headString = "Top download"; } 
					else if (typeString.equalsIgnoreCase(Const.ADD_DATE)) { headString = "Newest"; } %>
			<h1><%= headString %></h1>
			<% 	List<SongEntry> songEntries = null; %>
			<%	  if (typeString.equalsIgnoreCase(Const.QUERY)) { songEntries = SearchUtils.getResultsByKeyword(query_key, start); } 
					else if (typeString.equalsIgnoreCase(Const.ARTIST)) { songEntries = SearchUtils.getResultsByArtist(artist, start); } 
					else if (typeString.equalsIgnoreCase(Const.CATEGORY)) { songEntries = SearchUtils.getResultsByCategory(category, start); } 
					else if (typeString.equalsIgnoreCase(Const.DOWNLOAD_COUNT)) {songEntries = SearchUtils.getResultsByDownloadCount(start); } 
					else if (typeString.equalsIgnoreCase(Const.ADD_DATE)) {songEntries = SearchUtils.getResultsByDate(start); } %>
			<% 	int index = 0; %>
			<% 	for(SongEntry song : songEntries) { %>
			<%	index++; %>
			<table border="0" cellspacing="0" cellpadding="0" class="bb1 nm" width="100%" >
				<tr>
					<td class="bc4" style="padding:2px;" width="55" valign="top">
						<img alt="img" height="42" src="<%=song.getImage()%>" width="55" />
					</td>
					<td valign="top" class="bc2" width="100%">
						<ul class="nav">
							<li>
	                			<a href="/ringtoneserver/mobile/show?key=<%= song.getUuid() %>" class="nb ico_play">
	                				<font size="4">
										<%=	index %>. <%=song.getTitle() %>
									</font>
								</a>
							</li>
							<li> 
								<img alt="Ico_info" src="/images/ic_mp_artist_playback.png" />
								<%= song.getArtist() %>
				 			</li> 	
						</ul>
					</td>
				</tr>
			</table>
			<% 	} %>
			<jsp:include page="/footer.jsp"></jsp:include>
		</div>
	</body>
</html>