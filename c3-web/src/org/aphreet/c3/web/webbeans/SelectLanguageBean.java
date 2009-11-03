package org.aphreet.c3.web.webbeans;

import java.util.Locale;

import org.aphreet.c3.web.util.HttpUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component
@Scope("request")
public class SelectLanguageBean {

	public String selectRus(){
		HttpUtil.getSession().setAttribute("CURRENT_LOCALE", new Locale("ru", "RU"));
		return "success";
	}
	
	public String selectEng(){
		HttpUtil.getSession().setAttribute("CURRENT_LOCALE", Locale.US);
		return "success";
	}
}
