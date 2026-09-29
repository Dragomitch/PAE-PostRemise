package com.dragomitch.ipl.pae.business.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Every error code declared in {@link ErrorFormat} has an entry in errors.json: a
 * {@link BusinessException} with an unknown code cannot build its error (the web layer then
 * answered 500 instead of 400).
 */
class ErrorCatalogueTest {

  private static List<Field> errorCodes() {
    return Stream.of(ErrorFormat.class.getDeclaredFields())
        .filter(f -> Modifier.isStatic(f.getModifiers()) && f.getType() == int.class).toList();
  }

  @Test
  void everyDeclaredErrorCodeIsInTheCatalogue() throws IllegalAccessException {
    assertThat(errorCodes()).hasSizeGreaterThan(100);
    for (Field field : errorCodes()) {
      int code = field.getInt(null);
      ErrorFormat error = ErrorManager.getError(code);

      assertThat(error).as(field.getName()).isNotNull();
      assertThat(error.getErrorCode()).as(field.getName()).isEqualTo(code);
      assertThat(error.getDeveloperMessage()).as(field.getName()).isNotBlank();
    }
  }

  @Test
  void aBusinessExceptionOfTheFiltersBuildsItsError() {
    assertThat(new BusinessException(ErrorFormat.INVALID_PARTNER_FILTER_709).getError()
        .getErrorCode()).isEqualTo(709);
    assertThat(new BusinessException(ErrorFormat.INVALID_DOCUMENT_FILTER_508).getError()
        .getErrorCode()).isEqualTo(508);
  }
}
