package org.aphreet.c3.web.webbeans.user;

import javax.annotation.PostConstruct;

import org.aphreet.springframework.web.HttpParam;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
public class SigninUserBean {

	@HttpParam("signinfailure")
	private Integer signInFailure;
	
	private Boolean renderError = false;
	
	@PostConstruct
	public void init(){
		if(signInFailure != null){
			renderError = signInFailure != null;
		}
	}

	public void setSignInFailure(Integer signInFailure) {
		this.signInFailure = signInFailure;
	}

	public Boolean getRenderError() {
		return renderError;
	}

	public void setRenderError(Boolean renderError) {
		this.renderError = renderError;
	}
}
