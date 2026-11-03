package com.dragomitch.ipl.pae.domain.model;

import com.dragomitch.ipl.pae.domain.dto.UserDto;

/**
 * Business object of a {@link UserDto}. Its validation rules are the Bean Validation constraints
 * declared on the getters of {@link UserDto}.
 */
public interface User extends UserDto {}
