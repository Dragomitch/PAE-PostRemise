package presentation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ApiCollection {
  /**
   * @return the collection name
   */
  String name();

  /**
   * @return the collection endpoint
   */
  String endpoint();
}
