package com.dragomitch.ipl.pae.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;

import com.dragomitch.ipl.pae.adapter.out.persistence.AbstractRepositoryIT;
import com.dragomitch.ipl.pae.config.CacheConfig;
import com.dragomitch.ipl.pae.domain.model.DenialReason;
import com.dragomitch.ipl.pae.domain.port.in.CountryService;
import com.dragomitch.ipl.pae.domain.port.in.DenialReasonService;
import com.dragomitch.ipl.pae.domain.port.in.OptionService;
import com.dragomitch.ipl.pae.domain.port.in.ProgrammeService;
import jakarta.persistence.EntityManagerFactory;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * The cache of the reference data on the real persistence adapters and the embedded PostgreSQL of
 * {@link AbstractRepositoryIT}: the first read of a use case runs its SQL, the second one runs
 * <strong>no</strong> statement (counted by the Hibernate statistics, as in {@code QueryCountIT}).
 * The use cases are transactional and cached as in the application ({@link CacheConfig}).
 */
@ContextConfiguration(classes = ReferenceDataCacheIT.UseCases.class)
class ReferenceDataCacheIT extends AbstractRepositoryIT {

  /** The cached use cases and the cache configuration, on top of the persistence beans. */
  @TestConfiguration(proxyBeanMethods = false)
  @EnableTransactionManagement
  @PropertySource("classpath:application.properties")
  @Import({
    CacheConfig.class,
    CountryServiceImpl.class,
    OptionServiceImpl.class,
    ProgrammeServiceImpl.class,
    ProgrammeDocuments.class,
    DenialReasonServiceImpl.class
  })
  static class UseCases {}

  @Autowired private EntityManagerFactory entityManagerFactory;
  @Autowired private CacheManager cacheManager;

  @Autowired private CountryService countryService;
  @Autowired private OptionService optionService;
  @Autowired private ProgrammeService programmeService;
  @Autowired private ProgrammeDocuments programmeDocuments;
  @Autowired private DenialReasonService denialReasonService;

  private Statistics statistics;

  @BeforeEach
  void emptyTheCaches() {
    cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
  }

  static Stream<Named<Function<ReferenceDataCacheIT, Object>>> reads() {
    return Stream.of(
        read("CountryService.showAll", it -> it.countryService.showAll()),
        read("CountryService.showOne", it -> it.countryService.showOne("BE")),
        read("OptionService.showAll", it -> it.optionService.showAll()),
        read("ProgrammeService.showAll", it -> it.programmeService.showAll()),
        read("ProgrammeService.showOne", it -> it.programmeService.showOne(1)),
        read(
            "ProgrammeDocuments.findAllByProgramme",
            it -> it.programmeDocuments.findAllByProgramme(1)),
        read("DenialReasonService.showAll", it -> it.denialReasonService.showAll()));
  }

  private static Named<Function<ReferenceDataCacheIT, Object>> read(
      String name, Function<ReferenceDataCacheIT, Object> read) {
    return named(name, read);
  }

  /** The number of SQL statements {@code work} runs. */
  private long statements(Runnable work) {
    statistics.clear();
    work.run();
    return statistics.getPrepareStatementCount();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("reads")
  void theSecondReadRunsNoStatement(Function<ReferenceDataCacheIT, Object> read) {
    Object[] results = new Object[2];

    assertThat(statements(() -> results[0] = read.apply(this))).as("first read").isEqualTo(1);
    assertThat(statements(() -> results[1] = read.apply(this))).as("second read").isZero();

    assertThat(results[0]).as("the reference data has rows").isNotNull();
    if (results[0] instanceof Collection<?> rows) {
      assertThat(rows).isNotEmpty();
    }
    assertThat(results[1]).as("the cached result").isSameAs(results[0]);
  }

  @Test
  void aCreatedDenialReasonIsInTheNextList() {
    int before = denialReasonService.showAll().size();
    // the test transaction is rolled back: the new reason is never committed
    runInTransaction(() -> {
      DenialReason reason = new DenialReason();
      reason.setReason("Dossier incomplet.");
      denialReasonService.create(reason);

      List<DenialReason> after = denialReasonService.showAll();

      assertThat(after).hasSize(before + 1);
      assertThat(after).extracting(DenialReason::getReason).contains("Dossier incomplet.");
    });
  }
}
