package com.dragomitch.ipl.pae.persistence.jdbc.entity;

import com.dragomitch.ipl.pae.persistence.jdbc.JdbcPersistenceConfig;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A row of {@code addresses}, the country being referenced by its code.
 *
 * <p>{@code version} is a primitive {@link Version}: Spring Data JDBC inserts it as 1 and
 * updates with {@code WHERE address_id = ? AND version = ?}, incrementing it, or throws an
 * {@code OptimisticLockingFailureException} when no row matches.
 */
@Table(schema = JdbcPersistenceConfig.SCHEMA, name = "addresses")
public record AddressEntity(@Id @Column("address_id") Integer id, String street, String number,
    @Column("country") AggregateReference<CountryEntity, String> country, String city,
    String postalCode, String region, @Version int version) {
}
