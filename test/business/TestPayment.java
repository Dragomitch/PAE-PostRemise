package business;

import static org.junit.Assert.assertEquals;

import business.dto.CountryDto;
import business.dto.NominatedStudentDto;
import business.dto.PartnerDto;
import business.dto.PaymentDto;
import business.dto.ProgrammeDto;
import business.dto.UserDto;
import main.ContextManager;
import main.DependencyManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDateTime;

public class TestPayment {

  private static final int MOBILITYCHOICEID = 1;
  private static final String MOBILITYTYPE = "";
  private static final String ACADEMICYEAR = "";
  private static final int TERM = 1;
  private static final LocalDateTime PAYMENTDATE = LocalDateTime.of(2016, 12, 26, 5, 55);
  private static final String PAYMENTTYPE = "";
  private static final int VERSION = 1;

  private EntityFactory entityFactory;
  private PaymentDto payment;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Creates a new Payment instance.
   */
  @Before
  public void setUp() throws Exception {
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
    this.payment = (PaymentDto) entityFactory.build(PaymentDto.class);
  }

  @Test
  public void testSetAndGetMobilityChoiceIdTC1() {
    payment.setMobilityChoiceId(MOBILITYCHOICEID);
    assertEquals(MOBILITYCHOICEID, payment.getMobilityChoiceId());
  }

  @Test
  public void testSetAndGetUserTC1() {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    payment.setUser(user);
    assertEquals(user, payment.getUser());
  }

  @Test
  public void testSetAndGetNominatedStudentTC1() {
    NominatedStudentDto nominatedStudent =
        (NominatedStudentDto) entityFactory.build(NominatedStudentDto.class);
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
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    payment.setProgramme(programme);
    assertEquals(programme, payment.getProgramme());
  }

  @Test
  public void testSetAndGetCountryTC1() {
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
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
    PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
    payment.setPartner(partner);
    assertEquals(partner, payment.getPartner());
  }


  @Test
  public void testSetAndGetVersionTC1() {
    payment.setVersion(VERSION);
    assertEquals(VERSION, payment.getVersion());
  }
}
