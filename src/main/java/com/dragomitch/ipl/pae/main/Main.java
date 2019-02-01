package com.dragomitch.ipl.pae.main;

import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;
import com.dragomitch.ipl.pae.context.ErrorManager;
import com.dragomitch.ipl.pae.logging.LogManager;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.presentation.RoutingServlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

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
      Main app = new Main();
      app.startServer();
    } catch (FatalException ex) {
      logger.log(Level.SEVERE, "Impossible to start basic configuration", ex);
    }
  }

  /**
   * Starts Jetty server.
   */
  private void  startServer() {
    if (ContextManager.isInProduction()) {
      disableJettyLogging();
    }
    WebAppContext context = new WebAppContext();
    context.setContextPath("/");
    context.addServlet(new ServletHolder(new DefaultServlet()), "/*");
    context.addServlet(new ServletHolder(DependencyManager.getInstance(RoutingServlet.class)),
        "/api/1.0/*");
    context.setResourceBase("src/main/webapp"); //TODO Find a way to dynamicly load ressources from the JAR and other path(s)
    context.setInitParameter("cacheControl", "no-store, no-cache, must-revalidate");
    ErrorPageErrorHandler errorMapper = new ErrorPageErrorHandler();
    errorMapper.addErrorPage(404, "/");
    context.setErrorHandler(errorMapper);

    Server server = new Server(8080);
    server.setHandler(context);
    try {
      logger.info("Starting server");
      server.start();
    } catch (Exception ex) {
      throw new FatalException("Impossible to start server", ex);
    }
  }

  /**
   * Disables Jetty logging.
   */
  private void disableJettyLogging() {
    org.eclipse.jetty.util.log.Log.setLog(new org.eclipse.jetty.util.log.Logger() {
      @Override
      public String getName() {
        return "no";
      }

      @Override
      public void warn(String msg, Object... args) {}

      @Override
      public void warn(Throwable thrown) {}

      @Override
      public void warn(String msg, Throwable thrown) {}

      @Override
      public void info(String msg, Object... args) {}

      @Override
      public void info(Throwable thrown) {}

      @Override
      public void info(String msg, Throwable thrown) {}

      @Override
      public boolean isDebugEnabled() {
        return false;
      }

      @Override
      public void setDebugEnabled(boolean enabled) {}

      @Override
      public void debug(String msg, Object... args) {}

      @Override
      public void debug(Throwable thrown) {}

      @Override
      public void debug(String msg, Throwable thrown) {}

      @Override
      public void debug(String arg0, long arg1) {}

      @Override
      public org.eclipse.jetty.util.log.Logger getLogger(String name) {
        return this;
      }

      @Override
      public void ignore(Throwable ignored) {}

    });
  }

}
