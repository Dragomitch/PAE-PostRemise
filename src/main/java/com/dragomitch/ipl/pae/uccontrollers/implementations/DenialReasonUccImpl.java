package com.dragomitch.ipl.pae.uccontrollers.implementations;

import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.persistence.DenialReasonDao;
import com.dragomitch.ipl.pae.uccontrollers.DenialReasonUcc;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Denial reasons. Their format is checked by the constraints of {@link DenialReasonDto}. */
@Service
@Transactional
class DenialReasonUccImpl implements DenialReasonUcc {

  private final DenialReasonDao denialReasonDao;

  DenialReasonUccImpl(DenialReasonDao denialReasonDao) {
    this.denialReasonDao = denialReasonDao;
  }

  @Override
  public DenialReasonDto create(DenialReasonDto denialReason) {
    return denialReasonDao.create(denialReason);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DenialReasonDto> showAll() {
    return denialReasonDao.findAll();
  }

  @Override
  public DenialReasonDto edit(int id, DenialReasonDto denialReason) {
    if (denialReasonDao.findById(id) == null) {
      throw new ResourceNotFoundException();
    }
    denialReason.setId(id);
    denialReasonDao.update(denialReason);
    return denialReason;
  }
}
