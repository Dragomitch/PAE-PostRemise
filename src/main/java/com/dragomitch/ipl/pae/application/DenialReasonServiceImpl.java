package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.port.in.DenialReasonService;
import com.dragomitch.ipl.pae.domain.port.out.DenialReasonRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Denial reasons. Their format is checked by the constraints of {@link DenialReasonDto}. */
@Service
@Transactional
class DenialReasonServiceImpl implements DenialReasonService {

  private final DenialReasonRepository denialReasonRepository;

  DenialReasonServiceImpl(DenialReasonRepository denialReasonRepository) {
    this.denialReasonRepository = denialReasonRepository;
  }

  @Override
  public DenialReasonDto create(DenialReasonDto denialReason) {
    return denialReasonRepository.create(denialReason);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DenialReasonDto> showAll() {
    return denialReasonRepository.findAll();
  }

  @Override
  public DenialReasonDto edit(int id, DenialReasonDto denialReason) {
    if (denialReasonRepository.findById(id) == null) {
      throw new ResourceNotFoundException();
    }
    denialReason.setId(id);
    denialReasonRepository.update(denialReason);
    return denialReason;
  }
}
