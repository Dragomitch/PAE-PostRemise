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

import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

/**
 * The hexagonal architecture of ADR 0001, checked on the production classes: the domain at the
 * centre, the application services around it, the adapters outside, and the dependencies pointing
 * inwards. A violation fails the build.
 */
@AnalyzeClasses(packages = ArchitectureTest.BASE, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  static final String BASE = "com.dragomitch.ipl.pae";

  private static final String DOMAIN = BASE + ".domain..";
  private static final String APPLICATION = BASE + ".application..";
  private static final String WEB_ADAPTER = BASE + ".adapter.in.web..";
  private static final String PERSISTENCE_ADAPTER = BASE + ".adapter.out.persistence..";
  private static final String CONFIGURATION = BASE + ".config..";

  /** Dependencies point inwards: adapters and configuration depend on the domain, never the reverse. */
  @ArchTest
  static final ArchRule dependenciesPointInwards = layeredArchitecture()
      .consideringOnlyDependenciesInLayers()
      .layer("Domain").definedBy(DOMAIN)
      .layer("Application").definedBy(APPLICATION)
      .layer("Web adapter").definedBy(WEB_ADAPTER)
      .layer("Persistence adapter").definedBy(PERSISTENCE_ADAPTER)
      .layer("Configuration").definedBy(CONFIGURATION)
      // the use-case implementations are reached through their ports only (Spring wires them)
      .whereLayer("Application").mayNotBeAccessedByAnyLayer()
      // the security chain of the configuration is built from the web adapter's filters
      .whereLayer("Web adapter").mayOnlyBeAccessedByLayers("Configuration")
      .whereLayer("Persistence adapter").mayNotBeAccessedByAnyLayer()
      .whereLayer("Configuration").mayNotBeAccessedByAnyLayer();

  /**
   * The domain knows no persistence, web or security technology. (It still carries Bean
   * Validation, Jackson and a few Spring annotations: step 3 of ADR 0001 removes them.)
   */
  @ArchTest
  static final ArchRule domainIsFreeOfInfrastructure = noClasses().that().resideInAPackage(DOMAIN)
      .should().dependOnClassesThat().resideInAnyPackage(
          "jakarta.persistence..", "org.hibernate..", "org.springframework.data..",
          "org.springframework.jdbc..", "org.springframework.transaction..", "java.sql..",
          "org.springframework.security..", "org.springframework.web.servlet..",
          "org.springframework.web.bind..", "jakarta.servlet..");

  /** Ports are interfaces. */
  @ArchTest
  static final ArchRule portsAreInterfaces = classes().that()
      .resideInAnyPackage(BASE + ".domain.port.in", BASE + ".domain.port.out")
      .should().beInterfaces();

  /** Only the application services implement the driving ports (the use cases). */
  @ArchTest
  static final ArchRule drivingPortsAreImplementedByTheApplication = noClasses().that()
      .resideOutsideOfPackage(APPLICATION)
      .should().implement(resideInAPackage(BASE + ".domain.port.in"));

  /** Only the persistence adapter implements the driven ports (the repositories). */
  @ArchTest
  static final ArchRule drivenPortsAreImplementedByThePersistenceAdapter = noClasses().that()
      .resideOutsideOfPackage(PERSISTENCE_ADAPTER)
      .should().implement(resideInAPackage(BASE + ".domain.port.out"));

  @ArchTest
  static final ArchRule useCaseServicesLiveInTheApplication = classes().that()
      .areAnnotatedWith(Service.class).should().resideInAPackage(BASE + ".application");

  @ArchTest
  static final ArchRule controllersLiveInTheWebAdapter = classes().that()
      .areAnnotatedWith(RestController.class).should().resideInAPackage(BASE + ".adapter.in.web");

  @ArchTest
  static final ArchRule entitiesLiveInThePersistenceAdapter = classes().that()
      .areAnnotatedWith(Entity.class)
      .should().resideInAPackage(BASE + ".adapter.out.persistence.entity");

  @ArchTest
  static final ArchRule springDataRepositoriesLiveInThePersistenceAdapter = classes().that()
      .areAssignableTo(Repository.class).and().areInterfaces()
      .should().resideInAPackage(BASE + ".adapter.out.persistence.repository")
      .andShould().haveSimpleNameEndingWith("JpaRepository");

  /** The JPA adapters are named after the port they implement, as ADR 0001 says. */
  @ArchTest
  static final ArchRule jpaAdaptersAreNamedAfterTheirPort = classes().that()
      .resideInAPackage(BASE + ".adapter.out.persistence")
      .and().implement(resideInAPackage(BASE + ".domain.port.out"))
      .should().haveSimpleNameStartingWith("Jpa")
      .andShould().haveSimpleNameEndingWith("RepositoryAdapter");
}
