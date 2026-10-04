package com.dragomitch.ipl.pae.persistence.jdbc.repository;

import com.dragomitch.ipl.pae.persistence.jdbc.entity.AddressEntity;

import org.springframework.data.repository.ListCrudRepository;

/**
 * Addresses. Inserts and version-checked updates go through {@code JdbcAggregateOperations}
 * ({@code insert}/{@code update}): {@code save} would decide between them from the version.
 */
public interface AddressRepository extends ListCrudRepository<AddressEntity, Integer> {
}
