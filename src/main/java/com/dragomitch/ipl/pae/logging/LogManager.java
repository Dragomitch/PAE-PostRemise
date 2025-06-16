package com.dragomitch.ipl.pae.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LogManager {

  private LogManager() {
    throw new UnsupportedOperationException();
  }

  public static Logger getLogger(String className) {
    return LoggerFactory.getLogger(className);
  }
}
