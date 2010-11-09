<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.ringtone.server.SongEntry"%>
<%@page import="com.ringtone.server.SearchUtils"%>
<%@page import="com.ringtone.server.Const" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<title>Ringtones for your phone!</title>
		<jsp:include page="/meta_data.html"></jsp:include>
	</head>

	<body onorientationchange="updateOrientation();">
		<div id="page_wrapper">
			<jsp:include page="/header.jsp"></jsp:include>
			<h1>Ringtones Information: </h1>
			<%	String uuid = request.getParameter(Const.KEY); %>
			<%	SongEntry songEntry = SearchUtils.getSongEntryByUUID(uuid); %>
			<table border="0" cellspacing="0" cellpadding="0" width="100%" class="pageHeader">
				<tr>		   
	       			<td class="bc4" style="padding:2px;" width="55" valign="top">
	    						<img alt="img" height="42" src="<%=	songEntry.getImage() %>" width="55" />
	    			</td>    			
	    	   		<td valign="top" class="bc2" width="100%">
	    			<ul class="nav">
	    				<li>
	                   		<a href="<%= songEntry.getS3_url() %>" class="nb ico_play">
	                    	<font size="4">
	    						<%=songEntry.getTitle() %>
	    					</font>
	    					</a>
	    				</li>
	    				<li>   			    
	    					<img alt="Ico_info" src="/images/ic_mp_artist_playback.png" />	<%=	songEntry.getArtist() %>
						</li> 
	    			</td>		
				</tr>
			</table>
			
			<table border="0" cellspacing="0" cellpadding="0" width="100%" class="pageHeader">
		    	<tr>
		        	<td>
	   	       		</td>
		    	</tr>
				<tr>
			    	<td class="bc2" width="100%"><li>Artist: <%= songEntry.getArtist() %></li> </td>
				</tr>
				<tr>
			    	<td class="bc2" width="100%"><li>Category: <%= songEntry.getCategory() %></li> </td>
				</tr>
				<tr>
			    	<td class="bc2" width="100%"><li>Downloads: <%= songEntry.getDownload_count() %></li> </td>
				</tr>
				<tr>
			    	<td class="bc2" width="100%"><li>Size: <%= songEntry.getSize() %></li> </td>
				</tr>
				<tr>
			    	<td class="bc2" width="100%"><li>Date Added: <%=songEntry.getAdd_date().toString() %></li> </td>
				</tr>
			</table>
			<h1>More Ringtones: </h1>
			<table class="linklist hot" cellspacing="0" cellpadding="0">
				<tr>
					<td width="22"><img src="/images/play_22.gif" alt="img" width="22" height="22"/></td>
					<td>
						<a href="/ringtoneserver/mobile/search?artist=<%= songEntry.getArtist() %>&<%= Const.TYPE %>=<%=Const.ARTIST %>">More by <%= songEntry.getArtist() %></a>
					</td>
				</tr>
				<tr>
					<td width="22"><img src="/images/play_22.gif" alt="img" width="22" height="22"/></td>
					<td>
						<a href="/ringtoneserver/mobile/search?category=<%= songEntry.getCategory() %>&<%=Const.TYPE %>=<%=Const.CATEGORY %>">More from <%= songEntry.getCategory() %></a>
					</td>
				</tr>
			</table>
			<jsp:include page="/footer.jsp"></jsp:include>
		</div>
	</body>
</html>