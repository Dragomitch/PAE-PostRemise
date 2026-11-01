package com.dragomitch.ipl.pae.adapter.out.persistence.repository;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.CountryEntity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<CountryEntity, String> {

  /** The country and its programme, in one query. */
  @EntityGraph(attributePaths = "programme")
  Optional<CountryEntity> findWithProgrammeByCode(String code);

  /** Every country with its programme, sorted by name, in one query. */
  @EntityGraph(attributePaths = "programme")
  List<CountryEntity> findAllWithProgrammeByOrderByNameAsc();
}
