package com.dragomitch.ipl.pae.persistence.jdbc.entity;

import com.dragomitch.ipl.pae.persistence.jdbc.JdbcPersistenceConfig;

import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A row of {@code countries}: read-only reference data. The programme is another aggregate and
 * is referenced by id. The {@code version} column (nullable, never written) is not mapped.
 */
@Table(schema = JdbcPersistenceConfig.SCHEMA, name = "countries")
public record CountryEntity(@Id @Column("country_code") String code, String name,
    @Column("programme_id") AggregateReference<ProgrammeEntity, Integer> programme) {
}
