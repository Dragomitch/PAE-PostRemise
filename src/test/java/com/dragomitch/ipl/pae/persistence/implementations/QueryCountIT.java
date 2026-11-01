package com.dragomitch.ipl.pae.persistence.implementations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;

import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.persistence.CountryDao;
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

import jakarta.persistence.EntityManagerFactory;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * N+1 guard: every read of a driven port runs <strong>one</strong> SQL statement, however many
 * rows it returns, with everything its DTOs carry (fetch joins and entity graphs). A lazy
 * association read in a mapping would add one statement per row and fail here.
 */
class QueryCountIT extends AbstractDaoIT {

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @Autowired
  private CountryDao countryDao;
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

  @Override
  protected List<String> fixtures() {
    return List.of("db/fixtures/users.sql", "db/fixtures/partners.sql",
        "db/fixtures/nominated_students.sql", "db/fixtures/mobility_choices.sql",
        "db/fixtures/mobilities.sql");
  }

  static Stream<Named<Function<QueryCountIT, Object>>> reads() {
    return Stream.of(
        read("CountryDao.findAll", it -> it.countryDao.findAll()),
        read("DocumentDao.findAllByProgramme", it -> it.documentDao.findAllByProgramme(1)),
        read("MobilityChoiceDao.findAll", it -> it.mobilityChoiceDao.findAll(
            MobilityChoiceDao.FILTER_ALL_MOBILITIES_CHOICES)),
        read("MobilityChoiceDao.findById", it -> it.mobilityChoiceDao.findById(5001)),
        read("MobilityChoiceDao.findByUser", it -> it.mobilityChoiceDao.findByUser(1001)),
        read("MobilityChoiceDao.findByActivePartner",
            it -> it.mobilityChoiceDao.findByActivePartner(3001)),
        read("MobilityDao.findAll", it -> it.mobilityDao.findAll()),
        read("MobilityDao.findById", it -> it.mobilityDao.findById(5006)),
        read("MobilityDocumentDao.findAllByMobility",
            it -> it.mobilityDocumentDao.findAllByMobility(5006)),
        read("NominatedStudentDao.findAll", it -> it.nominatedStudentDao.findAll()),
        read("OptionDao.findAll", it -> it.optionDao.findAll()),
        read("PartnerDao.findAll (professor)", it -> it.partnerDao.findAll(
            PartnerDao.FILTER_ALL_PARTNERS, "", UserDto.ROLE_PROFESSOR, null)),
        read("PartnerDao.findAll (student)", it -> it.partnerDao.findAll(
            PartnerDao.FILTER_ALL_PARTNERS, "", UserDto.ROLE_STUDENT, "BIN")),
        read("PartnerDao.findById", it -> it.partnerDao.findById(3001)),
        read("PartnerOptionDao.findAllOptionsByPartner",
            it -> it.partnerOptionDao.findAllOptionsByPartner(3001)),
        read("PaymentDao.findAll", it -> it.paymentDao.findAll()),
        read("ProgrammeDao.findAll", it -> it.programmeDao.findAll()),
        read("UserDao.findAll", it -> it.userDao.findAll()));
  }

  private static Named<Function<QueryCountIT, Object>> read(String name,
      Function<QueryCountIT, Object> read) {
    return named(name, read);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("reads")
  void aReadRunsOneStatement(Function<QueryCountIT, Object> read) {
    Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    runInTransaction(() -> {
      statistics.clear();

      Object result = read.apply(this);

      assertThat(result).as("the fixtures give the read something to map").isNotNull();
      if (result instanceof Collection<?> rows) {
        assertThat(rows).as("the fixtures give the read rows to map").isNotEmpty();
      }
      assertThat(statistics.getPrepareStatementCount()).as("SQL statements").isEqualTo(1);
    });
  }
}
