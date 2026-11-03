package com.dragomitch.ipl.pae.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dragomitch.ipl.pae.UnitTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestDocument {

  @Autowired private ApplicationContext context;

  private static final int ID = 1;
  private static final String NAME = "Contrat de bourse";
  private static final DocumentCategory CATEGORY = DocumentCategory.DEPARTURE;
  private static final int VERSION = 1;

  private Document document;

  /** Creates a new Document instance. */
  @BeforeEach
  public void setUp() throws Exception {
    this.document = new Document();
  }

  @Test
  public void testSetAndGetIdTC1() {
    document.setId(ID);
    assertEquals(ID, document.getId());
  }

  @Test
  public void testSetAndGetNameTC1() {
    document.setName(NAME);
    assertEquals(NAME, document.getName());
  }

  @Test
  public void testSetAndGetCategoryTC1() {
    document.setCategory(CATEGORY);
    assertEquals(CATEGORY, document.getCategory());
  }

  @Test
  public void testSetAndGetFilledInTC1() {
    document.setFilledIn(true);
    assertTrue(document.isFilledIn());
  }

  @Test
  public void testSetAndGetVersionTC1() {
    document.setVersion(VERSION);
    assertEquals(VERSION, document.getVersion());
  }
}
