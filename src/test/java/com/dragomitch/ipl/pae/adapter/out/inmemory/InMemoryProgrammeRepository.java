package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.model.Programme;
import com.dragomitch.ipl.pae.domain.port.out.ProgrammeRepository;
import java.util.ArrayList;
import java.util.List;

public class InMemoryProgrammeRepository implements ProgrammeRepository {

  private List<Programme> programmes;

  /** Sole constructor for explicit invocation. */
  public InMemoryProgrammeRepository() {
    programmes = new ArrayList<Programme>();
    Programme programme1 = new Programme();
    programme1.setId(1);
    programme1.setProgrammeName("Erasmus+");
    programme1.setExternalSoftName("Mobility Tool");
    programmes.add(programme1);
  }

  @Override
  public Programme findById(int id) {
    if (id > 0 && id <= programmes.size()) {
      return programmes.get((id - 1));
    }
    return null;
  }

  @Override
  public List<Programme> findAll() {
    return programmes;
  }
}
