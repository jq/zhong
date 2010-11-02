<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.ringtone.server.Const"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<title>Ringtones For Your Phone!</title>
		<jsp:include page="/meta_data.html"></jsp:include>
	</head>
	<body>
		<jsp:include page="/header.jsp"></jsp:include>
		<a name='search' accesskey="1"></a>
		<div style="padding:6px" class="sbox">
		<form name="data" action="/mobile/search/" method="get">	
			<input type="text" name="q" value="" size="10" class="t_input" />	
			<button name="submit" value="go" type="submit" >Search</button>
		</form>
		<h1>Featured: </h1>
		<table class="linklist hot" cellspacing="0" cellpadding="0">
			<tr>
				<td width="22"><img src="/images/play_22.gif" alt="img" width="22" height="22"/></td>
				<td>
					<a href="/mobile/bb100/">Current Hot Music</a>
				</td>
			</tr>
			<tr>
				<td width="22"><img src="/images/play_22.gif" alt="img" width="22" height="22"/></td>
				<td>
					<a href="/mobile/topdl/">Weekly Top Download</a>
				</td>
			</tr>
			<tr>
				<td width="22"><img src="/images/play_22.gif" alt="img" width="22" height="22"/></td>
				<td>
					<a href="/mobile/newest/">Newest Added</a>
				</td>
			</tr>
			<tr>
				<td width="22"><img src="/images/play_22.gif" alt="img" width="22" height="22"/></td>
				<td>
					<a href="/mobile/soundall/">Sound Effects</a>
				</td>
			</tr>				
	    </table>
		<h1>Categories:</h1>
		<table class="linklist" cellspacing="0" cellpadding="0">
			<tr>
				<td width="22"><img src="/images/videos_22.gif" alt="img" width="22" height="22"/></td>
				<td class="hl"><a href="/mobile/cate/Christian">Christian</a></td>
			</tr>
		</table>
		<table class="linklist" cellspacing="0" cellpadding="0">
			<tr>
				<td width="22"><img src="/images/videos_22.gif" alt="img" width="22" height="22"/></td>
				<td class="hl"><a href="/mobile/cate/Metal">Metal</a></td>
			</tr>
		</table>
		<table class="linklist" cellspacing="0" cellpadding="0">
			<tr>
				<td width="22"><img src="/images/videos_22.gif" alt="img" width="22" height="22"/></td>
				<td class="hl"><a href="/mobile/cate/Holiday">Holiday</a></td>
			</tr>
		</table>
		<table class="linklist" cellspacing="0" cellpadding="0">
			<tr>
				<td width="22"><img src="/images/videos_22.gif" alt="img" width="22" height="22"/></td>
				<td class="hl"><a href="/mobile/cate/R&B">R&B</a></td>
			</tr>
		</table>
  		<table class="linklist" cellspacing="0" cellpadding="0">
	    	<tr>
				<td width="22"><img src="/images/videos_22.gif" alt="img" width="22" height="22"/></td>
				<td class="hl"><a href="/mobile/cate/World Music">World Music</a></td>
		 	</tr>
  		</table>

		<ul class='linklist'>
			<li>
				<a href="/mobile/cateall/" class="more">All Categories</a>	
			</li>
		</ul>
		<a name='menu' accesskey="0"></a>
		<jsp:include page="/footer.jsp"></jsp:include>
	</body>

</html>