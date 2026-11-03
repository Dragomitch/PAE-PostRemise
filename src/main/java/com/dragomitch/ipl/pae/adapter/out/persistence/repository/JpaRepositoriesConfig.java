package com.dragomitch.ipl.pae.adapter.out.persistence.repository;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Creates the Spring Data repositories of this package on {@link StrictFindByIdRepository}, in the
 * application and in the integration tests ({@code RepositoryItConfig} scans this package). It
 * replaces Spring Boot's repository auto-configuration, which offers no way to set the base class.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(
    basePackageClasses = JpaRepositoriesConfig.class,
    repositoryBaseClass = StrictFindByIdRepository.class)
public class JpaRepositoriesConfig {}
