package persistence.mocks;

import business.dto.DenialReasonDto;
import persistence.DenialReasonDao;

import java.util.ArrayList;
import java.util.List;

public class MockDenialReasonDao implements DenialReasonDao {
  private List<DenialReasonDto> denialReasons;

  public MockDenialReasonDao() {
    denialReasons = new ArrayList<DenialReasonDto>();
  }

  @Override
  public DenialReasonDto create(DenialReasonDto denialReason) {
    denialReasons.add(denialReason);
    denialReason.setId(denialReasons.size());
    return denialReason;
  }

  @Override
  public DenialReasonDto findById(int id) {
    if (id > 0 && id <= denialReasons.size()) {
      return denialReasons.get(id - 1);
    }
    return null;
  }

  @Override
  public List<DenialReasonDto> findAll() {
    return denialReasons;
  }

  @Override
  public DenialReasonDto update(DenialReasonDto denialReasonDto) {
    denialReasonDto.setVersion(denialReasons.get(denialReasonDto.getId() - 1).getVersion() + 1);
    denialReasons.set(denialReasonDto.getId() - 1, denialReasonDto);
    return denialReasonDto;
  }

  public void empty() {
    denialReasons = new ArrayList<DenialReasonDto>();
  }

}
