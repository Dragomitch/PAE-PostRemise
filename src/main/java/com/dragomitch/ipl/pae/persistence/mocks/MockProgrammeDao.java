package com.dragomitch.ipl.pae.persistence.mocks;

import com.dragomitch.ipl.pae.business.DtoFactory;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.persistence.ProgrammeDao;

import java.util.ArrayList;
import java.util.List;

public class MockProgrammeDao implements ProgrammeDao {

  private List<ProgrammeDto> programmes;
  private DtoFactory dtoFactory;

  /**
   * Sole constructor for explicit invocation.
   * 
   * @param dtoFactory an on-demand object dispenser
   */
  public MockProgrammeDao(DtoFactory dtoFactory) {
    programmes = new ArrayList<ProgrammeDto>();
    this.dtoFactory = dtoFactory;
    ProgrammeDto programme1 = (ProgrammeDto) this.dtoFactory.create(ProgrammeDto.class);
    programme1.setId(1);
    programme1.setProgrammeName("Erasmus+");
    programme1.setExternalSoftName("Mobility Tool");
    programmes.add(programme1);
  }

  @Override
  public ProgrammeDto findById(int id) {
    if (id > 0 && id <= programmes.size()) {
      return programmes.get((id - 1));
    }
    return null;
  }

  @Override
  public List<ProgrammeDto> findAll() {
    return programmes;
  }
}
