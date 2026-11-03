package com.dragomitch.ipl.pae.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.domain.dto.AddressDto;
import com.dragomitch.ipl.pae.domain.dto.CountryDto;
import com.dragomitch.ipl.pae.domain.port.out.AddressRepository;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class AddressRepositoryIT extends AbstractRepositoryIT {

  private static final String ADDRESS_BY_ID =
      "SELECT * FROM student_exchange_tools.addresses WHERE address_id = ?";

  @Autowired
  private AddressRepository addressRepository;

  @Override
  protected List<String> fixtures() {
    return List.of("db/fixtures/partners.sql");
  }

  private AddressDto newAddress(String countryCode, String region) {
    AddressDto address = build(AddressDto.class);
    address.setStreet("Avenue Louise");
    address.setNumber("54a");
    CountryDto country = build(CountryDto.class);
    country.setCountryCode(countryCode);
    address.setCountry(country);
    address.setCity("Bruxelles");
    address.setPostalCode("1050");
    address.setRegion(region);
    return address;
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"Bruxelles-Capitale"})
  void createInsertsEveryColumnAndReturnsTheGeneratedIdWithVersionOne(String region) {
    AddressDto found = inTransaction(() -> {
      AddressDto created = addressRepository.create(newAddress("BE", region));

      assertThat(created.getId()).isGreaterThanOrEqualTo(100000);
      assertThat(created.getVersion()).isEqualTo(1);
      Map<String, Object> row = queryForRow(ADDRESS_BY_ID, created.getId());
      assertThat(row).containsEntry("street", "Avenue Louise").containsEntry("number", "54a")
          .containsEntry("country", "BE").containsEntry("city", "Bruxelles")
          .containsEntry("postal_code", "1050").containsEntry("region", region)
          .containsEntry("version", 1);
      return addressRepository.findById(created.getId());
    });

    assertThat(found.getRegion()).isEqualTo(region);
    assertThat(found.getCountry().getName()).isEqualTo("Belgique");
  }

  @Test
  void createWithAnUnknownCountryIsRejectedByTheForeignKey() {
    runInTransaction(() -> assertThatThrownBy(() -> addressRepository.create(newAddress("ZZ", null)))
        .isInstanceOf(DataIntegrityViolationException.class));
  }

  @Test
  void findByIdMapsEveryFieldAndJoinsTheCountryName() {
    AddressDto address = inTransaction(() -> addressRepository.findById(2001));

    assertThat(address.getId()).isEqualTo(2001);
    assertThat(address.getStreet()).isEqualTo("Rue de la République");
    assertThat(address.getNumber()).isEqualTo("12B");
    assertThat(address.getCity()).isEqualTo("Lyon");
    assertThat(address.getPostalCode()).isEqualTo("69002");
    assertThat(address.getRegion()).isEqualTo("Auvergne-Rhône-Alpes");
    assertThat(address.getVersion()).isEqualTo(1);
    assertThat(address.getCountry().getCountryCode()).isEqualTo("FR");
    assertThat(address.getCountry().getName()).isEqualTo("France");
  }

  @Test
  void findByIdKeepsAMissingRegionNull() {
    AddressDto address = inTransaction(() -> addressRepository.findById(2002));

    assertThat(address.getRegion()).isNull();
    assertThat(address.getVersion()).isEqualTo(4);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -5, 1999, 99999})
  void findByIdReturnsNullWhenAbsent(int id) {
    assertThat(inTransaction(() -> addressRepository.findById(id))).isNull();
  }

  @Test
  void updateWritesTheFieldsAndIncrementsTheVersion() {
    runInTransaction(() -> {
      AddressDto address = addressRepository.findById(2002);
      address.setStreet("Place Flagey");
      address.setNumber("7");
      address.getCountry().setCountryCode("FR");
      address.setCity("Lille");
      address.setPostalCode("59000");
      address.setRegion("Hauts-de-France");

      AddressDto updated = addressRepository.update(address);

      assertThat(updated).isSameAs(address);
      assertThat(updated.getVersion()).isEqualTo(5);
      assertThat(queryForRow(ADDRESS_BY_ID, 2002)).containsEntry("street", "Place Flagey")
          .containsEntry("number", "7").containsEntry("country", "FR")
          .containsEntry("city", "Lille").containsEntry("postal_code", "59000")
          .containsEntry("region", "Hauts-de-France").containsEntry("version", 5);
    });
  }

  @Test
  void updateCanClearTheRegion() {
    runInTransaction(() -> {
      AddressDto address = addressRepository.findById(2001);
      address.setRegion(null);
      addressRepository.update(address);

      assertThat(queryForRow(ADDRESS_BY_ID, 2001)).containsEntry("region", null)
          .containsEntry("version", 2);
    });
  }

  @Test
  void updateWithAStaleVersionFailsAndLeavesTheRowUntouched() {
    runInTransaction(() -> {
      AddressDto stale = addressRepository.findById(2002);
      stale.setVersion(3);
      stale.setCity("Namur");

      assertThatThrownBy(() -> addressRepository.update(stale))
          .isInstanceOf(ConcurrentModificationException.class);
      assertThat(queryForRow(ADDRESS_BY_ID, 2002)).containsEntry("city", "Bruxelles")
          .containsEntry("version", 4);
    });
  }

  @Test
  void updateOfAnUnknownAddressFails() {
    runInTransaction(() -> {
      AddressDto ghost = newAddress("BE", null);
      ghost.setId(777);
      ghost.setVersion(1);
      assertThatThrownBy(() -> addressRepository.update(ghost))
          .isInstanceOf(ConcurrentModificationException.class);
    });
  }
}
