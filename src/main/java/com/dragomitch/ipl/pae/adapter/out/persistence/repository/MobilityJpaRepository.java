package com.dragomitch.ipl.pae.adapter.out.persistence.repository;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.MobilityEntity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobilityJpaRepository extends JpaRepository<MobilityEntity, Integer> {

  /** The mobility with its choice, the choice's student, partner, country and programme. */
  @EntityGraph(attributePaths = {"choice", "choice.user", "choice.partner", "choice.country",
      "choice.programme", "denialReason"})
  Optional<MobilityEntity> findWithChoiceById(int id);

  /** Every mobility with what {@link #findWithChoiceById} fetches, in one query. */
  @EntityGraph(attributePaths = {"choice", "choice.user", "choice.partner", "choice.country",
      "choice.programme", "denialReason"})
  List<MobilityEntity> findAllWithChoiceByOrderById();

  /** The mobilities of the student with what {@link #findWithChoiceById} fetches. */
  @EntityGraph(attributePaths = {"choice", "choice.user", "choice.partner", "choice.country",
      "choice.programme", "denialReason"})
  List<MobilityEntity> findWithChoiceByChoiceUserIdOrderById(int userId);
}
