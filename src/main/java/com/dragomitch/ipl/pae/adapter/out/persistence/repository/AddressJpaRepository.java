package com.dragomitch.ipl.pae.adapter.out.persistence.repository;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.AddressEntity;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressJpaRepository extends JpaRepository<AddressEntity, Integer> {

  /** The address and its country, in one query. */
  @EntityGraph(attributePaths = "country")
  Optional<AddressEntity> findWithCountryById(int id);
}
