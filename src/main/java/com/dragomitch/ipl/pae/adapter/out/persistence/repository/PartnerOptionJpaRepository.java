package com.dragomitch.ipl.pae.adapter.out.persistence.repository;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.PartnerOptionEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.PartnerOptionId;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartnerOptionJpaRepository
    extends JpaRepository<PartnerOptionEntity, PartnerOptionId> {

  /** The options of the partner with their names, sorted by option code, in one query. */
  @Query("SELECT po FROM PartnerOptionEntity po JOIN FETCH po.option"
      + " WHERE po.id.partnerId = :partnerId ORDER BY po.id.optionCode")
  List<PartnerOptionEntity> findByPartnerWithOption(@Param("partnerId") int partnerId);

  /** The ids of the partners that offer the option, sorted. */
  @Query("SELECT po.id.partnerId FROM PartnerOptionEntity po WHERE po.id.optionCode = :optionCode"
      + " ORDER BY po.id.partnerId")
  List<Integer> findPartnerIdsByOption(@Param("optionCode") String optionCode);
}
