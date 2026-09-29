package com.dragomitch.ipl.pae.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.dragomitch.ipl.pae.presentation.annotations.ApiCollection;
import com.dragomitch.ipl.pae.presentation.annotations.HttpBody;
import com.dragomitch.ipl.pae.presentation.annotations.HttpHeader;
import com.dragomitch.ipl.pae.presentation.annotations.HttpParameter;
import com.dragomitch.ipl.pae.presentation.annotations.PathParameter;
import com.dragomitch.ipl.pae.presentation.annotations.Route;
import com.dragomitch.ipl.pae.presentation.annotations.SessionParameter;
import com.dragomitch.ipl.pae.uccontrollers.SessionUcc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

/**
 * Static checks of the {@link Route} methods of the use-case controllers: the {@link Invoker}
 * can only call a method whose parameters are all bound to a request value, and a
 * {@code @PathParameter} must name a placeholder of its template. A mistake is otherwise only
 * noticed at run time, as a 500 on that route.
 */
class RouteDeclarationsTest {

  private static final List<Class<? extends Annotation>> BINDINGS = List.of(PathParameter.class,
      HttpParameter.class, HttpHeader.class, SessionParameter.class, HttpBody.class);

  static List<Method> routeMethods() {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Service.class));
    List<Method> methods = new ArrayList<>();
    for (BeanDefinition definition : scanner
        .findCandidateComponents("com.dragomitch.ipl.pae.uccontrollers.implementations")) {
      Class<?> type = ClassUtils.resolveClassName(definition.getBeanClassName(), null);
      Stream.of(type.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Route.class))
          .forEach(methods::add);
    }
    return methods;
  }

  static String template(Method method) {
    ApiCollection collection = method.getDeclaringClass().getAnnotation(ApiCollection.class);
    return (collection == null ? "" : collection.endpoint())
        + method.getAnnotation(Route.class).template();
  }

  @Test
  void theUseCaseControllersDeclareRoutes() {
    assertThat(routeMethods()).hasSizeGreaterThan(30);
  }

  @Test
  void everyParameterOfARouteIsBoundToExactlyOneRequestValue() {
    List<String> unbound = new ArrayList<>();
    for (Method method : routeMethods()) {
      for (Parameter parameter : method.getParameters()) {
        long bindings = BINDINGS.stream().filter(parameter::isAnnotationPresent).count();
        if (bindings != 1) {
          unbound.add(method.getDeclaringClass().getSimpleName() + "." + method.getName() + "("
              + parameter.getType().getSimpleName() + ")");
        }
      }
    }

    assertThat(unbound).isEmpty();
  }

  @Test
  void everySessionParameterIsAnAttributeTheSessionHolds() {
    // signin stores exactly these two attributes (see SessionUcc)
    List<String> unknown = new ArrayList<>();
    for (Method method : routeMethods()) {
      for (Parameter parameter : method.getParameters()) {
        SessionParameter session = parameter.getAnnotation(SessionParameter.class);
        if (session != null && !List.of(SessionUcc.USER_ID, SessionUcc.USER_ROLE)
            .contains(session.value())) {
          unknown.add(method.getDeclaringClass().getSimpleName() + "." + method.getName() + ": "
              + session.value());
        }
      }
    }

    assertThat(unknown).isEmpty();
  }

  @Test
  void everyPathParameterNamesAPlaceholderOfTheTemplate() {
    List<String> mismatches = new ArrayList<>();
    for (Method method : routeMethods()) {
      for (Parameter parameter : method.getParameters()) {
        PathParameter path = parameter.getAnnotation(PathParameter.class);
        if (path != null && !template(method).contains("{" + path.value() + "}")) {
          mismatches.add(method.getName() + ": {" + path.value() + "} in " + template(method));
        }
      }
    }

    assertThat(mismatches).isEmpty();
  }
}
