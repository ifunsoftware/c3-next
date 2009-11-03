package org.aphreet.c3.web.util;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

/**
 * Utility class to work with JSF
 * @author Mikhail Malygin
 *
 */
public class FacesUtil {

	public static UIComponent findComponent(String id){
		UIComponent root = FacesContext.getCurrentInstance().getViewRoot();
		
		return findComponent(root, id);
	}
	
	public static UIComponent findComponent(UIComponent comp, String id){
		UIComponent child = comp.findComponent(id);
		if(child != null){
			return child;
		}
		
		List<UIComponent> children = comp.getChildren();
		if(children != null){
			for (UIComponent component : children) {
				UIComponent found = findComponent(component, id);
				if(found != null){
					return found;
				}
			}
		}
		return null;
	}
	
	public static String getViewRootId(){
		return FacesContext.getCurrentInstance().getViewRoot().getViewId();
	}
}
