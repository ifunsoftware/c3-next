<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" isErrorPage="true"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@page import="java.io.StringWriter"%>
<%@page import="java.io.PrintWriter"%><html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<base href="/c3/" />
<link rel="stylesheet" href="css/base.css" />
<title>Error</title>
</head>
<body>
	<div class="head_wrap">
		<div class="head">
			<div class="head_title" style="padding-bottom:22px;">
				<p><a href="index.xhtml">C3 Service</a></p>
				<p id="mantra">Content Collaboration Communication</p>
			</div>
		</div>
	</div>
	<div class="wrap">
		
		<div class="wrap_head">
			<div class="wrap_head_title">
				<p>Error 500</p>
			</div>
		</div>
		<div class="main_wrap">
			<div id="content_wrap">
				Internal Server Error
				<pre style="font-size: 0.7em;">
<% exception.printStackTrace(new PrintWriter(out)); %>
				</pre>
			</div>
		</div>
		<div class="footer_wrap">
			<div class="footer_version">
			</div>
			<div class="footer_menu">
			</div>
		</div>
	</div>
</body>
</html>