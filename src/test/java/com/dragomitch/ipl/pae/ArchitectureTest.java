package com.dragomitch.ipl.pae;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

/**
 * The hexagonal architecture of ADR 0001, checked on the production classes: the domain at the
 * centre, the application services around it, the adapters outside, and the dependencies pointing
 * inwards. A violation fails the build.
 */
@AnalyzeClasses(
    packages = ArchitectureTest.BASE,
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  static final String BASE = "com.dragomitch.ipl.pae";

  private static final String DOMAIN = BASE + ".domain..";
  private static final String APPLICATION = BASE + ".application..";
  private static final String WEB_ADAPTER = BASE + ".adapter.in.web..";
  private static final String PERSISTENCE_ADAPTER = BASE + ".adapter.out.persistence..";
  private static final String CONFIGURATION = BASE + ".config..";

  /**
   * Dependencies point inwards: adapters and configuration depend on the domain, never the reverse.
   */
  @ArchTest
  static final ArchRule dependenciesPointInwards =
      layeredArchitecture()
          .consideringOnlyDependenciesInLayers()
          .layer("Domain")
          .definedBy(DOMAIN)
          .layer("Application")
          .definedBy(APPLICATION)
          .layer("Web adapter")
          .definedBy(WEB_ADAPTER)
          .layer("Persistence adapter")
          .definedBy(PERSISTENCE_ADAPTER)
          .layer("Configuration")
          .definedBy(CONFIGURATION)
          // the use-case implementations are reached through their ports only (Spring wires them)
          .whereLayer("Application")
          .mayNotBeAccessedByAnyLayer()
          // the security chain of the configuration is built from the web adapter's filters
          .whereLayer("Web adapter")
          .mayOnlyBeAccessedByLayers("Configuration")
          .whereLayer("Persistence adapter")
          .mayNotBeAccessedByAnyLayer()
          .whereLayer("Configuration")
          .mayNotBeAccessedByAnyLayer();

  /**
   * The domain depends on the JDK only (and on JSpecify's {@code @Nullable}): no framework, no Bean
   * Validation, no Jackson, no Spring (step 3 of ADR 0001).
   */
  @ArchTest
  static final ArchRule domainDependsOnTheJdkOnly =
      classes()
          .that()
          .resideInAPackage(DOMAIN)
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(DOMAIN, "java..", "org.jspecify.annotations..");

  /** The domain knows no persistence, web or security technology. */
  @ArchTest
  static final ArchRule domainIsFreeOfInfrastructure =
      noClasses()
          .that()
          .resideInAPackage(DOMAIN)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "jakarta.persistence..",
              "org.hibernate..",
              "org.springframework.data..",
              "org.springframework.jdbc..",
              "org.springframework.transaction..",
              "java.sql..",
              "org.springframework.security..",
              "org.springframework.web.servlet..",
              "org.springframework.web.bind..",
              "jakarta.servlet..");

  /** Ports are interfaces. */
  @ArchTest
  static final ArchRule portsAreInterfaces =
      classes()
          .that()
          .resideInAnyPackage(BASE + ".domain.port.in", BASE + ".domain.port.out")
          .should()
          .beInterfaces();

  /** Only the application services implement the driving ports (the use cases). */
  @ArchTest
  static final ArchRule drivingPortsAreImplementedByTheApplication =
      noClasses()
          .that()
          .resideOutsideOfPackage(APPLICATION)
          .should()
          .implement(resideInAPackage(BASE + ".domain.port.in"));

  /** Only the persistence adapter implements the driven ports (the repositories). */
  @ArchTest
  static final ArchRule drivenPortsAreImplementedByThePersistenceAdapter =
      noClasses()
          .that()
          .resideOutsideOfPackage(PERSISTENCE_ADAPTER)
          .should()
          .implement(resideInAPackage(BASE + ".domain.port.out"));

  /**
   * The current date and time come from the injected {@code Clock} ({@code config/ClockConfig}), so
   * that a test can fix them: {@code LocalDateTime.now(clock)}, never the no-argument {@code
   * now()}.
   */
  @ArchTest
  static final ArchRule currentTimeComesFromTheClock =
      noClasses()
          .should()
          .callMethod(LocalDateTime.class, "now")
          .orShould()
          .callMethod(LocalDate.class, "now")
          .orShould()
          .callMethod(Year.class, "now");

  @ArchTest
  static final ArchRule useCaseServicesLiveInTheApplication =
      classes()
          .that()
          .areAnnotatedWith(Service.class)
          .should()
          .resideInAPackage(BASE + ".application");

  @ArchTest
  static final ArchRule controllersLiveInTheWebAdapter =
      classes()
          .that()
          .areAnnotatedWith(RestController.class)
          .should()
          .resideInAPackage(BASE + ".adapter.in.web");

  @ArchTest
  static final ArchRule entitiesLiveInThePersistenceAdapter =
      classes()
          .that()
          .areAnnotatedWith(Entity.class)
          .should()
          .resideInAPackage(BASE + ".adapter.out.persistence.entity");

  @ArchTest
  static final ArchRule springDataRepositoriesLiveInThePersistenceAdapter =
      classes()
          .that()
          .areAssignableTo(Repository.class)
          .and()
          .areInterfaces()
          .should()
          .resideInAPackage(BASE + ".adapter.out.persistence.repository")
          .andShould()
          .haveSimpleNameEndingWith("JpaRepository");

  /** The JPA adapters are named after the port they implement, as ADR 0001 says. */
  @ArchTest
  static final ArchRule jpaAdaptersAreNamedAfterTheirPort =
      classes()
          .that()
          .resideInAPackage(BASE + ".adapter.out.persistence")
          .and()
          .implement(resideInAPackage(BASE + ".domain.port.out"))
          .should()
          .haveSimpleNameStartingWith("Jpa")
          .andShould()
          .haveSimpleNameEndingWith("RepositoryAdapter");
}
