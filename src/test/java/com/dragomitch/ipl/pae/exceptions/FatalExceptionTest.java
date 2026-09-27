package com.dragomitch.ipl.pae.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

class FatalExceptionTest {

  private final SQLException cause = new SQLException("boom");

  @Test
  void isUnchecked() {
    assertThat(new FatalException()).isInstanceOf(RuntimeException.class).hasNoCause()
        .hasMessage(null);
  }

  @Test
  void keepsMessageAndCause() {
    assertThat(new FatalException(FatalException.DATABASE_ERROR_MSG))
        .hasMessage("Database error").hasNoCause();
    assertThat(new FatalException(cause)).hasCause(cause).hasMessageContaining("boom");
    assertThat(new FatalException(FatalException.LAZY_LOADING_ERROR_MSG, cause))
        .hasMessage("Operation not completed: instance is not fully loaded.").hasCause(cause);
  }
}
