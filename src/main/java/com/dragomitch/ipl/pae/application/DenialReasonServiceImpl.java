package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.DenialReason;
import com.dragomitch.ipl.pae.domain.port.in.DenialReasonService;
import com.dragomitch.ipl.pae.domain.port.out.DenialReasonRepository;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Denial reasons. Their format is checked by the constraints of {@link DenialReason}. The list is
 * cached ({@link ReferenceDataCaches#DENIAL_REASONS}) and emptied by every create and update.
 */
@Service
@Transactional
class DenialReasonServiceImpl implements DenialReasonService {

  private final DenialReasonRepository denialReasonRepository;

  DenialReasonServiceImpl(DenialReasonRepository denialReasonRepository) {
    this.denialReasonRepository = denialReasonRepository;
  }

  @Override
  @CacheEvict(cacheNames = ReferenceDataCaches.DENIAL_REASONS, allEntries = true)
  public DenialReason create(DenialReason denialReason) {
    return denialReasonRepository.create(denialReason);
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(ReferenceDataCaches.DENIAL_REASONS)
  public List<DenialReason> showAll() {
    return List.copyOf(denialReasonRepository.findAll());
  }

  @Override
  @CacheEvict(cacheNames = ReferenceDataCaches.DENIAL_REASONS, allEntries = true)
  public DenialReason edit(int id, DenialReason denialReason) {
    if (denialReasonRepository.findById(id).isEmpty()) {
      throw new ResourceNotFoundException();
    }
    denialReason.setId(id);
    denialReasonRepository.update(denialReason);
    return denialReason;
  }
}
