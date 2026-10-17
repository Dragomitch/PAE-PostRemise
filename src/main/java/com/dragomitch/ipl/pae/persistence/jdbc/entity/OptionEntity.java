package com.dragomitch.ipl.pae.persistence.jdbc.entity;

import com.dragomitch.ipl.pae.persistence.jdbc.JdbcPersistenceConfig;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** A row of {@code options}: read-only reference data (the IPL options). */
@Table(schema = JdbcPersistenceConfig.SCHEMA, name = "options")
public record OptionEntity(@Id @Column("option_code") String code, String name) {
}
