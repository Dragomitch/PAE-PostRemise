package com.dragomitch.ipl.pae.main;

import com.dragomitch.ipl.pae.Application;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.ErrorManager;
import com.dragomitch.ipl.pae.logging.LogManager;
import com.dragomitch.ipl.pae.exceptions.FatalException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

  private static Logger logger = LogManager.getLogger(Main.class.getName());

  /**
   * Main program entry point.
   * 
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    String environment;
    if (args.length > 0 && args[1].equals(ContextManager.ENV_PRODUCTION)) {
      environment = ContextManager.ENV_PRODUCTION;
    } else {
      environment = ContextManager.ENV_DEVELOPMENT;
    }
    try {
      ContextManager.loadContext(environment);
      ErrorManager.load();
      Application.main(args);
    } catch (FatalException ex) {
      logger.log(Level.SEVERE, "Impossible to start basic configuration", ex);
    }
  }

}
