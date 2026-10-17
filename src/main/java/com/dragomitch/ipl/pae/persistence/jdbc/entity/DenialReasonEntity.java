package com.dragomitch.ipl.pae.persistence.jdbc.entity;

import com.dragomitch.ipl.pae.persistence.jdbc.JdbcPersistenceConfig;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** A row of {@code denial_reasons}. The table has no version column. */
@Table(schema = JdbcPersistenceConfig.SCHEMA, name = "denial_reasons")
public record DenialReasonEntity(@Id @Column("reason_id") Integer id, String reason) {
}
