package com.dragomitch.ipl.pae.adapter.out.persistence.repository;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.ProgrammeEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgrammeJpaRepository extends JpaRepository<ProgrammeEntity, Integer> {
}
