package persistence.mocks;

import business.EntityFactory;
import business.dto.ProgrammeDto;
import main.annotations.Inject;
import persistence.ProgrammeDao;

import java.util.ArrayList;
import java.util.List;

public class MockProgrammeDao implements ProgrammeDao {

  private List<ProgrammeDto> programmes;
  private EntityFactory entityFactory;

  /**
   * Sole constructor for explicit invocation.
   * 
   * @param entityFactory an on-demand object dispenser
   */
  @Inject
  public MockProgrammeDao(EntityFactory entityFactory) {
    programmes = new ArrayList<ProgrammeDto>();
    this.entityFactory = entityFactory;
    ProgrammeDto programme1 = (ProgrammeDto) this.entityFactory.build(ProgrammeDto.class);
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
