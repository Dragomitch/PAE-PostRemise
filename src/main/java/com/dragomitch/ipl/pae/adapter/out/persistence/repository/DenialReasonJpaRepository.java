package com.dragomitch.ipl.pae.adapter.out.persistence.repository;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.DenialReasonEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DenialReasonJpaRepository extends JpaRepository<DenialReasonEntity, Integer> {
}
