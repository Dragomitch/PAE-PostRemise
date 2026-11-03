package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.domain.port.out.DenialReasonRepository;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

public class InMemoryDenialReasonRepository
    implements DenialReasonRepository, ResettableRepository {
  private List<DenialReasonDto> denialReasons;

  public InMemoryDenialReasonRepository() {
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
    if (findById(denialReasonDto.getId()) == null) {
      throw new ConcurrentModificationException();
    }
    denialReasonDto.setVersion(denialReasons.get(denialReasonDto.getId() - 1).getVersion() + 1);
    denialReasons.set(denialReasonDto.getId() - 1, denialReasonDto);
    return denialReasonDto;
  }

  @Override
  public void empty() {
    denialReasons = new ArrayList<DenialReasonDto>();
  }
}
