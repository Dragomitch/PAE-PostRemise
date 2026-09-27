package com.dragomitch.ipl.pae.persistence.jdbc.repository;

import com.dragomitch.ipl.pae.persistence.jdbc.entity.DenialReasonEntity;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

public interface DenialReasonRepository extends ListCrudRepository<DenialReasonEntity, Integer> {

  /**
   * Replaces the text of a reason. Unlike {@code save}, which throws when no row is updated, an
   * unknown id is silently ignored (the contract of {@code DenialReasonDao.update}).
   *
   * @param id the reason id
   * @param reason the new text
   * @return the number of rows updated (0 or 1)
   */
  @Modifying
  @Query("UPDATE student_exchange_tools.denial_reasons SET reason = :reason WHERE reason_id = :id")
  int updateReason(@Param("id") int id, @Param("reason") String reason);
}
