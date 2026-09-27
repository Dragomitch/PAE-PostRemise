package com.dragomitch.ipl.pae.persistence.jdbc.entity;

import com.dragomitch.ipl.pae.persistence.jdbc.JdbcPersistenceConfig;

import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A row of {@code documents}: the read-only list of documents each programme requires,
 * {@code category} being 'D' (departure) or 'R' (return). The {@code version} column is never
 * written and is not mapped.
 */
@Table(schema = JdbcPersistenceConfig.SCHEMA, name = "documents")
public record DocumentEntity(@Id @Column("document_id") Integer id, String name, String category,
    @Column("programme_id") AggregateReference<ProgrammeEntity, Integer> programme) {
}
