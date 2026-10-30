package com.example.studentapp.repository;

import com.example.studentapp.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access for users. Each finder has no SQL written by hand.
 * Spring Data builds the query from the method name:
 *   findByUsername          → SELECT ... FROM app_users WHERE username = ?
 *   existsByUsername        → true or false
 *   existsByUsernameAndIdNot → true if another user (not this id) already has the username
 */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);
}
