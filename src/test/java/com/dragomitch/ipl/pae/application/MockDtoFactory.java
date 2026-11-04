package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.model.Address;
import com.dragomitch.ipl.pae.domain.model.Country;
import com.dragomitch.ipl.pae.domain.model.DenialReason;
import com.dragomitch.ipl.pae.domain.model.Document;
import com.dragomitch.ipl.pae.domain.model.DocumentCategory;
import com.dragomitch.ipl.pae.domain.model.Mobility;
import com.dragomitch.ipl.pae.domain.model.MobilityChoice;
import com.dragomitch.ipl.pae.domain.model.MobilityState;
import com.dragomitch.ipl.pae.domain.model.MobilityType;
import com.dragomitch.ipl.pae.domain.model.NominatedStudent;
import com.dragomitch.ipl.pae.domain.model.Option;
import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.PartnerOption;
import com.dragomitch.ipl.pae.domain.model.Payment;
import com.dragomitch.ipl.pae.domain.model.Programme;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MockDtoFactory {

  public MockDtoFactory() {}

  /**
   * Return a MobilityChoice mock object.
   *
   * @return a mobilityChoice
   */
  public MobilityChoice getMobilityChoice() {
    MobilityChoice mobilityChoice = new MobilityChoice();
    mobilityChoice.setId(1);
    mobilityChoice.setProgramme(getProgramme());
    mobilityChoice.setPartner(getPartner());
    mobilityChoice.setCountry(getCountry());
    mobilityChoice.setUser(getUser(Role.PROFESSOR));
    mobilityChoice.getUser().setId(1);
    mobilityChoice.setPreferenceOrder(2);
    mobilityChoice.setMobilityType(MobilityType.SMS);
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
  public Mobility getMobility() {
    Mobility mobility = new Mobility();
    mobility.setId(1);
    mobility.setProgramme(getProgramme());
    mobility.setPartner(getPartner());
    mobility.setCountry(getCountry());
    mobility.setUser(getUser(Role.STUDENT));
    mobility.getUser().setId(1);
    mobility.setPreferenceOrder(2);
    mobility.setMobilityType(MobilityType.SMS);
    mobility.setTerm(1);
    mobility.setAcademicYear(2016);
    mobility.setState(MobilityState.CREATED);
    mobility.setProfessorInCharge(getUser(Role.PROFESSOR));
    mobility.setNominatedStudent(getNominatedStudent());
    List<Document> documents = new ArrayList<Document>();
    documents.add(getDocument());
    Document doc = getDocument();
    doc.setCategory(DocumentCategory.RETURN);
    documents.add(doc);
    mobility.setDocuments(documents);
    return mobility;
  }

  /**
   * Return a Document mock object.
   *
   * @return a document
   */
  public Document getDocument() {
    Document document = new Document();
    document.setId(1);
    document.setCategory(DocumentCategory.DEPARTURE);
    return document;
  }

  /**
   * Return a country mock object.
   *
   * @return a country
   */
  public Country getCountry() {
    Country country = new Country();
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
  public Programme getProgramme() {
    Programme programme = new Programme();
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
  public Partner getPartner() {
    Partner partner = new Partner();
    partner.setBusinessName("Business partner");
    partner.setEmail("email@email.org");
    partner.setEmployeeCount(3);
    partner.setFullName("Full partner");
    partner.setLegalName("Legal partner");
    partner.setOrganisationType("Partner organization type");
    partner.setPhoneNumber("+32493184140");
    partner.setWebsite("www.partnerwebsite.com");
    partner.setProgramme(getProgramme());
    List<PartnerOption> options = new ArrayList<PartnerOption>();
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
  public DenialReason getDenialReason() {
    DenialReason denialReason = new DenialReason();
    denialReason.setReason("no reason whatsoever");
    return denialReason;
  }

  /**
   * Return an address mock object.
   *
   * @return an address
   */
  public Address getAddress() {
    Address address = new Address();
    address.setVersion(1);
    address.setNumber("69/4B");
    address.setPostalCode("Bee");
    address.setRegion("Beetle's region");
    address.setStreet("Penny Lane");
    address.setCity("LiverPool");
    Country country = new Country();
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
  public PartnerOption getPartnerOption() {
    PartnerOption option = new PartnerOption();
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
  public User getUser(Role role) {
    User user = new User();
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
  public Option getOption() {
    Option option = new Option();
    option.setCode("BIN");
    option.setName("Bachelier en informatique de gestion");
    return option;
  }

  /**
   * Return a payment mock object.
   *
   * @return a payment
   */
  public Payment getPayment() {
    Payment payment = new Payment();
    payment.setMobilityChoiceId(1);
    payment.setUser(getUser(Role.STUDENT));
    payment.setMobilityType(MobilityType.SMS);
    payment.setAcademicYear("2015-2016");
    payment.setTerm(1);
    payment.setProgramme(getProgramme());
    Country country = new Country();
    country.setCountryCode("IE");
    country.setName("Irlande");
    payment.setCountry(country);
    Partner partner = new Partner();
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
  public NominatedStudent getNominatedStudent() {
    NominatedStudent nominatedStudent = new NominatedStudent();
    nominatedStudent.setLastName("TheReaper");
    nominatedStudent.setFirstName("Jack");
    nominatedStudent.setUsername("JackKikoo");
    nominatedStudent.setPassword("cyka");
    nominatedStudent.setEmail("Jack@Reaper.uk");
    nominatedStudent.setRegistrationDate(LocalDateTime.now());
    nominatedStudent.setRole(Role.STUDENT);
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
