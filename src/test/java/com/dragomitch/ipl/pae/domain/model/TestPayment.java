package com.dragomitch.ipl.pae.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.UnitTestConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestPayment {

  @Autowired private ApplicationContext context;

  private static final int MOBILITYCHOICEID = 1;
  private static final MobilityType MOBILITYTYPE = MobilityType.SMP;
  private static final String ACADEMICYEAR = "";
  private static final int TERM = 1;
  private static final LocalDateTime PAYMENTDATE = LocalDateTime.of(2016, 12, 26, 5, 55);
  private static final String PAYMENTTYPE = "";
  private static final int VERSION = 1;

  private Payment payment;

  /** Creates a new Payment instance. */
  @BeforeEach
  public void setUp() throws Exception {
    this.payment = new Payment();
  }

  @Test
  public void testSetAndGetMobilityChoiceIdTC1() {
    payment.setMobilityChoiceId(MOBILITYCHOICEID);
    assertEquals(MOBILITYCHOICEID, payment.getMobilityChoiceId());
  }

  @Test
  public void testSetAndGetUserTC1() {
    User user = new User();
    payment.setUser(user);
    assertEquals(user, payment.getUser());
  }

  @Test
  public void testSetAndGetNominatedStudentTC1() {
    NominatedStudent nominatedStudent = new NominatedStudent();
    payment.setNominatedStudent(nominatedStudent);
    assertEquals(nominatedStudent, payment.getNominatedStudent());
  }

  @Test
  public void testSetAndGetMobilityTypeTC1() {
    payment.setMobilityType(MOBILITYTYPE);
    assertEquals(MOBILITYTYPE, payment.getMobilityType());
  }

  @Test
  public void testSetAndGetAcademicYearTC1() {
    payment.setAcademicYear(ACADEMICYEAR);
    assertEquals(ACADEMICYEAR, payment.getAcademicYear());
  }

  @Test
  public void testSetAndGetTermTC1() {
    payment.setTerm(TERM);
    assertEquals(TERM, payment.getTerm());
  }

  @Test
  public void testSetAndGetProgrammeTC1() {
    Programme programme = new Programme();
    payment.setProgramme(programme);
    assertEquals(programme, payment.getProgramme());
  }

  @Test
  public void testSetAndGetCountryTC1() {
    Country country = new Country();
    payment.setCountry(country);
    assertEquals(country, payment.getCountry());
  }

  @Test
  public void testSetAndGetPaymentDateTC1() {
    payment.setPaymentDate(PAYMENTDATE);
    assertEquals(PAYMENTDATE, LocalDateTime.of(2016, 12, 26, 5, 55));
  }

  @Test
  public void testSetAndGetPaymentTypeTC1() {
    payment.setPaymentType(PAYMENTTYPE);
    assertEquals(PAYMENTTYPE, payment.getPaymentType());
  }

  @Test
  public void testSetAndGetPartnerTC1() {
    Partner partner = new Partner();
    payment.setPartner(partner);
    assertEquals(partner, payment.getPartner());
  }
}
