package com.dragomitch.ipl.pae.uccontrollers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;

/**
 * Every use-case service is transactional at class level: a DAO can then never be reached outside
 * a transaction (which would leak its connection; {@code DalBackendServices} refuses it anyway).
 */
class TransactionalServicesTest {

  @Test
  void everyUseCaseServiceIsTransactionalAtClassLevel() throws ClassNotFoundException {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Service.class));
    Set<BeanDefinition> services =
        scanner.findCandidateComponents("com.dragomitch.ipl.pae.uccontrollers");
    assertFalse(services.isEmpty());

    for (BeanDefinition service : services) {
      Class<?> type = ClassUtils.forName(service.getBeanClassName(), null);
      assertNotNull(type.getAnnotation(Transactional.class),
          type.getName() + " must be annotated with @Transactional");
    }
    Set<String> names = services.stream().map(BeanDefinition::getBeanClassName)
        .collect(Collectors.toSet());
    assertTrue(names.size() >= 12, "every use case is scanned: " + names);
  }
}
