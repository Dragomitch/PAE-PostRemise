package com.dragomitch.ipl.pae.persistence.jdbc.repository;

import com.dragomitch.ipl.pae.persistence.jdbc.entity.ProgrammeEntity;

import org.springframework.data.repository.ListCrudRepository;

public interface ProgrammeRepository extends ListCrudRepository<ProgrammeEntity, Integer> {
}
