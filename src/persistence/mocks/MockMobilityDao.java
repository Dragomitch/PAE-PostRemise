package persistence.mocks;

import business.EntityFactory;
import business.NominatedStudent;
import business.dto.MobilityChoiceDto;
import business.dto.MobilityDto;
import main.annotations.Inject;
import persistence.MobilityChoiceDao;
import persistence.MobilityDao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MockMobilityDao implements MobilityDao {

  private MobilityChoiceDao mobilityChoiceDao;
  private EntityFactory entityFactory;
  private Set<MobilityDto> mobilities = new HashSet<MobilityDto>();

  @Inject
  public MockMobilityDao(EntityFactory entityFactory, MobilityChoiceDao mobilityChoiceDao) {
    this.entityFactory = entityFactory;
    this.mobilityChoiceDao = mobilityChoiceDao;
  }

  @Override
  public MobilityDto create(MobilityDto mobility) {
    mobilities.add(mobility);
    mobility.setVersion(1);
    return mobility;
  }

  @Override
  public List<MobilityDto> findAll() {
    List<MobilityDto> mobilities = new ArrayList<MobilityDto>();
    for (MobilityDto curMobility : this.mobilities) {
      mobilities.add(curMobility);
    }
    return mobilities;
  }

  @Override
  public MobilityDto findById(int id) {
    for (MobilityDto curMobility : this.mobilities) {
      if (curMobility.getId() == id) {
        MobilityChoiceDto mobilityChoice = mobilityChoiceDao.findById(id);
        NominatedStudent student = (NominatedStudent) entityFactory.build(NominatedStudent.class);
        student.setId(mobilityChoice.getUser().getId());
        curMobility.setNominatedStudent(student);
        return curMobility;
      }
    }
    return null;
  }

  @Override
  public List<MobilityDto> findByUser(int user) {
    List<MobilityDto> mobilities = new ArrayList<MobilityDto>();
    for (MobilityDto curMobility : this.mobilities) {
      if (curMobility.getUser().getId() == user) {
        mobilities.add(curMobility);
      }
    }
    return mobilities;
  }

  @Override
  public MobilityDto update(MobilityDto mobility) {
    mobility.setVersion(mobility.getVersion() + 1);
    return mobility;
  }

  public void empty() {
    mobilities = new HashSet<MobilityDto>();
  }
}
