package org.aphreet.springframework.web;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.web.context.WebApplicationContext;

/**
 * Bean factory that sets bean properties with {@link HttpParam} annotation
 * @author Mikhail Malygin
 *
 */
public class FacesListableBeanFactory extends DefaultListableBeanFactory{

	@SuppressWarnings("unused")
	private final Log logger = LogFactory.getLog(getClass());
	
	public FacesListableBeanFactory(){
		super();
	}
	
	public FacesListableBeanFactory(BeanFactory parent){
		super(parent);
	}
	
	protected void populateBean(String beanName, AbstractBeanDefinition mbd, BeanWrapper bw) {
		PropertyValues pvs = mbd.getPropertyValues();
		
		if (bw == null) {
			if (!pvs.isEmpty()) {
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
			}
			else {
				// Skip property population phase for null instance.
				return;
			}
		}

		super.populateBean(beanName, mbd, bw);
		
		if(mbd.getScope().equals(WebApplicationContext.SCOPE_REQUEST)){
			setHttpParams(bw);
		}
	}
	
	private void setHttpParams(BeanWrapper bw) {
		
		FacesContext context = FacesContext.getCurrentInstance();
		
		if(context != null){

			HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

			List<PropertyValue> pwList = new ArrayList<PropertyValue>();

			List<Field> fields = getAllAnnotatedFields(bw.getWrappedClass());

			for (Field field : fields) {
				HttpParam param = field.getAnnotation(HttpParam.class);
				String fieldName = field.getName();
				boolean isNameExist = false;

				String[] names = param.value();
				for (String paramName : names) {
					if(paramName.length() > 0){

						String paramValue = request.getParameter(paramName);
						if(paramValue != null){
							pwList.add(new PropertyValue(fieldName, paramValue));
							isNameExist= true;
							break;
						}

					}
				}
				if(!isNameExist){
					//set field by name
					pwList.add(new PropertyValue(fieldName, request.getParameter(fieldName)));
				}
			}

			for (PropertyValue propertyValue : pwList) {
				try{
					bw.setPropertyValue(propertyValue);
				}catch (TypeMismatchException e) {
					//e.printStackTrace();
				}
			}
		}
	}
	
	private List<Field> getAllAnnotatedFields(Class<?> clazz){
		ArrayList<Field> annotatedFields = new ArrayList<Field>();
		
		while(clazz != null){
		
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				if(field.getAnnotation(HttpParam.class) != null){
					annotatedFields.add(field);
				}
			}
			clazz = clazz.getSuperclass();
		}
		
		return annotatedFields;
	}

}
