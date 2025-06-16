package com.dragomitch.ipl.pae.persistence.mocks;

import com.dragomitch.ipl.pae.business.DtoFactory;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.persistence.OptionDao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockOptionDao implements OptionDao {

  private Map<String, OptionDto> options;

  /**
   * Sole constructor.
   * 
   * @param dtoFactory builds objects
   */
  public MockOptionDao(DtoFactory dtoFactory) {
    options = new HashMap<String, OptionDto>();
    OptionDto optionBin = (OptionDto) dtoFactory.create(OptionDto.class);
    optionBin.setCode("BIN");
    optionBin.setName("Bachelier en informatique de gestion");
    OptionDto optionBbm = (OptionDto) dtoFactory.create(OptionDto.class);
    optionBbm.setCode("BBM");
    optionBbm.setName("Bachelier en biologie médicale");
    OptionDto optionBch = (OptionDto) dtoFactory.create(OptionDto.class);
    optionBch.setCode("BCH");
    optionBch.setName("Bachelier en chimie");
    OptionDto optionBdi = (OptionDto) dtoFactory.create(OptionDto.class);
    optionBdi.setCode("BDI");
    optionBdi.setName("Bachelier en diététique");
    OptionDto optionBim = (OptionDto) dtoFactory.create(OptionDto.class);
    optionBim.setCode("BIM");
    optionBim.setName("Bachelier en imagerie médicale");
    options.put("BIN", optionBin);
    options.put("BBM", optionBbm);
    options.put("BCH", optionBch);
    options.put("BDI", optionBdi);
    options.put("BIM", optionBim);
  }

  @Override
  public List<OptionDto> findAll() {
    List<OptionDto> optionsList = new ArrayList<OptionDto>();
    for (OptionDto option : options.values()) {
      optionsList.add(option);
    }
    return optionsList;
  }

  @Override
  public OptionDto findByCode(String code) {
    return options.get(code);
  }
}
