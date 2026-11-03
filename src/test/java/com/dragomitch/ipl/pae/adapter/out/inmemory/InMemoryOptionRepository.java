package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.model.Option;
import com.dragomitch.ipl.pae.domain.port.out.OptionRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryOptionRepository implements OptionRepository {

  private Map<String, Option> options;

  /** Sole constructor. */
  public InMemoryOptionRepository() {
    options = new HashMap<String, Option>();
    Option optionBin = new Option();
    optionBin.setCode("BIN");
    optionBin.setName("Bachelier en informatique de gestion");
    Option optionBbm = new Option();
    optionBbm.setCode("BBM");
    optionBbm.setName("Bachelier en biologie médicale");
    Option optionBch = new Option();
    optionBch.setCode("BCH");
    optionBch.setName("Bachelier en chimie");
    Option optionBdi = new Option();
    optionBdi.setCode("BDI");
    optionBdi.setName("Bachelier en diététique");
    Option optionBim = new Option();
    optionBim.setCode("BIM");
    optionBim.setName("Bachelier en imagerie médicale");
    options.put("BIN", optionBin);
    options.put("BBM", optionBbm);
    options.put("BCH", optionBch);
    options.put("BDI", optionBdi);
    options.put("BIM", optionBim);
  }

  @Override
  public List<Option> findAll() {
    List<Option> optionsList = new ArrayList<Option>();
    for (Option option : options.values()) {
      optionsList.add(option);
    }
    return optionsList;
  }

  @Override
  public Option findByCode(String code) {
    return options.get(code);
  }
}
