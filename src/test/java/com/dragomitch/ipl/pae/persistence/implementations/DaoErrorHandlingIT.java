package com.dragomitch.ipl.pae.persistence.implementations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.named;

import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.persistence.DenialReasonDao;
import com.dragomitch.ipl.pae.persistence.DocumentDao;
import com.dragomitch.ipl.pae.persistence.MobilityChoiceDao;
import com.dragomitch.ipl.pae.persistence.MobilityDao;
import com.dragomitch.ipl.pae.persistence.MobilityDocumentDao;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;
import com.dragomitch.ipl.pae.persistence.OptionDao;
import com.dragomitch.ipl.pae.persistence.PartnerDao;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;
import com.dragomitch.ipl.pae.persistence.PaymentDao;
import com.dragomitch.ipl.pae.persistence.ProgrammeDao;
import com.dragomitch.ipl.pae.persistence.UserDao;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Every DAO method reports a database failure as a {@link FatalException} (the use cases and the
 * presentation layer rely on it to answer 500 and roll back), never as a raw {@code SQLException},
 * which is kept as the cause.
 *
 * <p>The failure is provoked by aborting the transaction first: PostgreSQL then rejects every
 * following statement ("current transaction is aborted"). When the persistence layer moves to
 * Spring, this contract becomes "a {@code DataAccessException} is thrown" and only the expected
 * type below changes.
 *
 * <p>The same calls also check that no DAO method leaks a JDBC statement.
 */
class DaoErrorHandlingIT extends AbstractDaoIT {

  /** Every DAO bean, so that the method source can reach them. */
  record Daos(AddressDao address, CountryDao country, DenialReasonDao denialReason,
      DocumentDao document, MobilityChoiceDao mobilityChoice, MobilityDao mobility,
      MobilityDocumentDao mobilityDocument, NominatedStudentDao nominatedStudent,
      OptionDao option, PartnerDao partner, PartnerOptionDao partnerOption, PaymentDao payment,
      ProgrammeDao programme, UserDao user, DaoErrorHandlingIT test) {
  }

  @Autowired
  private StatementRecordingDataSource recordingDataSource;
  @Autowired
  private AddressDao addressDao;
  @Autowired
  private CountryDao countryDao;
  @Autowired
  private DenialReasonDao denialReasonDao;
  @Autowired
  private DocumentDao documentDao;
  @Autowired
  private MobilityChoiceDao mobilityChoiceDao;
  @Autowired
  private MobilityDao mobilityDao;
  @Autowired
  private MobilityDocumentDao mobilityDocumentDao;
  @Autowired
  private NominatedStudentDao nominatedStudentDao;
  @Autowired
  private OptionDao optionDao;
  @Autowired
  private PartnerDao partnerDao;
  @Autowired
  private PartnerOptionDao partnerOptionDao;
  @Autowired
  private PaymentDao paymentDao;
  @Autowired
  private ProgrammeDao programmeDao;
  @Autowired
  private UserDao userDao;

  static Stream<Named<Consumer<Daos>>> daoCalls() {
    return Stream.of(
        call("AddressDao.create", d -> d.address().create(d.test().address())),
        call("AddressDao.findById", d -> d.address().findById(1)),
        call("AddressDao.update", d -> d.address().update(d.test().address())),
        call("CountryDao.findById", d -> d.country().findById("BE")),
        call("CountryDao.findAll", d -> d.country().findAll()),
        call("DenialReasonDao.create", d -> d.denialReason().create(d.test().denialReason())),
        call("DenialReasonDao.findById", d -> d.denialReason().findById(1)),
        call("DenialReasonDao.findAll", d -> d.denialReason().findAll()),
        call("DenialReasonDao.update", d -> d.denialReason().update(d.test().denialReason())),
        call("DocumentDao.findAllByProgramme", d -> d.document().findAllByProgramme(1)),
        call("MobilityChoiceDao.create", d -> d.mobilityChoice().create(d.test().choice())),
        call("MobilityChoiceDao.findById", d -> d.mobilityChoice().findById(1)),
        call("MobilityChoiceDao.findAll", d -> d.mobilityChoice().findAll("all")),
        call("MobilityChoiceDao.findByUser", d -> d.mobilityChoice().findByUser(1)),
        call("MobilityChoiceDao.findByPartner", d -> d.mobilityChoice().findByPartner(1)),
        call("MobilityChoiceDao.findByActivePartner",
            d -> d.mobilityChoice().findByActivePartner(1)),
        call("MobilityChoiceDao.update", d -> d.mobilityChoice().update(d.test().choice())),
        call("MobilityDao.create", d -> d.mobility().create(d.test().mobility())),
        call("MobilityDao.findById", d -> d.mobility().findById(1)),
        call("MobilityDao.findAll", d -> d.mobility().findAll()),
        call("MobilityDao.findByUser", d -> d.mobility().findByUser(1)),
        call("MobilityDao.update", d -> d.mobility().update(d.test().mobility())),
        call("MobilityDocumentDao.create", d -> d.mobilityDocument().create(1, 1)),
        call("MobilityDocumentDao.findAllByMobility", d -> d.mobilityDocument().findAllByMobility(1)),
        call("MobilityDocumentDao.fillInDocument", d -> d.mobilityDocument().fillInDocument(1, 1)),
        call("NominatedStudentDao.create", d -> d.nominatedStudent().create(d.test().student())),
        call("NominatedStudentDao.findById", d -> d.nominatedStudent().findById(1)),
        call("NominatedStudentDao.findAll", d -> d.nominatedStudent().findAll()),
        call("NominatedStudentDao.update", d -> d.nominatedStudent().update(d.test().student())),
        call("OptionDao.findByCode", d -> d.option().findByCode("BIN")),
        call("OptionDao.findAll", d -> d.option().findAll()),
        call("PartnerDao.create", d -> d.partner().create(d.test().partner())),
        call("PartnerDao.findById", d -> d.partner().findById(1)),
        call("PartnerDao.findAll", d -> d.partner().findAll("all", null, "Professor", "BIN")),
        call("PartnerDao.update", d -> d.partner().update(d.test().partner())),
        call("PartnerOptionDao.create", d -> d.partnerOption().create(
            d.test().build(PartnerOptionDto.class), 1)),
        call("PartnerOptionDao.findAllPartnersByOption",
            d -> d.partnerOption().findAllPartnersByOption("BIN")),
        call("PartnerOptionDao.findAllOptionsByPartner",
            d -> d.partnerOption().findAllOptionsByPartner(1)),
        call("PaymentDao.findAll", d -> d.payment().findAll()),
        call("ProgrammeDao.findById", d -> d.programme().findById(1)),
        call("ProgrammeDao.findAll", d -> d.programme().findAll()),
        call("UserDao.create", d -> d.user().create(d.test().user())),
        call("UserDao.findById", d -> d.user().findById(1)),
        call("UserDao.findAll", d -> d.user().findAll()),
        call("UserDao.findBy", d -> d.user().findBy("username", "alice")),
        call("UserDao.update", d -> d.user().update(d.test().user())),
        call("UserDao.promoteToProfessor(id)", d -> d.user().promoteToProfessor(1, 1)),
        call("UserDao.promoteToProfessor(username)",
            d -> d.user().promoteToProfessor("alice", 1)),
        call("UserDao.isEmpty", d -> d.user().isEmpty()));
  }

  private static Named<Consumer<Daos>> call(String name, Consumer<Daos> call) {
    return named(name, call);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("daoCalls")
  void aDatabaseErrorIsReportedAsAFatalException(Consumer<Daos> call) {
    Daos daos = new Daos(addressDao, countryDao, denialReasonDao, documentDao, mobilityChoiceDao,
        mobilityDao, mobilityDocumentDao, nominatedStudentDao, optionDao, partnerDao,
        partnerOptionDao, paymentDao, programmeDao, userDao, this);
    runInTransaction(() -> {
      abortTransaction();
      // the SQL cause is kept for the logs
      assertThatThrownBy(() -> call.accept(daos)).isInstanceOf(FatalException.class)
          .hasCauseInstanceOf(SQLException.class);
    });
  }

  /**
   * Every statement a DAO prepares is closed before the method returns, whether it succeeds,
   * fails in the database or throws a business exception (e.g. a stale version), and no DAO takes
   * a connection of its own: it works on the one of the caller's transaction. The data source of
   * {@link DaoItConfig} records the statements created on its connections (whether by a Spring
   * Data repository or by {@code JdbcClient}) and the connections it hands out.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("daoCalls")
  void everyPreparedStatementIsClosed(Consumer<Daos> call) {
    Daos daos = new Daos(addressDao, countryDao, denialReasonDao, documentDao, mobilityChoiceDao,
        mobilityDao, mobilityDocumentDao, nominatedStudentDao, optionDao, partnerDao,
        partnerOptionDao, paymentDao, programmeDao, userDao, this);
    for (boolean aborted : new boolean[] {false, true}) {
      runInTransaction(() -> {
        if (aborted) {
          abortTransaction();
        }
        recordingDataSource.startRecording();
        try {
          call.accept(daos);
        } catch (RuntimeException expected) {
          // failures are fine here: only the statement lifecycle is checked
        } finally {
          recordingDataSource.stopRecording();
        }
        List<Statement> statements = recordingDataSource.statements();
        assertThat(statements).as("statements prepared").isNotEmpty();
        assertThat(statements).allSatisfy(
            stmt -> assertThat(stmt.isClosed()).as("closed: %s", stmt).isTrue());
        assertThat(recordingDataSource.connections())
            .as("connections taken outside the transaction").isEmpty();
      });
    }
  }

  private void abortTransaction() {
    try {
      execute("SELECT 1 / 0");
    } catch (IllegalStateException expected) {
      // division by zero: the transaction is now aborted
    }
  }

  // Minimal but complete DTOs, so that the failure comes from the database and not from a
  // NullPointerException while binding the parameters.

  AddressDto address() {
    AddressDto address = build(AddressDto.class);
    CountryDto country = build(CountryDto.class);
    country.setCountryCode("BE");
    address.setCountry(country);
    return address;
  }

  DenialReasonDto denialReason() {
    return build(DenialReasonDto.class);
  }

  MobilityChoiceDto choice() {
    MobilityChoiceDto choice = build(MobilityChoiceDto.class);
    choice.setUser(build(UserDto.class));
    choice.setProgramme(build(ProgrammeDto.class));
    choice.setCountry(build(CountryDto.class));
    choice.setSubmissionDate(LocalDateTime.of(2025, 1, 1, 0, 0));
    return choice;
  }

  MobilityDto mobility() {
    MobilityDto mobility = build(MobilityDto.class);
    mobility.setSubmissionDate(LocalDateTime.of(2025, 1, 1, 0, 0));
    return mobility;
  }

  NominatedStudentDto student() {
    NominatedStudentDto student = build(NominatedStudentDto.class);
    student.setBirthdate(LocalDate.of(2000, 1, 1));
    student.setNationality(build(CountryDto.class));
    student.setAddress(build(AddressDto.class));
    return student;
  }

  PartnerDto partner() {
    PartnerDto partner = build(PartnerDto.class);
    partner.setAddress(build(AddressDto.class));
    return partner;
  }

  UserDto user() {
    UserDto user = build(UserDto.class);
    user.setOption(build(OptionDto.class));
    user.setRegistrationDate(LocalDateTime.of(2025, 1, 1, 0, 0));
    return user;
  }
}
