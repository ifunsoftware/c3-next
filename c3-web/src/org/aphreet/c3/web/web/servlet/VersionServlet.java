package org.aphreet.c3.web.web.servlet;

import java.util.Scanner;

import javax.servlet.http.HttpServlet;

import org.aphreet.c3.web.util.SpringUtil;
import org.aphreet.c3.web.webbeans.AppBean;

public class VersionServlet extends HttpServlet{

	private static final long serialVersionUID = 6648347436987785330L;

	public void init(){
		Scanner sc = new Scanner(getServletContext().getResourceAsStream("WEB-INF/version.properties"));
		String version = sc.next();
		sc.close();
		
		((AppBean)SpringUtil.getBean("appBean")).setVersion(version);
		
	}
}
