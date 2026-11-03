package com.dragomitch.ipl.pae.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;

import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.domain.port.out.CountryRepository;
import com.dragomitch.ipl.pae.domain.port.out.DocumentRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityChoiceRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityDocumentRepository;
import com.dragomitch.ipl.pae.domain.port.out.NominatedStudentRepository;
import com.dragomitch.ipl.pae.domain.port.out.OptionRepository;
import com.dragomitch.ipl.pae.domain.port.out.PartnerRepository;
import com.dragomitch.ipl.pae.domain.port.out.PartnerOptionRepository;
import com.dragomitch.ipl.pae.domain.port.out.PaymentRepository;
import com.dragomitch.ipl.pae.domain.port.out.ProgrammeRepository;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;

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
class QueryCountIT extends AbstractRepositoryIT {

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @Autowired
  private CountryRepository countryRepository;
  @Autowired
  private DocumentRepository documentRepository;
  @Autowired
  private MobilityChoiceRepository mobilityChoiceRepository;
  @Autowired
  private MobilityRepository mobilityRepository;
  @Autowired
  private MobilityDocumentRepository mobilityDocumentRepository;
  @Autowired
  private NominatedStudentRepository nominatedStudentRepository;
  @Autowired
  private OptionRepository optionRepository;
  @Autowired
  private PartnerRepository partnerRepository;
  @Autowired
  private PartnerOptionRepository partnerOptionRepository;
  @Autowired
  private PaymentRepository paymentRepository;
  @Autowired
  private ProgrammeRepository programmeRepository;
  @Autowired
  private UserRepository userRepository;

  @Override
  protected List<String> fixtures() {
    return List.of("db/fixtures/users.sql", "db/fixtures/partners.sql",
        "db/fixtures/nominated_students.sql", "db/fixtures/mobility_choices.sql",
        "db/fixtures/mobilities.sql");
  }

  static Stream<Named<Function<QueryCountIT, Object>>> reads() {
    return Stream.of(
        read("CountryRepository.findAll", it -> it.countryRepository.findAll()),
        read("DocumentRepository.findAllByProgramme", it -> it.documentRepository.findAllByProgramme(1)),
        read("MobilityChoiceRepository.findAll", it -> it.mobilityChoiceRepository.findAll(
            MobilityChoiceRepository.FILTER_ALL_MOBILITIES_CHOICES)),
        read("MobilityChoiceRepository.findById", it -> it.mobilityChoiceRepository.findById(5001)),
        read("MobilityChoiceRepository.findByUser", it -> it.mobilityChoiceRepository.findByUser(1001)),
        read("MobilityChoiceRepository.findByActivePartner",
            it -> it.mobilityChoiceRepository.findByActivePartner(3001)),
        read("MobilityRepository.findAll", it -> it.mobilityRepository.findAll()),
        read("MobilityRepository.findById", it -> it.mobilityRepository.findById(5006)),
        read("MobilityDocumentRepository.findAllByMobility",
            it -> it.mobilityDocumentRepository.findAllByMobility(5006)),
        read("NominatedStudentRepository.findAll", it -> it.nominatedStudentRepository.findAll()),
        read("OptionRepository.findAll", it -> it.optionRepository.findAll()),
        read("PartnerRepository.findAll (professor)", it -> it.partnerRepository.findAll(
            PartnerRepository.FILTER_ALL_PARTNERS, "", UserDto.ROLE_PROFESSOR, null)),
        read("PartnerRepository.findAll (student)", it -> it.partnerRepository.findAll(
            PartnerRepository.FILTER_ALL_PARTNERS, "", UserDto.ROLE_STUDENT, "BIN")),
        read("PartnerRepository.findById", it -> it.partnerRepository.findById(3001)),
        read("PartnerOptionRepository.findAllOptionsByPartner",
            it -> it.partnerOptionRepository.findAllOptionsByPartner(3001)),
        read("PaymentRepository.findAll", it -> it.paymentRepository.findAll()),
        read("ProgrammeRepository.findAll", it -> it.programmeRepository.findAll()),
        read("UserRepository.findAll", it -> it.userRepository.findAll()));
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
