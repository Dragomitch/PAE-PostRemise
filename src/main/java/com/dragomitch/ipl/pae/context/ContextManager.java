package com.dragomitch.ipl.pae.context;

import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.logging.LogManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;

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
    private static String absolutePath;

    private ContextManager() {
        throw new UnsupportedOperationException();
    }

    /**
     * Loads properties based on the application context.
     *
     * @param environmentFilePath the current running environmentFilePath
     */
    public static void loadContext(String environmentFilePath) {
        ContextManager.environment = environmentFilePath;
        ContextManager.properties = new Properties();

        String fileName = environmentFilePath + ".properties";
        String propertiesPath = getCurrentAbsolutePath();
        logger.info("Loading context: running in " + propertiesPath + environmentFilePath);

        try (InputStream in = openResource(fileName)) {
            properties.load(in);
        } catch (FileNotFoundException ex) {
            throw new FatalException("Properties file not found: " + propertiesPath + fileName, ex);
        } catch (IOException ex) {
            throw new FatalException("I/O Error while reading properties file: " + propertiesPath + fileName, ex);
        }
    }

    /**
     * Opens a configuration resource. A file placed next to the application (in the launch
     * directory) takes precedence so it can be overridden at deployment time; otherwise the
     * resource is read from the classpath.
     *
     * @param fileName the name of the resource to open
     * @return an open stream on the resource
     * @throws FileNotFoundException if the resource exists neither on disk nor on the classpath
     */
    public static InputStream openResource(String fileName) throws FileNotFoundException {
        File external = new File(getCurrentAbsolutePath() + fileName);
        if (external.isFile()) {
            return new FileInputStream(external);
        }
        InputStream in = ContextManager.class.getClassLoader().getResourceAsStream(fileName);
        if (in == null) {
            throw new FileNotFoundException(fileName);
        }
        return in;
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

    /**
     * Returns the complete path where the executable is launched
     * This allow to bind different properties files to the program on Runtime
     *
     * @return the absolute path of the application launch.
     */
    public static String getCurrentAbsolutePath() {
        if (ContextManager.absolutePath == null) {
            File jarPath = new File(ContextManager.class.getProtectionDomain().getCodeSource().getLocation().getPath());
            ContextManager.absolutePath = jarPath.getParentFile().getAbsolutePath() + '/';
        }
        return ContextManager.absolutePath;
    }

}
