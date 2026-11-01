package com.dragomitch.ipl.pae.adapter.out.persistence.repository;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.PartnerEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/** The partners; their filtered search is built by {@code PartnerPersistenceAdapter}. */
public interface PartnerRepository extends JpaRepository<PartnerEntity, Integer> {
}
