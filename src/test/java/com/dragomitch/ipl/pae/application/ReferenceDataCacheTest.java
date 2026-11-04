package com.dragomitch.ipl.pae.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dragomitch.ipl.pae.config.CacheConfig;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.Country;
import com.dragomitch.ipl.pae.domain.model.DenialReason;
import com.dragomitch.ipl.pae.domain.model.Document;
import com.dragomitch.ipl.pae.domain.model.Option;
import com.dragomitch.ipl.pae.domain.model.Programme;
import com.dragomitch.ipl.pae.domain.port.in.CountryService;
import com.dragomitch.ipl.pae.domain.port.in.DenialReasonService;
import com.dragomitch.ipl.pae.domain.port.in.OptionService;
import com.dragomitch.ipl.pae.domain.port.in.ProgrammeService;
import com.dragomitch.ipl.pae.domain.port.out.CountryRepository;
import com.dragomitch.ipl.pae.domain.port.out.DenialReasonRepository;
import com.dragomitch.ipl.pae.domain.port.out.DocumentRepository;
import com.dragomitch.ipl.pae.domain.port.out.OptionRepository;
import com.dragomitch.ipl.pae.domain.port.out.PartnerOptionRepository;
import com.dragomitch.ipl.pae.domain.port.out.ProgrammeRepository;
import com.github.benmanes.caffeine.cache.Policy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * The cache of the reference data on the use cases: the real {@link CacheConfig} (the Caffeine
 * specification of {@code application.properties}) and the real services, on mocked repositories
 * and a mocked transaction manager. Two reads reach the repository once, a read after an eviction
 * reaches it again.
 */
class ReferenceDataCacheTest {

  private final CountryRepository countryRepository = mock(CountryRepository.class);
  private final OptionRepository optionRepository = mock(OptionRepository.class);
  private final PartnerOptionRepository partnerOptionRepository =
      mock(PartnerOptionRepository.class);
  private final ProgrammeRepository programmeRepository = mock(ProgrammeRepository.class);
  private final DocumentRepository documentRepository = mock(DocumentRepository.class);
  private final DenialReasonRepository denialReasonRepository = mock(DenialReasonRepository.class);
  private final PlatformTransactionManager transactionManager =
      mock(PlatformTransactionManager.class);

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(Infrastructure.class, CacheConfig.class)
          .withBean(CountryRepository.class, () -> countryRepository)
          .withBean(OptionRepository.class, () -> optionRepository)
          .withBean(PartnerOptionRepository.class, () -> partnerOptionRepository)
          .withBean(ProgrammeRepository.class, () -> programmeRepository)
          .withBean(DocumentRepository.class, () -> documentRepository)
          .withBean(DenialReasonRepository.class, () -> denialReasonRepository)
          .withBean(PlatformTransactionManager.class, () -> transactionManager)
          .withBean(CountryServiceImpl.class)
          .withBean(OptionServiceImpl.class)
          .withBean(ProgrammeServiceImpl.class)
          .withBean(ProgrammeDocuments.class)
          .withBean(DenialReasonServiceImpl.class);

  /** The transactions of the use cases, and the properties of the application. */
  @Configuration(proxyBeanMethods = false)
  @EnableTransactionManagement
  @PropertySource("classpath:application.properties")
  static class Infrastructure {}

  @BeforeEach
  void transactions() {
    when(transactionManager.getTransaction(any()))
        .thenAnswer(call -> new SimpleTransactionStatus());
  }

  private static Country country(String code) {
    Country country = new Country();
    country.setCountryCode(code);
    return country;
  }

  private static Programme programme(int id) {
    Programme programme = new Programme();
    programme.setId(id);
    return programme;
  }

  private static DenialReason denialReason(int id) {
    DenialReason reason = new DenialReason();
    reason.setId(id);
    reason.setReason("Reason " + id);
    return reason;
  }

  private static void evict(AssertableApplicationContext context, String cache) {
    context.getBean(CacheManager.class).getCache(cache).clear();
  }

  @Test
  void theCacheManagerHoldsOneCachePerTypeWithTheSpecificationOfTheProperties() {
    runner.run(context -> {
      CacheManager cacheManager = context.getBean(CacheManager.class);
      assertThat(cacheManager.getCacheNames())
          .containsExactlyInAnyOrder(
              ReferenceDataCaches.COUNTRIES,
              ReferenceDataCaches.OPTIONS,
              ReferenceDataCaches.PROGRAMMES,
              ReferenceDataCaches.DOCUMENTS,
              ReferenceDataCaches.DENIAL_REASONS);
      assertThat(cacheManager.getCache("partners")).as("no cache is created on demand").isNull();

      Policy<Object, Object> policy =
          ((CaffeineCache) cacheManager.getCache(ReferenceDataCaches.COUNTRIES))
              .getNativeCache()
              .policy();
      assertThat(policy.eviction().orElseThrow().getMaximum()).isEqualTo(500);
      assertThat(policy.expireAfterWrite().orElseThrow().getExpiresAfter())
          .isEqualTo(Duration.ofHours(6));
      assertThat(policy.isRecordingStats()).as("hits and misses for the cache meters").isTrue();
    });
  }

  @Test
  void theCountriesAreReadOnceUntilEvicted() {
    when(countryRepository.findAll())
        .thenReturn(new ArrayList<>(List.of(country("GB"), country("IE"))));
    runner.run(context -> {
      CountryService service = context.getBean(CountryService.class);

      List<Country> first = service.showAll();
      List<Country> second = service.showAll();

      assertThat(second).isSameAs(first).extracting(Country::getCountryCode).hasSize(2);
      verify(countryRepository, times(1)).findAll();

      evict(context, ReferenceDataCaches.COUNTRIES);
      service.showAll();
      verify(countryRepository, times(2)).findAll();
    });
  }

  @Test
  void aCountryIsReadOnceByCodeUntilEvicted() {
    when(countryRepository.findById("IE")).thenReturn(Optional.of(country("IE")));
    when(countryRepository.findById("GB")).thenReturn(Optional.of(country("GB")));
    runner.run(context -> {
      CountryService service = context.getBean(CountryService.class);

      assertThat(service.showOne("IE").getCountryCode()).isEqualTo("IE");
      assertThat(service.showOne("IE").getCountryCode()).isEqualTo("IE");
      assertThat(service.showOne("GB").getCountryCode()).as("another key").isEqualTo("GB");
      verify(countryRepository, times(1)).findById("IE");
      verify(countryRepository, times(1)).findById("GB");

      evict(context, ReferenceDataCaches.COUNTRIES);
      service.showOne("IE");
      verify(countryRepository, times(2)).findById("IE");
    });
  }

  @Test
  void anUnknownCountryIsNotCached() {
    when(countryRepository.findById("ZZ")).thenReturn(Optional.empty());
    runner.run(context -> {
      CountryService service = context.getBean(CountryService.class);

      assertThatThrownBy(() -> service.showOne("ZZ")).isInstanceOf(ResourceNotFoundException.class);
      assertThatThrownBy(() -> service.showOne("ZZ")).isInstanceOf(ResourceNotFoundException.class);
      verify(countryRepository, times(2)).findById("ZZ");
    });
  }

  @Test
  void theCachedListsCannotBeModified() {
    when(countryRepository.findAll()).thenReturn(new ArrayList<>(List.of(country("GB"))));
    when(programmeRepository.findAll()).thenReturn(new ArrayList<>(List.of(programme(1))));
    when(optionRepository.findAll()).thenReturn(new ArrayList<>(List.of(new Option())));
    when(documentRepository.findAllByProgramme(1)).thenReturn(new ArrayList<>());
    when(denialReasonRepository.findAll()).thenReturn(new ArrayList<>(List.of(denialReason(1))));
    runner.run(context -> {
      List<List<?>> lists =
          List.of(
              context.getBean(CountryService.class).showAll(),
              context.getBean(ProgrammeService.class).showAll(),
              context.getBean(OptionService.class).showAll(),
              context.getBean(ProgrammeDocuments.class).findAllByProgramme(1),
              context.getBean(DenialReasonService.class).showAll());
      for (List<?> list : lists) {
        assertThatThrownBy(list::clear).isInstanceOf(UnsupportedOperationException.class);
      }
    });
  }

  @Test
  void aCacheHitOpensNoTransaction() {
    when(countryRepository.findAll()).thenReturn(List.of(country("GB")));
    runner.run(context -> {
      CountryService service = context.getBean(CountryService.class);

      service.showAll();
      verify(transactionManager, times(1)).getTransaction(any());

      service.showAll();
      verify(transactionManager, times(1)).getTransaction(any());
    });
  }

  @Test
  void theProgrammesAreReadOnceUntilEvicted() {
    when(programmeRepository.findAll()).thenReturn(List.of(programme(1)));
    when(programmeRepository.findById(1)).thenReturn(Optional.of(programme(1)));
    runner.run(context -> {
      ProgrammeService service = context.getBean(ProgrammeService.class);

      service.showAll();
      service.showAll();
      service.showOne(1);
      service.showOne(1);
      verify(programmeRepository, times(1)).findAll();
      verify(programmeRepository, times(1)).findById(1);

      evict(context, ReferenceDataCaches.PROGRAMMES);
      service.showAll();
      service.showOne(1);
      verify(programmeRepository, times(2)).findAll();
      verify(programmeRepository, times(2)).findById(1);
    });
  }

  @Test
  void theOptionsAreReadOnceUntilEvicted() {
    when(optionRepository.findAll()).thenReturn(List.of(new Option()));
    runner.run(context -> {
      OptionService service = context.getBean(OptionService.class);

      service.showAll();
      service.showAll();
      verify(optionRepository, times(1)).findAll();

      evict(context, ReferenceDataCaches.OPTIONS);
      service.showAll();
      verify(optionRepository, times(2)).findAll();
    });
  }

  @Test
  void theDocumentsOfAProgrammeAreReadOnceUntilEvicted() {
    when(documentRepository.findAllByProgramme(1)).thenReturn(List.of(new Document()));
    when(documentRepository.findAllByProgramme(2)).thenReturn(List.of());
    runner.run(context -> {
      ProgrammeDocuments documents = context.getBean(ProgrammeDocuments.class);

      assertThat(documents.findAllByProgramme(1)).hasSize(1);
      assertThat(documents.findAllByProgramme(1)).hasSize(1);
      assertThat(documents.findAllByProgramme(2)).as("another key").isEmpty();
      verify(documentRepository, times(1)).findAllByProgramme(1);
      verify(documentRepository, times(1)).findAllByProgramme(2);

      evict(context, ReferenceDataCaches.DOCUMENTS);
      documents.findAllByProgramme(1);
      verify(documentRepository, times(2)).findAllByProgramme(1);
    });
  }

  @Test
  void theDenialReasonsAreReadOnceUntilOneIsCreatedOrEdited() {
    when(denialReasonRepository.findAll()).thenReturn(List.of(denialReason(1)));
    when(denialReasonRepository.create(any())).thenAnswer(call -> call.getArgument(0));
    when(denialReasonRepository.findById(1)).thenReturn(Optional.of(denialReason(1)));
    runner.run(context -> {
      DenialReasonService service = context.getBean(DenialReasonService.class);

      service.showAll();
      service.showAll();
      verify(denialReasonRepository, times(1)).findAll();

      service.create(denialReason(0));
      service.showAll();
      service.showAll();
      verify(denialReasonRepository, times(2)).findAll();

      service.edit(1, denialReason(1));
      service.showAll();
      verify(denialReasonRepository, times(3)).findAll();
    });
  }

  @Test
  void aFailedEditOfADenialReasonKeepsTheCache() {
    when(denialReasonRepository.findAll()).thenReturn(List.of(denialReason(1)));
    when(denialReasonRepository.findById(9)).thenReturn(Optional.empty());
    runner.run(context -> {
      DenialReasonService service = context.getBean(DenialReasonService.class);

      service.showAll();
      assertThatThrownBy(() -> service.edit(9, denialReason(9)))
          .isInstanceOf(ResourceNotFoundException.class);
      service.showAll();
      verify(denialReasonRepository, times(1)).findAll();
    });
  }
}
