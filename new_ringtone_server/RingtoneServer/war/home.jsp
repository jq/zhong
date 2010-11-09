<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.ringtone.server.Const"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<title>Ringtones For Your Phone!</title>
		<jsp:include page="/meta_data.html"></jsp:include>
	</head>
	<body onorientationchange="updateOrientation();">
		<div id="page_wrapper">
			<jsp:include page="/header.jsp"></jsp:include>
			<a name='search' accesskey="1"></a>
			<div style="padding:6px" class="sbox">
			<form name="data" action="/ringtoneserver/mobile/search" method="get">	
				<input type="text" name="q" value="" size="10" class="t_input" />
				<button type="submit" name="type" value="q">Search</button>
			</form>
			<h1>Featured: </h1>
			<table class="linklist hot" cellspacing="0" cellpadding="0">
				<tr>
					<td width="22"><img src="/images/play_22.gif" alt="img" width="22" height="22"/></td>
					<td>
						<a href="/ringtoneserver/mobile/search?type=<%= Const.DOWNLOAD_COUNT %>">Top Download</a>
					</td>
				</tr>
				<tr>
					<td width="22"><img src="/images/play_22.gif" alt="img" width="22" height="22"/></td>
					<td>
						<a href="/ringtoneserver/mobile/search?type=<%= Const.ADD_DATE %>">Newest Added</a>
					</td>
				</tr>
				<tr>
					<td width="22"><img src="/images/play_22.gif" alt="img" width="22" height="22"/></td>
					<td>
						<a href="/ringtoneserver/mobile/search?type=<%= Const.CATEGORY %>&<%= Const.CATEGORY %>=<%= Const.CATE_SOUND_EFFECTES %>">Sound Effects</a>
					</td>
				</tr>				
		    </table>
			<h1>Categories:</h1>
			<table class="linklist" cellspacing="0" cellpadding="0">
				<tr>
					<td width="22"><img src="/images/videos_22.gif" alt="img" width="22" height="22"/></td>
					<td class="hl"><a href="/ringtoneserver/mobile/search?type=<%= Const.CATEGORY %>&<%=Const.CATEGORY %>=<%=Const.CATE_COUNTRY %>"><%= Const.CATE_COUNTRY %></a></td>
				</tr>
			</table>
			<table class="linklist" cellspacing="0" cellpadding="0">
				<tr>
					<td width="22"><img src="/images/videos_22.gif" alt="img" width="22" height="22"/></td>
					<td class="hl"><a href="/ringtoneserver/mobile/search?type=<%= Const.CATEGORY %>&<%=Const.CATEGORY %>=<%=Const.CATE_HIP_HOP %>"><%= Const.CATE_HIP_HOP %></a></td>
				</tr>
			</table>
			<table class="linklist" cellspacing="0" cellpadding="0">
				<tr>
					<td width="22"><img src="/images/videos_22.gif" alt="img" width="22" height="22"/></td>
					<td class="hl"><a href="/ringtoneserver/mobile/search?type=<%= Const.CATEGORY %>&<%=Const.CATEGORY %>=<%=Const.CATE_ROCK %>"><%=Const.CATE_ROCK %></a></td>
				</tr>
			</table>
			<table class="linklist" cellspacing="0" cellpadding="0">
				<tr>
					<td width="22"><img src="/images/videos_22.gif" alt="img" width="22" height="22"/></td>
					<td class="hl"><a href="/ringtoneserver/mobile/search?type=<%= Const.CATEGORY %>&<%=Const.CATEGORY %>=<%=Const.CATE_POP %>"><%=Const.CATE_POP %></a></td>
				</tr>
			</table>
	
			<ul class='linklist'>
				<li>
					<a href="/ringtoneserver/mobile/cateall" class="more">All Categories</a>	
				</li>
			</ul>
			<a name='menu' accesskey="0"></a>
			<jsp:include page="/footer.jsp"></jsp:include>
		</div>
	</body>

</html>