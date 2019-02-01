package com.dragomitch.ipl.pae.logging;

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Filter;
import java.util.logging.Logger;
import com.dragomitch.ipl.pae.exceptions.FatalException;

public class LogManager {

  private static Logger appLogger;
  private static Filter logFilter;

  static {
    logFilter = new LogFilter();
    appLogger = Logger.getLogger("appName"); // TODO Rendre dynamique
    appLogger.setUseParentHandlers(false);
    ConsoleHandler consoleHandler = new LogConsole();
    consoleHandler.setFormatter(new LogFormatter());
    appLogger.addHandler(consoleHandler);
    FileHandler fileHandler;
    try {
      File dir = new File("logs");
      if (!dir.exists()) {
        dir.mkdir();
      }
      fileHandler = new FileHandler("logs/default.log", true);
    } catch (SecurityException ex) {
      throw new FatalException("Caller does not have LoggingPermission(\"control\")", ex);
    } catch (IOException ex) {
      throw new FatalException("Error opening log file", ex);
    }
    appLogger.addHandler(fileHandler);
  }

  /**
   * Returns a class logger.
   *
   * @param className the name of the invoking class
   * @return the logger related to the class name
   */
  public static Logger getLogger(String className) {
    Logger logger = Logger.getLogger(className);
    logger.setParent(appLogger);
    logger.setFilter(logFilter);
    return logger;
  }

}
