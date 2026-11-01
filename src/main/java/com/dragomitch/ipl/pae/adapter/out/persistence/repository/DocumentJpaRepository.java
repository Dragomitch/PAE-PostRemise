package com.dragomitch.ipl.pae.adapter.out.persistence.repository;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.DocumentEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentJpaRepository extends JpaRepository<DocumentEntity, Integer> {

  List<DocumentEntity> findByProgrammeIdOrderById(int programmeId);
}
