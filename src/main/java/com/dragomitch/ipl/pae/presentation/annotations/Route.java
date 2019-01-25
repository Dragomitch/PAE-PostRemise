package com.dragomitch.ipl.pae.presentation.annotations;

import com.dragomitch.ipl.pae.presentation.enums.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Route {
  /**
   * Returns an HTTP method: HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE
   * 
   * @return an HTTP method
   */
  HttpMethod method();

  /**
   * Returns a path template: /mobilities/{id} for example
   * 
   * @return a path template
   */
  String template() default "";

  String contentType() default "application/json";
}
