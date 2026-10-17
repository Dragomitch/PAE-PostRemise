package com.dragomitch.ipl.pae.persistence.jdbc.repository;

import com.dragomitch.ipl.pae.persistence.jdbc.entity.OptionEntity;

import org.springframework.data.repository.ListCrudRepository;

public interface OptionRepository extends ListCrudRepository<OptionEntity, String> {
}
