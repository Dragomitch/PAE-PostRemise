package com.dragomitch.ipl.pae;

import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.dragomitch.ipl.pae.business.Address;
import com.dragomitch.ipl.pae.business.DtoFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DtoFactoryTest {

  @Autowired
  private DtoFactory factory;

  @Test
  void prototypesReturned() {
    Address a1 = factory.create(Address.class);
    Address a2 = factory.create(Address.class);
    assertNotSame(a1, a2);
  }
}
