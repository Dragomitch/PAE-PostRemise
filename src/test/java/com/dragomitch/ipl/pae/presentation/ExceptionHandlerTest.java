package com.dragomitch.ipl.pae.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.business.EntityFactory;

import java.util.ConcurrentModificationException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
class ExceptionHandlerTest {

  @Autowired
  private EntityFactory entityFactory;

  @Test
  void aConcurrentModificationAnswers400WithTheCatalogueMessage() throws Exception {
    ExceptionHandler handler = new ExceptionHandler(new JsonSerializer(entityFactory));
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.handleException(new ConcurrentModificationException(), response);

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentAsString()).contains("\"errorCode\":120")
        .contains("modified or deleted in the meantime");
  }
}
