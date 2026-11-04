package com.dragomitch.ipl.pae.domain.port.out;

import com.dragomitch.ipl.pae.domain.model.Mobility;
import java.util.List;
import java.util.Optional;

public interface MobilityRepository {

  /**
   * Inserts a mobility in db.
   *
   * @param mobility the mobility to insert
   * @return the created mobility
   */
  Mobility create(Mobility mobility);

  /**
   * Finds a mobility in db.
   *
   * @param id the mobility id to be found
   * @return the mobility, or empty if there is none
   */
  Optional<Mobility> findById(int id);

  /**
   * Lists all mobilities stored in db.
   *
   * @return a list of all mobilities
   */
  List<Mobility> findAll();

  /**
   * Lists all mobilities owned by a user.
   *
   * @param user the user id
   * @return a list of mobilities
   */
  List<Mobility> findByUser(int user);

  /**
   * Updates a mobility in db.
   *
   * @param mobility the mobility to update
   * @return the updated mobility
   */
  Mobility update(Mobility mobility);
}
