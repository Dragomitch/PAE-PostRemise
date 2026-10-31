package com.dragomitch.ipl.pae.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * What remains of {@link DataValidationUtils} (used by a JDBC DAO). The former format checks
 * (IBAN, BIC, email, phone number) are tested as Bean Validation constraints in
 * {@code CustomConstraintsTest}.
 */
class DataValidationUtilsTest {

  @ParameterizedTest
  @ValueSource(strings = {"hello", " ", "x"})
  void aNonEmptyStringIsValid(String value) {
    assertThat(DataValidationUtils.isAValidString(value)).isTrue();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void aNullOrEmptyStringIsNotValid(String value) {
    assertThat(DataValidationUtils.isAValidString(value)).isFalse();
  }

  @Test
  void theUtilityClassCannotBeInstantiated() throws Exception {
    Constructor<DataValidationUtils> constructor =
        DataValidationUtils.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    assertThatThrownBy(constructor::newInstance).isInstanceOf(InvocationTargetException.class)
        .cause().isInstanceOf(UnsupportedOperationException.class);
  }
}
