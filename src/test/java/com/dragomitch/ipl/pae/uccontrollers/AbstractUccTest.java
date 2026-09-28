package com.dragomitch.ipl.pae.uccontrollers;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.persistence.mocks.ResettableMock;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Base class of the use-case tests: the real use cases wired to the in-memory DAOs of
 * {@link UnitTestConfig}. The context (and so the mocks) is shared by every test class, so every
 * test starts from empty mocks whatever ran before it, in any class or method order. (JUnit runs
 * this {@code @BeforeEach} before those of the subclasses, which may then fill the mocks.)
 */
@SpringJUnitConfig(UnitTestConfig.class)
public abstract class AbstractUccTest {

  @Autowired
  private ApplicationContext mockContext;

  @BeforeEach
  void resetMockDaos() {
    ResettableMock.resetAll(mockContext);
  }
}
