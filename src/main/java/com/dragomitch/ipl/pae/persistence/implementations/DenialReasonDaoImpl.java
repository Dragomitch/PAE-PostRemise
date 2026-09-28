package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.persistence.DenialReasonDao;
import com.dragomitch.ipl.pae.persistence.jdbc.entity.DenialReasonEntity;
import com.dragomitch.ipl.pae.persistence.jdbc.repository.DenialReasonRepository;

import java.util.ConcurrentModificationException;
import java.util.List;

import org.springframework.stereotype.Repository;

/**
 * {@link DenialReasonDao} on top of the Spring Data {@link DenialReasonRepository}. Denial
 * reasons have no version (the last write wins), but an update matching no row is reported as a
 * {@link ConcurrentModificationException}, like the other DAOs.
 */
@Repository
class DenialReasonDaoImpl implements DenialReasonDao {

  private final EntityFactory entityFactory;
  private final DenialReasonRepository reasons;

  DenialReasonDaoImpl(EntityFactory entityFactory, DenialReasonRepository reasons) {
    this.entityFactory = entityFactory;
    this.reasons = reasons;
  }

  @Override
  public DenialReasonDto create(DenialReasonDto denialReason) {
    DenialReasonEntity created = DataAccess.call(
        () -> reasons.save(new DenialReasonEntity(null, denialReason.getReason())));
    denialReason.setId(created.id());
    return denialReason;
  }

  @Override
  public DenialReasonDto findById(int id) {
    return DataAccess.call(() -> reasons.findById(id).map(this::toDto).orElse(null));
  }

  @Override
  public List<DenialReasonDto> findAll() {
    return DataAccess.call(() -> reasons.findAll().stream().map(this::toDto).toList());
  }

  @Override
  public DenialReasonDto update(DenialReasonDto denialReason) {
    int updated = DataAccess.call(
        () -> reasons.updateReason(denialReason.getId(), denialReason.getReason()));
    if (updated == 0) {
      // denial_reasons has no version column: an update can only miss a deleted/unknown row
      throw new ConcurrentModificationException(
          "Denial reason " + denialReason.getId() + " does not exist (any more)");
    }
    return denialReason;
  }

  private DenialReasonDto toDto(DenialReasonEntity entity) {
    DenialReasonDto reason = (DenialReasonDto) entityFactory.build(DenialReasonDto.class);
    reason.setId(entity.id());
    reason.setReason(entity.reason());
    return reason;
  }
}
