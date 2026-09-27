package com.dragomitch.ipl.pae.uccontrollers.implementations;

import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.persistence.ProgrammeDao;
import com.dragomitch.ipl.pae.uccontrollers.ProgrammeUcc;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ProgrammeUccImpl implements ProgrammeUcc {

  private final ProgrammeDao programmeDao;

  ProgrammeUccImpl(ProgrammeDao programmeDao) {
    this.programmeDao = programmeDao;
  }

  @Override
  public ProgrammeDto showOne(int id) {
    ProgrammeDto programme = programmeDao.findById(id);
    if (programme == null) {
      throw new ResourceNotFoundException();
    }
    return programme;
  }

  @Override
  public List<ProgrammeDto> showAll() {
    return programmeDao.findAll();
  }

}
