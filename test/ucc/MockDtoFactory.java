package ucc;

import business.DenialReason;
import business.EntityFactory;
import business.dto.AddressDto;
import business.dto.CountryDto;
import business.dto.DenialReasonDto;
import business.dto.DocumentDto;
import business.dto.MobilityChoiceDto;
import business.dto.MobilityDto;
import business.dto.NominatedStudentDto;
import business.dto.OptionDto;
import business.dto.PartnerDto;
import business.dto.PartnerOptionDto;
import business.dto.PaymentDto;
import business.dto.ProgrammeDto;
import business.dto.UserDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MockDtoFactory {

  private EntityFactory entityFactory;

  MockDtoFactory(EntityFactory entityFactory) {
    this.entityFactory = entityFactory;
  }

  /**
   * Return a MobilityChoice mock object.
   * 
   * @return a mobilityChoice
   */
  public MobilityChoiceDto getMobilityChoice() {
    MobilityChoiceDto mobilityChoice =
        (MobilityChoiceDto) entityFactory.build(MobilityChoiceDto.class);
    mobilityChoice.setId(1);
    mobilityChoice.setProgramme(getProgramme());
    mobilityChoice.setPartner(getPartner());
    mobilityChoice.setCountry(getCountry());
    mobilityChoice.setUser(getUser(UserDto.ROLE_PROFESSOR));
    mobilityChoice.getUser().setId(1);
    mobilityChoice.setPreferenceOrder(2);
    mobilityChoice.setMobilityType("SMS");
    mobilityChoice.setTerm(1);
    mobilityChoice.setVersion(1);
    mobilityChoice.setAcademicYear(2016);
    return mobilityChoice;
  }

  /**
   * Return a Mobility mock object.
   * 
   * @return a mobility
   */
  public MobilityDto getMobility() {
    MobilityDto mobility = (MobilityDto) entityFactory.build(MobilityDto.class);
    mobility.setId(1);
    mobility.setProgramme(getProgramme());
    mobility.setPartner(getPartner());
    mobility.setCountry(getCountry());
    mobility.setUser(getUser(UserDto.ROLE_STUDENT));
    mobility.getUser().setId(1);
    mobility.setPreferenceOrder(2);
    mobility.setMobilityType("SMS");
    mobility.setTerm(1);
    mobility.setAcademicYear(2016);
    mobility.setState(MobilityDto.STATE_CREATED);
    mobility.setProfessorInCharge(getUser(UserDto.ROLE_PROFESSOR));
    mobility.setNominatedStudent(getNominatedStudent());
    List<DocumentDto> documents = new ArrayList<DocumentDto>();
    documents.add(getDocument());
    DocumentDto doc = getDocument();
    doc.setCategory(DocumentDto.RETURN_DOCUMENT);
    documents.add(doc);
    mobility.setDocuments(documents);
    return mobility;
  }

  /**
   * Return a Document mock object.
   * 
   * @return a document
   */
  public DocumentDto getDocument() {
    DocumentDto document = (DocumentDto) entityFactory.build(DocumentDto.class);
    document.setId(1);
    document.setCategory(DocumentDto.DEPARTURE_DOCUMENT);
    return document;
  }

  /**
   * Return a country mock object.
   * 
   * @return a country
   */
  public CountryDto getCountry() {
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode("IE");
    country.setName("Irlande");
    country.setProgramme(getProgramme());
    return country;
  }

  /**
   * Return a programme mock object.
   * 
   * @return a programme
   */
  public ProgrammeDto getProgramme() {
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    programme.setId(1);
    programme.setProgrammeName("Erasmus+");
    programme.setExternalSoftName("Mobility Tool");
    return programme;
  }

  /**
   * Return a partner mock object.
   * 
   * @return a partner
   */
  public PartnerDto getPartner() {
    PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
    partner.setBusinessName("Business partner");
    partner.setEmail("email@email.org");
    partner.setEmployeeCount(3);
    partner.setFullName("Full partner");
    partner.setLegalName("Legal partner");
    partner.setOrganisationType("Partner organization type");
    partner.setPhoneNumber("+32493184140");
    partner.setWebsite("www.partnerwebsite.com");
    partner.setProgramme(getProgramme());
    List<PartnerOptionDto> options = new ArrayList<PartnerOptionDto>();
    options.add(getPartnerOption());
    partner.setOptions(options);
    partner.setAddress(getAddress());
    return partner;
  }

  /**
   * Return a partner mock object.
   * 
   * @return a partner
   */
  public DenialReasonDto getDenialReason() {
    DenialReasonDto denialReason = (DenialReasonDto) entityFactory.build(DenialReason.class);
    denialReason.setReason("no reason whatsoever");
    return denialReason;
  }

  /**
   * Return an address mock object.
   * 
   * @return an address
   */
  public AddressDto getAddress() {
    AddressDto address = (AddressDto) entityFactory.build(AddressDto.class);
    address.setVersion(1);
    address.setNumber("69/4B");
    address.setPostalCode("Bee");
    address.setRegion("Beetle's region");
    address.setStreet("Penny Lane");
    address.setCity("LiverPool");
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode("GB");
    country.setName("Angleterre");
    country.setProgramme(getProgramme());
    address.setCountry(country);
    return address;
  }

  /**
   * Return a partnerOption mock object.
   * 
   * @return a partnerOption
   */
  public PartnerOptionDto getPartnerOption() {
    PartnerOptionDto option = (PartnerOptionDto) entityFactory.build(PartnerOptionDto.class);
    option.setDepartement("DIT");
    option.setCode("BIN");
    option.setName("Bachelier en informatique de gestion");
    return option;
  }

  /**
   * Return a user mock object.
   * 
   * @return a user
   */
  public UserDto getUser(String role) {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    user.setLastName("TheReaper");
    user.setFirstName("Jack");
    user.setUsername("JackKikoo");
    user.setPassword("cyka");
    user.setEmail("Jack@Reaper.uk");
    user.setRegistrationDate(LocalDateTime.now());
    user.setRole(role);
    user.setVersion(1);
    user.setOption(getOption());
    return user;
  }

  /**
   * Return an option mock object.
   * 
   * @return an option
   */
  public OptionDto getOption() {
    OptionDto option = (OptionDto) entityFactory.build(OptionDto.class);
    option.setCode("BIN");
    option.setName("Bachelier en informatique de gestion");
    return option;
  }

  /**
   * Return a payment mock object.
   * 
   * @return a payment
   */
  public PaymentDto getPayment() {
    PaymentDto payment = (PaymentDto) entityFactory.build(PaymentDto.class);
    payment.setMobilityChoiceId(1);
    payment.setUser(getUser(UserDto.ROLE_STUDENT));
    payment.setMobilityType("SMS");
    payment.setAcademicYear("2015-2016");
    payment.setTerm(1);
    payment.setProgramme(getProgramme());
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode("IE");
    country.setName("Irlande");
    payment.setCountry(country);
    PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
    partner.setId(1);
    partner.setFullName("fullName");
    payment.setPartner(partner);
    payment.setPaymentDate(LocalDateTime.now());
    payment.setPaymentType("PaymentYpe");
    return payment;
  }

  /**
   * Return a NominatedStudent mock object.
   * 
   * @return a nominatedStudent
   */
  public NominatedStudentDto getNominatedStudent() {
    NominatedStudentDto nominatedStudent =
        (NominatedStudentDto) entityFactory.build(NominatedStudentDto.class);
    nominatedStudent.setLastName("TheReaper");
    nominatedStudent.setFirstName("Jack");
    nominatedStudent.setUsername("JackKikoo");
    nominatedStudent.setPassword("cyka");
    nominatedStudent.setEmail("Jack@Reaper.uk");
    nominatedStudent.setRegistrationDate(LocalDateTime.now());
    nominatedStudent.setRole(UserDto.ROLE_STUDENT);
    nominatedStudent.setOption(getOption());
    nominatedStudent.setTitle("Mr");
    nominatedStudent.setBirthdate(LocalDate.now());
    nominatedStudent.setNationality(getCountry());
    nominatedStudent.setPhoneNumber("0496/43/33/33");
    nominatedStudent.setGender("M");
    nominatedStudent.setNbrPassedYears(2);
    nominatedStudent.setIban("This is an IBAN");
    nominatedStudent.setCardHolder("MyDad");
    nominatedStudent.setBankName("ChineseGoldFarmer");
    nominatedStudent.setBic("B093");
    nominatedStudent.setAddress(getAddress());
    nominatedStudent.setVersion(1);
    return nominatedStudent;
  }
}
