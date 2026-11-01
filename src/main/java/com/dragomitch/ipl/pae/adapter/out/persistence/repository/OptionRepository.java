package com.dragomitch.ipl.pae.adapter.out.persistence.repository;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.OptionEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionRepository extends JpaRepository<OptionEntity, String> {
}
