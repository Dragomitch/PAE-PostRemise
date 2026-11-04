package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.model.DenialReason;
import com.dragomitch.ipl.pae.domain.port.out.DenialReasonRepository;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;

public class InMemoryDenialReasonRepository
    implements DenialReasonRepository, ResettableRepository {
  private List<DenialReason> denialReasons;

  public InMemoryDenialReasonRepository() {
    denialReasons = new ArrayList<DenialReason>();
  }

  @Override
  public DenialReason create(DenialReason denialReason) {
    denialReasons.add(denialReason);
    denialReason.setId(denialReasons.size());
    return denialReason;
  }

  @Override
  public Optional<DenialReason> findById(int id) {
    if (id > 0 && id <= denialReasons.size()) {
      return Optional.ofNullable(denialReasons.get(id - 1));
    }
    return Optional.empty();
  }

  @Override
  public List<DenialReason> findAll() {
    return denialReasons;
  }

  @Override
  public DenialReason update(DenialReason denialReasonDto) {
    if (findById(denialReasonDto.getId()) == null) {
      throw new ConcurrentModificationException();
    }
    denialReasonDto.setVersion(denialReasons.get(denialReasonDto.getId() - 1).getVersion() + 1);
    denialReasons.set(denialReasonDto.getId() - 1, denialReasonDto);
    return denialReasonDto;
  }

  @Override
  public void empty() {
    denialReasons = new ArrayList<DenialReason>();
  }
}
