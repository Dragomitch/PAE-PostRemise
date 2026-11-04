package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.Programme;
import com.dragomitch.ipl.pae.domain.port.in.ProgrammeService;
import com.dragomitch.ipl.pae.domain.port.out.ProgrammeRepository;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ProgrammeServiceImpl implements ProgrammeService {

  private final ProgrammeRepository programmeRepository;

  ProgrammeServiceImpl(ProgrammeRepository programmeRepository) {
    this.programmeRepository = programmeRepository;
  }

  @Override
  @Cacheable(ReferenceDataCaches.PROGRAMMES)
  public Programme showOne(int id) {
    Programme programme =
        programmeRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    return programme;
  }

  @Override
  @Cacheable(ReferenceDataCaches.PROGRAMMES)
  public List<Programme> showAll() {
    return List.copyOf(programmeRepository.findAll());
  }
}
