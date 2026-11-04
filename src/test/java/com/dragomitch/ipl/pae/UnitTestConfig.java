package com.dragomitch.ipl.pae;

import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryAddressRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryCountryRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryDenialReasonRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryDocumentRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryMobilityChoiceRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryMobilityDocumentRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryMobilityRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryNominatedStudentRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryOptionRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryPartnerOptionRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryPartnerRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryPasswordHasher;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryPaymentRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryProgrammeRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryUserRepository;
import com.dragomitch.ipl.pae.domain.port.out.PasswordHasher;
import java.time.Clock;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

/**
 * Spring context for the unit tests: the real business objects and use-case controllers, wired to
 * the in-memory mock DAOs instead of the JDBC implementations. The context is cached by the Spring
 * TestContext framework and shared by every test class using it, so the stateful mocks ({@code
 * ResettableRepository}) are emptied before every test method by {@code
 * InMemoryRepositoryResetListener}.
 *
 * <p>As in the application, the use cases are validated ({@code @Validated}: method validation by
 * Spring Boot's validator, whose messages come from the {@code i18n/messages} bundle of {@code
 * application.properties}).
 *
 * <p>It is a {@link TestConfiguration} so that {@code @SpringBootTest} component scanning of the
 * application ignores it.
 */
@TestConfiguration(proxyBeanMethods = false)
@ComponentScan(basePackages = "com.dragomitch.ipl.pae.application")
@Import({
  InMemoryAddressRepository.class,
  InMemoryCountryRepository.class,
  InMemoryDenialReasonRepository.class,
  InMemoryDocumentRepository.class,
  InMemoryMobilityChoiceRepository.class,
  InMemoryMobilityRepository.class,
  InMemoryMobilityDocumentRepository.class,
  InMemoryNominatedStudentRepository.class,
  InMemoryOptionRepository.class,
  InMemoryPartnerRepository.class,
  InMemoryPartnerOptionRepository.class,
  InMemoryPaymentRepository.class,
  InMemoryProgrammeRepository.class,
  InMemoryUserRepository.class
})
@ImportAutoConfiguration({MessageSourceAutoConfiguration.class, ValidationAutoConfiguration.class})
@PropertySource("classpath:application.properties")
public class UnitTestConfig {

  /**
   * The system clock, as in the application ({@code ClockConfig}); the unit tests that need a fixed
   * date build their own.
   */
  @Bean
  Clock clock() {
    return Clock.systemDefaultZone();
  }

  @Bean
  PasswordHasher passwordHasher() {
    return new InMemoryPasswordHasher();
  }
}
