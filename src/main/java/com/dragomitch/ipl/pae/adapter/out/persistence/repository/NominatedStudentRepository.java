package com.dragomitch.ipl.pae.adapter.out.persistence.repository;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.NominatedStudentEntity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NominatedStudentRepository extends JpaRepository<NominatedStudentEntity, Integer> {

  /** The student with their user, option and nationality, in one query. */
  @EntityGraph(attributePaths = {"user", "user.option", "nationality"})
  Optional<NominatedStudentEntity> findWithUserById(int id);

  /** Every student with their user, option and nationality, in one query. */
  @EntityGraph(attributePaths = {"user", "user.option", "nationality"})
  List<NominatedStudentEntity> findAllWithUserByOrderById();
}
