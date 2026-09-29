package com.dragomitch.ipl.pae.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

class LogManagerTest {

  @Test
  void returnsTheSlf4jLoggerOfThatName() {
    assertThat(LogManager.getLogger("com.example.Foo").getName()).isEqualTo("com.example.Foo");
  }

  @Test
  void cannotBeInstantiated() throws Exception {
    Constructor<LogManager> constructor = LogManager.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    assertThatThrownBy(constructor::newInstance).isInstanceOf(InvocationTargetException.class)
        .cause().isInstanceOf(UnsupportedOperationException.class);
  }
}
