package com.dragomitch.ipl.pae.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.dragomitch.ipl.pae.business.Violations;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;

/** How the paths of the broken constraints become the {@code field} of the problem errors. */
class FieldViolationTest {

  /** A use case whose parameters are validated. */
  static class Service {
    public void rename(@Positive int id, @NotBlank String name) {
    }

    public void save(@Valid Partner partner) {
    }
  }

  record Option(@NotBlank String code) {
  }

  record Address(@NotBlank String city) {
  }

  record Partner(@NotNull @Valid Address address, @Valid List<Option> options) {
  }

  @Test
  void aParameterIsReportedUnderItsName() throws Exception {
    Method rename = Service.class.getMethod("rename", int.class, String.class);
    Set<ConstraintViolation<Service>> violations = Violations.validator().forExecutables()
        .validateParameters(new Service(), rename, new Object[] {0, " "});

    assertThat(FieldViolation.of(violations))
        .extracting(FieldViolation::field, FieldViolation::code)
        .containsExactly(tuple(rename.getParameters()[0].getName(), "Positive"),
            tuple(rename.getParameters()[1].getName(), "NotBlank"));
  }

  @Test
  void thePathOfABeanParameterIsRelativeToTheBean() throws Exception {
    Method save = Service.class.getMethod("save", Partner.class);
    Partner partner = new Partner(new Address(""), List.of(new Option("BIN"), new Option(" ")));
    Set<ConstraintViolation<Service>> violations = Violations.validator().forExecutables()
        .validateParameters(new Service(), save, new Object[] {partner});

    assertThat(FieldViolation.of(violations)).extracting(FieldViolation::field)
        .containsExactly("address.city", "options[1].code");
  }

  @Test
  void theIndexOfAListElementIsKeptForABean() {
    Partner partner = new Partner(null, List.of(new Option("")));

    assertThat(FieldViolation.of(Violations.validator().validate(partner)))
        .extracting(FieldViolation::field, FieldViolation::code)
        .containsExactly(tuple("address", "NotNull"), tuple("options[0].code", "NotBlank"));
  }

  @Test
  void aBindingResultGivesTheFieldAndTheMostGenericCode() {
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new Object(), "search");
    errors.reject("Invalid", "whole object");

    assertThat(FieldViolation.of(errors))
        .extracting(FieldViolation::field, FieldViolation::code, FieldViolation::message)
        .containsExactly(tuple("search", "Invalid", "whole object"));
  }
}
