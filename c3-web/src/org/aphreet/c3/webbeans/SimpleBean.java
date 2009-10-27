package org.aphreet.c3.webbeans;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;

import org.aphreet.springframework.web.HttpParam;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component
@Scope("request")
public class SimpleBean {

	@PostConstruct
	public void init(){
		System.out.println(FacesContext.getCurrentInstance().getExternalContext().getSession(true).toString());
	}
	
	@HttpParam("id")
	private String id = "";

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String doIt(){
		id="sdasd";
		return "success";
	}
}
