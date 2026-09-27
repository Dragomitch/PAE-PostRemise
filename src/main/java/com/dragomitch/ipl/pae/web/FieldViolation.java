package com.dragomitch.ipl.pae.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * One entry of the {@code errors} property of a {@code VALIDATION_FAILED} problem.
 *
 * @param field the path of the invalid value, relative to the request body or the name of the
 *     request parameter ({@code email}, {@code address.city}, {@code options[0].code}, {@code id})
 * @param code the name of the broken constraint ({@code NotBlank}, {@code Size}, {@code Iban}...)
 * @param message the localized message, displayable to the user
 */
public record FieldViolation(String field, String code, String message) {

  private static final String UNKNOWN_CODE = "Invalid";

  /**
   * The violations of a request body or of a model attribute ({@code MethodArgumentNotValidException}).
   *
   * @param errors the binding and validation errors
   * @return one violation per error, in the order of the errors
   */
  static List<FieldViolation> of(Errors errors) {
    List<FieldViolation> violations = new ArrayList<>();
    for (ObjectError error : errors.getAllErrors()) {
      String field = error instanceof FieldError fieldError
          ? fieldError.getField() : error.getObjectName();
      violations.add(new FieldViolation(field, codeOf(error), error.getDefaultMessage()));
    }
    return violations;
  }

  /**
   * The violations found by Spring MVC's method validation of a controller
   * ({@code HandlerMethodValidationException}): constraints on {@code @PathVariable} or
   * {@code @RequestParam} parameters, or on a request body validated together with them.
   *
   * @param ex the exception
   * @return one violation per error
   */
  static List<FieldViolation> of(HandlerMethodValidationException ex) {
    List<FieldViolation> violations = new ArrayList<>();
    for (ParameterValidationResult result : ex.getAllValidationResults()) {
      if (result instanceof ParameterErrors errors) {
        violations.addAll(of(errors));
        continue;
      }
      String parameter = parameterName(result.getMethodParameter());
      for (MessageSourceResolvable error : result.getResolvableErrors()) {
        violations.add(new FieldViolation(parameter, codeOf(error), error.getDefaultMessage()));
      }
    }
    return violations;
  }

  /**
   * The violations of a Bean Validation {@code ConstraintViolationException}, typically raised
   * by the method validation of a use case: the method and parameter nodes are left out of the
   * path, so that {@code signup.user.email} becomes {@code email} and
   * {@code promoteToProfessor.userId} becomes {@code userId}.
   *
   * @param violations the constraint violations
   * @return one violation per constraint violation, sorted by field
   */
  static List<FieldViolation> of(Iterable<? extends ConstraintViolation<?>> violations) {
    List<FieldViolation> result = new ArrayList<>();
    for (ConstraintViolation<?> violation : violations) {
      result.add(new FieldViolation(fieldOf(violation.getPropertyPath()),
          violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
          violation.getMessage()));
    }
    result.sort((a, b) -> a.field().compareTo(b.field()));
    return result;
  }

  static String fieldOf(Path path) {
    List<Path.Node> nodes = new ArrayList<>();
    path.forEach(nodes::add);
    int start = 0;
    if (!nodes.isEmpty() && (nodes.get(0).getKind() == ElementKind.METHOD
        || nodes.get(0).getKind() == ElementKind.CONSTRUCTOR)) {
      start = 1;
      // a parameter followed by properties: the path is relative to the parameter
      if (nodes.size() > 2 && nodes.get(1).getKind() == ElementKind.PARAMETER) {
        start = 2;
      }
    }
    StringBuilder field = new StringBuilder();
    Iterator<Path.Node> iterator = nodes.subList(start, nodes.size()).iterator();
    while (iterator.hasNext()) {
      Path.Node node = iterator.next();
      if (node.isInIterable()) {
        Object index = node.getIndex() != null ? node.getIndex() : node.getKey();
        field.append('[').append(index == null ? "" : index).append(']');
      }
      if (node.getName() != null && node.getKind() != ElementKind.CROSS_PARAMETER
          && node.getKind() != ElementKind.RETURN_VALUE) {
        if (!field.isEmpty()) {
          field.append('.');
        }
        field.append(node.getName());
      }
    }
    return field.toString();
  }

  private static String parameterName(MethodParameter parameter) {
    RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
    if (requestParam != null && !(requestParam.name() + requestParam.value()).isEmpty()) {
      return requestParam.name().isEmpty() ? requestParam.value() : requestParam.name();
    }
    PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
    if (pathVariable != null && !(pathVariable.name() + pathVariable.value()).isEmpty()) {
      return pathVariable.name().isEmpty() ? pathVariable.value() : pathVariable.name();
    }
    parameter.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
    String name = parameter.getParameterName();
    return name != null ? name : "arg" + parameter.getParameterIndex();
  }

  /** The most generic of the message codes Spring built for an error: the constraint name. */
  private static String codeOf(MessageSourceResolvable error) {
    String[] codes = error.getCodes();
    return codes == null || codes.length == 0 ? UNKNOWN_CODE : codes[codes.length - 1];
  }
}
