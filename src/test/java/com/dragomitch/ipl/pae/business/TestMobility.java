package com.dragomitch.ipl.pae.business;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.Mobility;
import com.dragomitch.ipl.pae.business.NominatedStudent;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dragomitch.ipl.pae.business.dto.DocumentDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;
import com.dragomitch.ipl.pae.exceptions.FatalException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestMobility {

  private static final LocalDateTime SUBMISSION_DATE = LocalDateTime.now();
  private static final String STATE = Mobility.STATE_CREATED;
  private static final LocalDateTime FIRST_PAYMENT_REQUEST_DATE = LocalDateTime.now();
  private static final LocalDateTime SECOND_PAYMENT_REQUEST_DATE = LocalDateTime.now();

  private EntityFactory entityFactory;
  private Mobility mobility;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Creates a new Mobility instance and populates its "documents" attribute.
   */
  @Before
  public void setUp() throws Exception {
    entityFactory = (EntityFactory) DependencyManager.getInstance(EntityFactory.class);
    mobility = (Mobility) entityFactory.build(Mobility.class);
    // Populates documents
    List<DocumentDto> documents = new ArrayList<DocumentDto>();
    DocumentDto document1 = (DocumentDto) entityFactory.build(DocumentDto.class);
    documents.add(document1);
    document1.setId(1);
    document1.setFilledIn(false);
    document1.setCategory(DocumentDto.DEPARTURE_DOCUMENT);
    DocumentDto document2 = (DocumentDto) entityFactory.build(DocumentDto.class);
    documents.add(document2);
    document2.setId(2);
    document2.setFilledIn(false);
    document2.setCategory(DocumentDto.RETURN_DOCUMENT);
    mobility.setDocuments(documents);
  }

  @Test
  public void testSetAndGetSubmissionDateTC1() {
    mobility.setSubmissionDate(SUBMISSION_DATE);
    assertEquals(SUBMISSION_DATE, mobility.getSubmissionDate());
  }

  @Test
  public void testSetAndGetStateTC1() {
    mobility.setState(STATE);
    assertEquals(STATE, mobility.getState());
  }

  @Test
  public void testSetAndGetStateBeforeCancellationTC1() {
    mobility.setStateBeforeCancellation(STATE);
    assertEquals(STATE, mobility.getStateBeforeCancellation());
  }

  @Test
  public void testSetAndGetDocumentsTC1() {
    List<DocumentDto> documents = new ArrayList<DocumentDto>();
    mobility.setDocuments(documents);
    assertEquals(documents, mobility.getDocuments());
  }

  @Test
  public void testSetAndGetFirstPaymentRequestDateTC1() {
    mobility.setFirstPaymentRequestDate(FIRST_PAYMENT_REQUEST_DATE);
    assertEquals(FIRST_PAYMENT_REQUEST_DATE, mobility.getFirstPaymentRequestDate());
  }

  @Test
  public void testSetAndGetSecondPaymentRequestDateTC1() {
    mobility.setSecondPaymentRequestDate(SECOND_PAYMENT_REQUEST_DATE);
    assertEquals(SECOND_PAYMENT_REQUEST_DATE, mobility.getSecondPaymentRequestDate());
  }

  @Test
  public void testSetAndGetEncodedInSecondSoftwareTC1() {
    mobility.setProEcoEncoding(true);
    assertTrue(mobility.isEncodedInProEco());
  }

  @Test
  public void testSetAndGetSecondSoftwareEncodingTC1() {
    mobility.setSecondSoftwareEncoding(true);
    assertTrue(mobility.isEncodedInSecondSoftware());
  }

  @Test
  public void testSetAndGetProfessorInChargeTC1() {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    mobility.setProfessorInCharge(user);
    assertEquals(user, mobility.getProfessorInCharge());
  }

  @Test
  public void testGetAndSetNominatedStudentTC1() {
    NominatedStudent student = (NominatedStudent) entityFactory.build(NominatedStudent.class);
    mobility.setNominatedStudent(student);
    assertEquals(student, mobility.getNominatedStudent());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFillInDocumentTC1() {
    mobility.fillInDocument(0);
  }

  @Test(expected = FatalException.class)
  public void testFillInDocumentTC2() {
    mobility.setDocuments(null);
    mobility.fillInDocument(1);
  }

  @Test(expected = BusinessException.class)
  public void testFillInDocumentTC3() {
    mobility.fillInDocument(99);
  }

  @Test
  public void testFillInDocumentTC4() {
    assertTrue(mobility.fillInDocument(1));
  }

  @Test
  public void testFillInDocumentTC5() {
    mobility.getDocuments().get(0).setFilledIn(true);
    assertFalse(mobility.fillInDocument(1));
  }

  @Test
  public void testAllDocumentsFilledInTC1() {
    assertFalse(mobility.allDocumentsFilledIn());
  }

  @Test
  public void testAllDocumentsFilledInTC2() {
    mobility.getDocuments().get(0).setFilledIn(true);
    mobility.getDocuments().get(1).setFilledIn(true);
    assertTrue(mobility.allDocumentsFilledIn());
  }

  @Test
  public void testAllDepartureDocumentsFilledInTC1() {
    assertFalse(mobility.allDepartureDocumentsFilledIn());
  }

  @Test
  public void testAllDepartureDocumentsFilledInTC2() {
    mobility.getDocuments().get(0).setFilledIn(true);
    assertTrue(mobility.allDepartureDocumentsFilledIn());
  }

  @Test
  public void testAllReturnDocumentsFilledInTC1() {
    assertFalse(mobility.allReturnDocumentsFilledIn());
  }

  @Test
  public void testAllReturnDocumentsFilledInTC2() {
    mobility.getDocuments().get(1).setFilledIn(true);
    assertTrue(mobility.allReturnDocumentsFilledIn());
  }

  @Test(expected = BusinessException.class)
  public void testCheckAllDocumentsFilledInTC1() {
    mobility.checkAllDocumentsFilledIn();
  }

  @Test
  public void testCheckAllDocumentsFilledInTC2() {
    mobility.getDocuments().get(0).setFilledIn(true);
    mobility.getDocuments().get(1).setFilledIn(true);
    mobility.checkAllDocumentsFilledIn();
  }

  @Test(expected = BusinessException.class)
  public void testCheckAllDepartureDocumentsFilledInTC1() {
    mobility.checkAllDepartureDocumentsFilledIn();
  }

  @Test
  public void testCheckAllDepartureDocumentsFilledInTC2() {
    mobility.getDocuments().get(0).setFilledIn(true);
    mobility.checkAllDepartureDocumentsFilledIn();
  }

  @Test(expected = BusinessException.class)
  public void testCheckAllReturnDocumentsFilledInTC1() {
    mobility.checkAllReturnDocumentsFilledIn();
  }

  @Test
  public void testCheckAllReturnDocumentsFilledInTC2() {
    mobility.getDocuments().get(1).setFilledIn(true);
    mobility.checkAllReturnDocumentsFilledIn();
  }

  @Test
  public void testCheckCancelledOrClosedTC1() {
    mobility.setState(Mobility.STATE_IN_PROGRESS);
    mobility.checkNotCancelledAndNotClosed();
  }


  @Test(expected = BusinessException.class)
  public void testCheckCancelledOrClosedTC2() {
    mobility.setState(Mobility.STATE_CANCELLED);
    mobility.checkNotCancelledAndNotClosed();
  }

  @Test(expected = BusinessException.class)
  public void testCheckCancelledOrClosedTC3() {
    mobility.setState(Mobility.STATE_CLOSED);
    mobility.checkNotCancelledAndNotClosed();
  }
}
