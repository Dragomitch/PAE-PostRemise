package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.persistence.jdbc.entity.OptionEntity;
import com.dragomitch.ipl.pae.persistence.jdbc.entity.UserEntity;
import com.dragomitch.ipl.pae.persistence.jdbc.repository.OptionRepository;
import com.dragomitch.ipl.pae.persistence.jdbc.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * {@link UserDao} on top of the Spring Data {@link UserRepository}. The optimistic lock is the
 * {@code @Version} of {@link UserEntity}; the registration date is never updated. The option of
 * the DTO carries its code and name, read from the {@link OptionRepository}.
 */
@Repository
class UserDaoImpl implements UserDao {

  private final EntityFactory entityFactory;
  private final UserRepository users;
  private final OptionRepository options;
  private final JdbcAggregateOperations aggregates;
  /** Column name of the users table to property of {@link UserEntity}, for {@link #findBy}. */
  private final Map<String, String> propertiesByColumn;

  UserDaoImpl(EntityFactory entityFactory, UserRepository users, OptionRepository options,
      JdbcAggregateOperations aggregates, RelationalMappingContext mappingContext) {
    this.entityFactory = entityFactory;
    this.users = users;
    this.options = options;
    this.aggregates = aggregates;
    this.propertiesByColumn = StreamSupport
        .stream(mappingContext.getRequiredPersistentEntity(UserEntity.class).spliterator(), false)
        .collect(Collectors.toUnmodifiableMap(
            property -> property.getColumnName().getReference(),
            RelationalPersistentProperty::getName));
  }

  @Override
  public UserDto create(UserDto user) {
    UserEntity created = DataAccess.call(() -> aggregates.insert(toEntity(user, null, 0)));
    user.setId(created.id());
    user.setVersion(created.version());
    return user;
  }

  @Override
  public UserDto findById(int id) {
    return DataAccess.call(() -> users.findById(id).map(this::toDto).orElse(null));
  }

  @Override
  public List<UserDto> findAll() {
    return DataAccess.call(() -> {
      Map<String, String> optionNames = options.findAll().stream()
          .collect(Collectors.toMap(OptionEntity::code, OptionEntity::name));
      return users.findAll().stream()
          .map(user -> toDto(user, optionNames.get(user.option().getId()))).toList();
    });
  }

  /**
   * {@inheritDoc}
   *
   * <p>{@code columnName} must be a column of the users table: it is resolved to a property of
   * the mapping instead of being concatenated into the SQL. Any other name is reported as a
   * database error, as the invalid SQL used to be.
   */
  @Override
  public UserDto findBy(String columnName, String columnValue) {
    return DataAccess.call(() -> {
      String property = propertiesByColumn.get(columnName);
      if (property == null) {
        throw new FatalException(FatalException.DATABASE_ERROR_MSG,
            new IllegalArgumentException("Unknown column of the users table: " + columnName));
      }
      Query query = Query.query(Criteria.where(property).is(columnValue)).limit(1);
      // limit(1): the first match, as usernames and e-mails are not unique in the schema
      return aggregates.findOne(query, UserEntity.class).map(this::toDto).orElse(null);
    });
  }

  @Override
  public void promoteToProfessor(int id) {
    DataAccess.run(() -> users.updateRole(id, UserDto.ROLE_PROFESSOR));
  }

  @Override
  public void update(UserDto user) {
    UserEntity updated =
        DataAccess.call(() -> aggregates.update(toEntity(user, user.getId(), user.getVersion())));
    user.setVersion(updated.version());
  }

  @Override
  public boolean isEmpty() {
    return DataAccess.call(() -> users.count() == 0);
  }

  private static UserEntity toEntity(UserDto user, Integer id, int version) {
    return new UserEntity(id, user.getUsername(), user.getLastName(), user.getFirstName(),
        user.getEmail(), user.getPassword(), user.getRole(),
        DataAccess.reference(user.getOption().getCode()), user.getRegistrationDate(), version);
  }

  private UserDto toDto(UserEntity entity) {
    return toDto(entity,
        options.findById(entity.option().getId()).map(OptionEntity::name).orElseThrow());
  }

  private UserDto toDto(UserEntity entity, String optionName) {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    user.setId(entity.id());
    user.setLastName(entity.lastName());
    user.setFirstName(entity.firstName());
    user.setUsername(entity.username());
    user.setPassword(entity.password());
    user.setEmail(entity.email());
    user.setRegistrationDate(entity.registrationDate());
    user.setRole(entity.role());
    user.setVersion(entity.version());
    OptionDto option = (OptionDto) entityFactory.build(OptionDto.class);
    option.setCode(entity.option().getId());
    option.setName(optionName);
    user.setOption(option);
    return user;
  }
}
