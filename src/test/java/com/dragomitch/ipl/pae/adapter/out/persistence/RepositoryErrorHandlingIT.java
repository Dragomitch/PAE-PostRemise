package com.dragomitch.ipl.pae.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.named;

import com.dragomitch.ipl.pae.domain.model.Address;
import com.dragomitch.ipl.pae.domain.model.Country;
import com.dragomitch.ipl.pae.domain.model.DenialReason;
import com.dragomitch.ipl.pae.domain.model.Mobility;
import com.dragomitch.ipl.pae.domain.model.MobilityChoice;
import com.dragomitch.ipl.pae.domain.model.MobilityChoiceFilter;
import com.dragomitch.ipl.pae.domain.model.NominatedStudent;
import com.dragomitch.ipl.pae.domain.model.Option;
import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.PartnerFilter;
import com.dragomitch.ipl.pae.domain.model.PartnerOption;
import com.dragomitch.ipl.pae.domain.model.Programme;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.out.AddressRepository;
import com.dragomitch.ipl.pae.domain.port.out.CountryRepository;
import com.dragomitch.ipl.pae.domain.port.out.DenialReasonRepository;
import com.dragomitch.ipl.pae.domain.port.out.DocumentRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityChoiceRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityDocumentRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityRepository;
import com.dragomitch.ipl.pae.domain.port.out.NominatedStudentRepository;
import com.dragomitch.ipl.pae.domain.port.out.OptionRepository;
import com.dragomitch.ipl.pae.domain.port.out.PartnerOptionRepository;
import com.dragomitch.ipl.pae.domain.port.out.PartnerRepository;
import com.dragomitch.ipl.pae.domain.port.out.PaymentRepository;
import com.dragomitch.ipl.pae.domain.port.out.ProgrammeRepository;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

/**
 * Every outbound port reports a database failure as a Spring {@link DataAccessException} (the
 * exception translation of the persistence adapters), never as a raw {@code SQLException}, which is
 * kept as the root cause for the logs. The use cases roll back and the API answers 500 with an
 * error id.
 *
 * <p>The failure is provoked by aborting the transaction first: PostgreSQL then rejects every
 * following statement ("current transaction is aborted").
 */
class RepositoryErrorHandlingIT extends AbstractRepositoryIT {

  /** Every DAO bean, so that the method source can reach them. */
  record Repositories(
      AddressRepository address,
      CountryRepository country,
      DenialReasonRepository denialReason,
      DocumentRepository document,
      MobilityChoiceRepository mobilityChoice,
      MobilityRepository mobility,
      MobilityDocumentRepository mobilityDocument,
      NominatedStudentRepository nominatedStudent,
      OptionRepository option,
      PartnerRepository partner,
      PartnerOptionRepository partnerOption,
      PaymentRepository payment,
      ProgrammeRepository programme,
      UserRepository user,
      RepositoryErrorHandlingIT test) {}

  @Autowired private AddressRepository addressRepository;
  @Autowired private CountryRepository countryRepository;
  @Autowired private DenialReasonRepository denialReasonRepository;
  @Autowired private DocumentRepository documentRepository;
  @Autowired private MobilityChoiceRepository mobilityChoiceRepository;
  @Autowired private MobilityRepository mobilityRepository;
  @Autowired private MobilityDocumentRepository mobilityDocumentRepository;
  @Autowired private NominatedStudentRepository nominatedStudentRepository;
  @Autowired private OptionRepository optionRepository;
  @Autowired private PartnerRepository partnerRepository;
  @Autowired private PartnerOptionRepository partnerOptionRepository;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private ProgrammeRepository programmeRepository;
  @Autowired private UserRepository userRepository;

  static Stream<Named<Consumer<Repositories>>> daoCalls() {
    return Stream.of(
        call("AddressRepository.create", d -> d.address().create(d.test().address())),
        call("AddressRepository.findById", d -> d.address().findById(1).orElse(null)),
        call("AddressRepository.update", d -> d.address().update(d.test().address())),
        call("CountryRepository.findById", d -> d.country().findById("BE").orElse(null)),
        call("CountryRepository.findAll", d -> d.country().findAll()),
        call(
            "DenialReasonRepository.create", d -> d.denialReason().create(d.test().denialReason())),
        call("DenialReasonRepository.findById", d -> d.denialReason().findById(1).orElse(null)),
        call("DenialReasonRepository.findAll", d -> d.denialReason().findAll()),
        call(
            "DenialReasonRepository.update", d -> d.denialReason().update(d.test().denialReason())),
        call("DocumentRepository.findAllByProgramme", d -> d.document().findAllByProgramme(1)),
        call("MobilityChoiceRepository.create", d -> d.mobilityChoice().create(d.test().choice())),
        call("MobilityChoiceRepository.findById", d -> d.mobilityChoice().findById(1).orElse(null)),
        call(
            "MobilityChoiceRepository.findAll",
            d -> d.mobilityChoice().findAll(MobilityChoiceFilter.ALL)),
        call("MobilityChoiceRepository.findByUser", d -> d.mobilityChoice().findByUser(1)),
        call("MobilityChoiceRepository.findByPartner", d -> d.mobilityChoice().findByPartner(1)),
        call(
            "MobilityChoiceRepository.findByActivePartner",
            d -> d.mobilityChoice().findByActivePartner(1)),
        call("MobilityChoiceRepository.update", d -> d.mobilityChoice().update(d.test().choice())),
        call("MobilityRepository.create", d -> d.mobility().create(d.test().mobility())),
        call("MobilityRepository.findById", d -> d.mobility().findById(1).orElse(null)),
        call("MobilityRepository.findAll", d -> d.mobility().findAll()),
        call("MobilityRepository.findByUser", d -> d.mobility().findByUser(1)),
        call("MobilityRepository.update", d -> d.mobility().update(d.test().mobility())),
        call("MobilityDocumentRepository.create", d -> d.mobilityDocument().create(1, 1)),
        call(
            "MobilityDocumentRepository.findAllByMobility",
            d -> d.mobilityDocument().findAllByMobility(1)),
        call(
            "MobilityDocumentRepository.fillInDocument",
            d -> d.mobilityDocument().fillInDocument(1, 1)),
        call(
            "NominatedStudentRepository.create",
            d -> d.nominatedStudent().create(d.test().student())),
        call(
            "NominatedStudentRepository.findById",
            d -> d.nominatedStudent().findById(1).orElse(null)),
        call("NominatedStudentRepository.findAll", d -> d.nominatedStudent().findAll()),
        call(
            "NominatedStudentRepository.update",
            d -> d.nominatedStudent().update(d.test().student())),
        call("OptionRepository.findByCode", d -> d.option().findByCode("BIN").orElse(null)),
        call("OptionRepository.findAll", d -> d.option().findAll()),
        call("PartnerRepository.create", d -> d.partner().create(d.test().partner())),
        call("PartnerRepository.findById", d -> d.partner().findById(1).orElse(null)),
        call(
            "PartnerRepository.findAll",
            d -> d.partner().findAll(PartnerFilter.ALL, null, Role.PROFESSOR, "BIN")),
        call("PartnerRepository.update", d -> d.partner().update(d.test().partner())),
        call(
            "PartnerOptionRepository.create",
            d -> d.partnerOption().create(d.test().partnerOption(), 1)),
        call(
            "PartnerOptionRepository.findAllPartnersByOption",
            d -> d.partnerOption().findAllPartnersByOption("BIN")),
        call(
            "PartnerOptionRepository.findAllOptionsByPartner",
            d -> d.partnerOption().findAllOptionsByPartner(1)),
        call("PaymentRepository.findAll", d -> d.payment().findAll()),
        call("ProgrammeRepository.findById", d -> d.programme().findById(1).orElse(null)),
        call("ProgrammeRepository.findAll", d -> d.programme().findAll()),
        call("UserRepository.create", d -> d.user().create(d.test().user())),
        call("UserRepository.findById", d -> d.user().findById(1).orElse(null)),
        call("UserRepository.findAll", d -> d.user().findAll()),
        call("UserRepository.findByUsername", d -> d.user().findByUsername("alice")),
        call("UserRepository.findByEmail", d -> d.user().findByEmail("alice@student.test")),
        call("UserRepository.update", d -> d.user().update(d.test().user())),
        call("UserRepository.promoteToProfessor(id)", d -> d.user().promoteToProfessor(1, 1)),
        call(
            "UserRepository.promoteToProfessor(username)",
            d -> d.user().promoteToProfessor("alice", 1)),
        call("UserRepository.isEmpty", d -> d.user().isEmpty()));
  }

  private static Named<Consumer<Repositories>> call(String name, Consumer<Repositories> call) {
    return named(name, call);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("daoCalls")
  void aDatabaseErrorIsReportedAsADataAccessException(Consumer<Repositories> call) {
    Repositories daos =
        new Repositories(
            addressRepository,
            countryRepository,
            denialReasonRepository,
            documentRepository,
            mobilityChoiceRepository,
            mobilityRepository,
            mobilityDocumentRepository,
            nominatedStudentRepository,
            optionRepository,
            partnerRepository,
            partnerOptionRepository,
            paymentRepository,
            programmeRepository,
            userRepository,
            this);
    runInTransaction(() -> {
      abortTransaction();
      // the SQL cause is kept for the logs
      assertThatThrownBy(() -> call.accept(daos))
          .isInstanceOf(DataAccessException.class)
          .hasRootCauseInstanceOf(SQLException.class);
    });
  }

  private void abortTransaction() {
    try {
      execute("SELECT 1 / 0");
    } catch (IllegalStateException expected) {
      // division by zero: the transaction is now aborted
    }
  }

  // Minimal but complete DTOs, so that the failure comes from the database and not from a
  // NullPointerException while binding the parameters, or from JPA refusing a reference to an
  // entity without id.

  Address address() {
    Address address = new Address();
    Country country = new Country();
    country.setCountryCode("BE");
    address.setCountry(country);
    return address;
  }

  DenialReason denialReason() {
    return new DenialReason();
  }

  MobilityChoice choice() {
    MobilityChoice choice = new MobilityChoice();
    choice.setUser(new User());
    choice.setProgramme(new Programme());
    Country country = new Country();
    country.setCountryCode("BE");
    choice.setCountry(country);
    choice.setSubmissionDate(LocalDateTime.of(2025, 1, 1, 0, 0));
    return choice;
  }

  Mobility mobility() {
    Mobility mobility = new Mobility();
    mobility.setSubmissionDate(LocalDateTime.of(2025, 1, 1, 0, 0));
    return mobility;
  }

  NominatedStudent student() {
    NominatedStudent student = new NominatedStudent();
    student.setBirthdate(LocalDate.of(2000, 1, 1));
    Country nationality = new Country();
    nationality.setCountryCode("BE");
    student.setNationality(nationality);
    student.setAddress(new Address());
    return student;
  }

  Partner partner() {
    Partner partner = new Partner();
    partner.setAddress(new Address());
    return partner;
  }

  PartnerOption partnerOption() {
    PartnerOption option = new PartnerOption();
    option.setCode("BIN");
    return option;
  }

  User user() {
    User user = new User();
    Option option = new Option();
    option.setCode("BIN");
    user.setOption(option);
    user.setRegistrationDate(LocalDateTime.of(2025, 1, 1, 0, 0));
    return user;
  }
}
