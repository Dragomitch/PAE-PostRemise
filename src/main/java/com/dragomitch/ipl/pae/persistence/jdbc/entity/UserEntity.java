package com.dragomitch.ipl.pae.persistence.jdbc.entity;

import com.dragomitch.ipl.pae.persistence.jdbc.JdbcPersistenceConfig;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.InsertOnlyProperty;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A row of {@code users}, the option being referenced by its code.
 *
 * <p>The registration date is written once, when the user is created ({@link InsertOnlyProperty}).
 * {@code version} is a primitive {@link Version}: inserted as 1, checked and incremented by every
 * update.
 */
@Table(schema = JdbcPersistenceConfig.SCHEMA, name = "users")
public record UserEntity(@Id @Column("user_id") Integer id, String username, String lastName,
    String firstName, String email, String password, String role,
    @Column("option") AggregateReference<OptionEntity, String> option,
    @InsertOnlyProperty LocalDateTime registrationDate, @Version int version) {
}
