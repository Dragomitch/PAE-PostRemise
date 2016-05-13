package presentation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * Annotation that specifies the data to be saved in current session, based on the annotated method
 * return value.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Session {
  public static final String SET = "set";
  public static final String DELETE = "delete";

  /**
   * The action to be performed in Session, either 'set' to add data to the current session or
   * 'delete' to remove data from it.
   * 
   * @return the action as a string
   */
  String action();

  /**
   * The name of the attributes to be set/delete in the current session.
   * 
   * @return an array of attribute names
   */
  String[] attributeNames() default {};

  /**
   * The name of the field of the annotated method return value. These values will be set or deleted
   * in current session.
   * 
   * @return an array of field names
   */
  String[] fieldNames() default {};
}
