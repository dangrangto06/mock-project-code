package com.codeforge.repository;

import com.codeforge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entity management.
 *
 * <p>Provides automated CRUD operations and custom query derivation methods
 * for user authentication, registration uniqueness checks, and profile lookups.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Retrieves a user entity matching the provided username.
     *
     * @param username The unique username to query.
     * @return An {@link Optional} containing the matched {@link User}, or empty if not found.
     */
    Optional<User> findByUsername(String username);

    /**
     * Retrieves a user entity matching the provided email address.
     *
     * @param email The unique email address to query.
     * @return An {@link Optional} containing the matched {@link User}, or empty if not found.
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user already exists with the given username.
     *
     * @param username The username to check for collision.
     * @return {@code true} if a user exists with this username; {@code false} otherwise.
     */
    Boolean existsByUsername(String username);

    /**
     * Checks if a user already exists with the given email address.
     *
     * @param email The email to check for collision.
     * @return {@code true} if a user exists with this email; {@code false} otherwise.
     */
    Boolean existsByEmail(String email);
}
