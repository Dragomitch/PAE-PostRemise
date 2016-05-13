package main;

import main.exceptions.FatalException;
import main.logging.LogManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

public class ContextManager {

  private static Logger logger = LogManager.getLogger(ContextManager.class.getName());

  // Environments
  public static final String ENV_PRODUCTION = "prod";
  public static final String ENV_DEVELOPMENT = "dev";
  public static final String ENV_TEST = "test";

  // Database credentials
  public static final String DB_DRIVER_CLASS = "driver_class";
  public static final String DB_SCHEMA = "db_schema";
  public static final String DB_NAME = "database";
  public static final String DB_HOST = "host";
  public static final String DB_PORT = "port";
  public static final String DB_USERNAME = "username";
  public static final String DB_PASSWORD = "password";
  public static final String ENV_ERRORS = "error_file";

  private static Properties properties;
  private static String environment;

  private ContextManager() {
    throw new UnsupportedOperationException();
  }

  /**
   * Loads properties based on the application context.
   * 
   * @param environment the current running environment
   */
  public static void loadContext(String environment) {
    ContextManager.environment = environment;
    ContextManager.properties = new Properties();
    logger.info("Loading context: running in " + environment + " environment");
    try (FileInputStream fin = new FileInputStream(environment + ".properties")) {
      properties.load(fin);
    } catch (FileNotFoundException ex) {
      throw new FatalException("Properties file not found: " + environment + ".properties", ex);
    } catch (IOException ex) {
      throw new FatalException(
          "I/O Error while reading properties file: " + environment + ".properties", ex);
    }
  }

  /**
   * Gets the application environment.
   * 
   * @return the current running environment
   */
  public static String getEnvironment() {
    return environment;
  }

  public static boolean isInProduction() {
    return environment.equals(ENV_PRODUCTION);
  }

  /**
   * Returns an application property.
   * 
   * @param key the key whose associated value is to be returned
   * @return the property to which the specified key is mapped, or null if the property isn't set
   */
  public static String getProperty(String key) {
    return properties.getProperty(key);
  }

}
