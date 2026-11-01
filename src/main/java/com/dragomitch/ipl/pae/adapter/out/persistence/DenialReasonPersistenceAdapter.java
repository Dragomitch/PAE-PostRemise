package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.DenialReasonEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.repository.DenialReasonJpaRepository;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.persistence.DenialReasonDao;

import java.util.ConcurrentModificationException;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/** The reasons a professor can give to deny a mobility choice. */
@Repository
class DenialReasonPersistenceAdapter implements DenialReasonDao {

  private final DenialReasonJpaRepository reasons;
  private final PersistenceMapper mapper;

  DenialReasonPersistenceAdapter(DenialReasonJpaRepository reasons, PersistenceMapper mapper) {
    this.reasons = reasons;
    this.mapper = mapper;
  }

  @Override
  public DenialReasonDto create(DenialReasonDto denialReason) {
    DenialReasonEntity entity = new DenialReasonEntity();
    entity.setReason(denialReason.getReason());
    denialReason.setId(reasons.save(entity).getId());
    return denialReason;
  }

  @Override
  public DenialReasonDto findById(int id) {
    return reasons.findById(id).map(mapper::denialReason).orElse(null);
  }

  @Override
  public List<DenialReasonDto> findAll() {
    return reasons.findAll(Sort.by("id")).stream().map(mapper::denialReason).toList();
  }

  /**
   * {@inheritDoc}
   *
   * @throws ConcurrentModificationException if the reason does not exist (any more): the table
   *         has no version column, so a missing row is the only conflict
   */
  @Override
  public DenialReasonDto update(DenialReasonDto denialReason) {
    DenialReasonEntity entity = reasons.findById(denialReason.getId())
        .orElseThrow(() -> new ConcurrentModificationException(
            "Denial reason " + denialReason.getId() + " does not exist (any more)"));
    entity.setReason(denialReason.getReason());
    reasons.flush();
    return denialReason;
  }
}
