package com.dragomitch.ipl.pae.persistence.jdbc.repository;

import com.dragomitch.ipl.pae.persistence.jdbc.entity.CountryEntity;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;

public interface CountryRepository extends ListCrudRepository<CountryEntity, String> {

  /**
   * Every country, sorted by name with the collation of the database.
   *
   * @return all countries
   */
  List<CountryEntity> findAllByOrderByNameAsc();
}
