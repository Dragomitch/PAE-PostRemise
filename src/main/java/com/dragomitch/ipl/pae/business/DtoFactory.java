package com.dragomitch.ipl.pae.business;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class DtoFactory {

  private final ApplicationContext context;

  public DtoFactory(ApplicationContext context) {
    this.context = context;
  }

  public <T> T create(Class<T> clazz) {
    return context.getBean(clazz);
  }
}
