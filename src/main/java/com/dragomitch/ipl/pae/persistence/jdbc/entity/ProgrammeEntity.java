package com.dragomitch.ipl.pae.persistence.jdbc.entity;

import com.dragomitch.ipl.pae.persistence.jdbc.JdbcPersistenceConfig;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** A row of {@code programmes}: read-only reference data (Erasmus+, Erabel, FAME). */
@Table(schema = JdbcPersistenceConfig.SCHEMA, name = "programmes")
public record ProgrammeEntity(@Id @Column("programme_id") Integer id, String name,
    String externalSoftwareName) {
}
