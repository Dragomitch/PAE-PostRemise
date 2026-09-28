package com.dragomitch.ipl.pae.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.presentation.annotations.ApiCollection;
import com.dragomitch.ipl.pae.presentation.annotations.Route;
import com.dragomitch.ipl.pae.presentation.enums.HttpMethod;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Route precedence must not depend on the order the controllers are registered in (the servlet
 * registers them from a HashSet): a literal segment wins over a {placeholder}.
 */
class RouteResolverTest {

  @ApiCollection(name = "Things", endpoint = "/things")
  static class ThingController {
    @Route(method = HttpMethod.GET, template = "/{id}")
    public void one() {
    }

    @Route(method = HttpMethod.PUT, template = "/{id}")
    public void edit() {
    }
  }

  @ApiCollection(name = "Things count", endpoint = "/things")
  static class ThingCountController {
    @Route(method = HttpMethod.GET, template = "/count")
    public void count() {
    }
  }

  @ApiCollection(name = "Things again", endpoint = "/things")
  static class DuplicateThingController {
    @Route(method = HttpMethod.GET, template = "/{id}")
    public void another() {
    }
  }

  private static RouteResolver resolver(Object... controllers) {
    Set<Object> set = new LinkedHashSet<>(List.of(controllers));
    RouteResolver resolver = new RouteResolver();
    resolver.initializeRoutes(set);
    return resolver;
  }

  @Test
  void aLiteralSegmentWinsOverAPlaceholderWhateverTheRegistrationOrder() {
    for (RouteResolver resolver : List.of(
        resolver(new ThingController(), new ThingCountController()),
        resolver(new ThingCountController(), new ThingController()))) {
      assertThat(resolver.findRoute(HttpMethod.GET, "/things/count").getImplMethod().getName())
          .isEqualTo("count");
      assertThat(resolver.findRoute(HttpMethod.GET, "/things/42").getImplMethod().getName())
          .isEqualTo("one");
      assertThat(resolver.findRoute(HttpMethod.PUT, "/things/count").getImplMethod().getName())
          .isEqualTo("edit");
    }
  }

  @Test
  void theRoutesAreOrderedByMethodThenTemplate() {
    RouteResolver resolver = resolver(new ThingController(), new ThingCountController());

    assertThat(resolver.dumpRoutes()).containsSubsequence("GET /things/count",
        "GET /things/{id}", "PUT /things/{id}");
  }

  @Test
  void twoRoutesWithTheSameMethodAndTemplateAreRejected() {
    assertThatThrownBy(() -> resolver(new ThingController(), new DuplicateThingController()))
        .isInstanceOf(FatalException.class).hasMessageContaining("GET /things/{id}");
  }
}
