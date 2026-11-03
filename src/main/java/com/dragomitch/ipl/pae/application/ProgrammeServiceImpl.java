package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.port.in.ProgrammeService;
import com.dragomitch.ipl.pae.domain.port.out.ProgrammeRepository;
import java.util.List;
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
  public ProgrammeDto showOne(int id) {
    ProgrammeDto programme = programmeRepository.findById(id);
    if (programme == null) {
      throw new ResourceNotFoundException();
    }
    return programme;
  }

  @Override
  public List<ProgrammeDto> showAll() {
    return programmeRepository.findAll();
  }
}
