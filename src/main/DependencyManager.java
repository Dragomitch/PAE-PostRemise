package main;

import main.annotations.Inject;
import main.annotations.NoCache;
import main.exceptions.FatalException;
import main.logging.LogManager;
import utils.DataValidationUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class DependencyManager {

  private static Logger logger = LogManager.getLogger(DependencyManager.class.getName());
  private static Map<Class<?>, Object> cache = new HashMap<Class<?>, Object>();

  private DependencyManager() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns an instance of cl Class.
   * 
   * @param cl the desired Class
   * @return the instance of an object of the desired Class
   */
  @SuppressWarnings("unchecked")
  public static <E> E getInstance(Class<E> cl) {
    DataValidationUtils.checkObject(cl);
    Object instance;
    try {
      logger.info("Looking for an instance of " + cl.getName() + " Class in cache");
      String implName = ContextManager.getProperty(cl.getName());
      if (implName == null) {
        throw new FatalException(
            "Implementation class not found in property file: " + cl.getName());
      }
      instance = cache.get(Class.forName(implName));
    } catch (ClassNotFoundException ex) {
      throw new FatalException("Class not found:" + cl.getName(), ex);
    }
    if (instance == null) {
      logger.info("Instance not found in cache");
      instance = buildInstance(cl);
    } else {
      logger.info("Instance found in cache");
    }
    return (E) instance;
  }

  /**
   * Builds and returns a new instance of the Class cl.
   * 
   * @param cl the desired Class
   * @return a new instance of the desired Class
   */
  @SuppressWarnings("unchecked")
  private static <E> E buildInstance(Class<E> cl) {
    DataValidationUtils.checkObject(cl);
    logger.info("Building a new instance of " + cl.getName());
    String implName = ContextManager.getProperty(cl.getName());
    Class<?> implClass;
    try {
      implClass = Class.forName(implName);
    } catch (ClassNotFoundException ex) {
      throw new FatalException("Class not found: " + implName, ex);
    }
    Constructor<?> constructor = null;
    for (Constructor<?> entry : implClass.getDeclaredConstructors()) {
      if (entry.isAnnotationPresent(Inject.class)) {
        constructor = entry;
        break;
      }
    }
    Object instance;
    if (constructor == null) {
      try {
        constructor = implClass.getDeclaredConstructor();
      } catch (NoSuchMethodException | SecurityException ex) {
        throw new FatalException("Error while getting the constructor for class " + implName, ex);
      }
      constructor.setAccessible(true);
      try {
        instance = constructor.newInstance();
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
          | InvocationTargetException ex) {
        throw new FatalException("Impossible to create a new instance of " + implName, ex);
      }
    } else {
      constructor.setAccessible(true);
      Class<?>[] parameterTypes = constructor.getParameterTypes();
      Object[] parameters = new Object[parameterTypes.length];
      for (int i = 0; i < parameterTypes.length; i++) {
        parameters[i] = getInstance(parameterTypes[i]);
      }
      try {
        instance = constructor.newInstance(parameters);
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
          | InvocationTargetException ex) {
        throw new FatalException("Impossible to create a new instance: " + implName, ex);
      }
    }
    if (!implClass.isAnnotationPresent(NoCache.class)) {
      cache.put(implClass, instance);
    }
    return (E) instance;
  }


  /**
   * Removes all instances stored in cache. Only used in tests.
   */
  public static void cleanCache() {
    cache = new HashMap<Class<?>, Object>();
  }


}
