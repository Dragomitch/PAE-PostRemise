package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.DenialReason;
import com.dragomitch.ipl.pae.domain.port.in.DenialReasonService;
import com.dragomitch.ipl.pae.domain.port.out.DenialReasonRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Denial reasons. Their format is checked by the constraints of {@link DenialReason}. */
@Service
@Transactional
class DenialReasonServiceImpl implements DenialReasonService {

  private final DenialReasonRepository denialReasonRepository;

  DenialReasonServiceImpl(DenialReasonRepository denialReasonRepository) {
    this.denialReasonRepository = denialReasonRepository;
  }

  @Override
  public DenialReason create(DenialReason denialReason) {
    return denialReasonRepository.create(denialReason);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DenialReason> showAll() {
    return denialReasonRepository.findAll();
  }

  @Override
  public DenialReason edit(int id, DenialReason denialReason) {
    if (denialReasonRepository.findById(id).isEmpty()) {
      throw new ResourceNotFoundException();
    }
    denialReason.setId(id);
    denialReasonRepository.update(denialReason);
    return denialReason;
  }
}
