package com.dragomitch.ipl.pae.persistence.mocks;

import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Empties every {@link ResettableMock} of the (cached, shared) test context before each test
 * method, before the test's own {@code @BeforeEach} fills them. Registered for every Spring test
 * in {@code META-INF/spring.factories}; contexts without mock DAOs are left untouched.
 */
public class MockDaoResetListener extends AbstractTestExecutionListener {

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

  @Override
  public void beforeTestMethod(TestContext testContext) {
    if (testContext.hasApplicationContext()) {
      testContext.getApplicationContext().getBeansOfType(ResettableMock.class).values()
          .forEach(ResettableMock::empty);
    }
  }
}
