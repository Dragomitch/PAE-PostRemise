package com.dragomitch.ipl.pae.presentation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface PathParameter {
  /**
   * Returns the name of a path parameter. /mobilities/{id} ---> id is a path parameter
   * 
   * @return the name of a path parameter
   */
  String value();
}
