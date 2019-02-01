package com.dragomitch.ipl.pae.presentation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface HttpHeader {
  /**
   * Returns the name of an HTTP header.
   * 
   * @return the name of an HTTP header.
   */
  String value();
}
