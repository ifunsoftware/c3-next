package org.aphreet.c3.web.webbeans;

import javax.annotation.PostConstruct;

import org.aphreet.c3.web.util.FacesUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component
@Scope("request")
public class MainMenuBean {

	private String activeViewId;
	
	@PostConstruct
	public void load(){
		String viewId = FacesUtil.getViewRootId();
		this.activeViewId = viewId;
	}
	
	public String getGroupStyle(){
		return getStyleByRegexp("(/group/\\w+[.]jspx)|(/index.jspx)");
	}
	
	public String getSearchStyle(){
		return getStyle("/search.jspx");
	}
	
	public String getAboutStyle(){
		return getStyle("/about.jspx");
	}
	
	private String getStyle(String ... views){
		for (String string : views) {
			if(string.equals(activeViewId)){
				return "head_menu_item_selected";
			}
		}
		return "head_menu_item_plain";
	}
	
	private String getStyleByRegexp(String regexp){
		if(activeViewId.matches(regexp)){
			return "head_menu_item_selected";
		}else{
			return "head_menu_item_plain";
		}
	}
}
