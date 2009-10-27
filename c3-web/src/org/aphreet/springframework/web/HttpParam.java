package org.aphreet.springframework.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * HttpParam indicates that value of annotated field should be
 * set from HTTP parameters
 * @author Mikhail Malygin
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpParam {
	/**
	 * Set of names of HTTP parameters that will be set to annotated field
	 * If no value specified using field name
	 * @return
	 */
	String[] value() default "";
}
