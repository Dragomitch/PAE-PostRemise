package presentation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface HttpParameter {
  /**
   * Returns the name of an HTTP parameter. /mobilities/1/?reason=hello ---> reason is an HTTP
   * parameter
   * 
   * @return the name of an HTTP parameter
   */
  String value();
}
