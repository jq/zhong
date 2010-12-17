<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.ringtone.server.Const"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<title>Insert title here</title>
		<jsp:include page="/meta_data.html"></jsp:include>
	</head>

	<body>
		<jsp:include page="/header.jsp"></jsp:include>
		<h1>All Categories: </h1>
		<% 	for (int index=0; index<Const.CATEGORIES_NAME.length; index++) { %>	
				<table border="0" cellspacing="0" cellpadding="0" class="bb1 nm" width="100%" >
					<tr>
						<td valign="top" class="bc2" width="100%">
							<ul class="nav">
								<li>
	                				<a href="/ringtoneserver/mobile/search?type=<%=Const.CATEGORY %>&<%=Const.CATEGORY %>=<%=Const.CATEGORIES_VALUE[index] %>" class="nb ico_play">
	                					<font size="4">
											<%=index+1 %>. <%=Const.CATEGORIES_NAME[index] %>
										</font>
									</a>
								</li>
								<li>
				 				</li> 	
							</ul>
						</td>
					</tr>
				</table> 
		<% 	}%>
		<jsp:include page="/footer.jsp"></jsp:include>
	</body>
</html>