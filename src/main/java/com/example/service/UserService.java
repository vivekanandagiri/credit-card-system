package com.example.service;

import com.example.entity.User;

import java.util.List;
import java.util.UUID;

/**
 * Internal service interface for user and customer entity resolution.
 *
 */
public interface UserService {

    /**
     * Returns the {@link User} entity for the given ID.
     *
     * @throws UserNotFoundException if no user exists with this ID
     */
    User getUser(UUID userId);


	List<User> getAllUsers();
}