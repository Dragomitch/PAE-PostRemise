package persistence.mocks;

import business.EntityFactory;
import business.dto.OptionDto;
import main.annotations.Inject;
import persistence.OptionDao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockOptionDao implements OptionDao {

  private Map<String, OptionDto> options;

  /**
   * Sole constructor.
   * 
   * @param entityFactory builds objects
   */
  @Inject
  public MockOptionDao(EntityFactory entityFactory) {
    options = new HashMap<String, OptionDto>();
    OptionDto optionBin = (OptionDto) entityFactory.build(OptionDto.class);
    optionBin.setCode("BIN");
    optionBin.setName("Bachelier en informatique de gestion");
    OptionDto optionBbm = (OptionDto) entityFactory.build(OptionDto.class);
    optionBbm.setCode("BBM");
    optionBbm.setName("Bachelier en biologie médicale");
    OptionDto optionBch = (OptionDto) entityFactory.build(OptionDto.class);
    optionBch.setCode("BCH");
    optionBch.setName("Bachelier en chimie");
    OptionDto optionBdi = (OptionDto) entityFactory.build(OptionDto.class);
    optionBdi.setCode("BDI");
    optionBdi.setName("Bachelier en diététique");
    OptionDto optionBim = (OptionDto) entityFactory.build(OptionDto.class);
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
