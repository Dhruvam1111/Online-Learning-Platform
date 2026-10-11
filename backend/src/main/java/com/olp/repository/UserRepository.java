package com.olp.repository;

import com.olp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Spring Data JPA generates the query automatically from the method name
    Optional<User> findByEmail(String email);

    // Used to check for duplicate email during registration
    boolean existsByEmail(String email);
}
