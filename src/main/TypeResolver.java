package main;

import main.exceptions.FatalException;

import java.lang.reflect.Type;

/**
 * Utility to resolve raw types.
 */
public final class TypeResolver {

  /**
   * Returns the raw class for {@code type} argument.
   * 
   * @param type to resolve from
   * @return raw class implementing {@code type}
   */
  public static Class<?> resolve(Type type) {
    String implName = ContextManager.getProperty(type.getTypeName());
    if (implName != null) {
      try {
        Class<?> klass = Class.forName(implName);
        return klass;
      } catch (ClassNotFoundException ex) {
        throw new FatalException("Class not found: " + implName);
      }
    }
    return null;
  }

}
