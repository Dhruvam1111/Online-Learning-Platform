package com.olp.repository;

import com.olp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Spring Data JPA generates the query automatically from the method name
    Optional<User> findByEmail(String email);

    // Used to check for duplicate email during registration
    boolean existsByEmail(String email);

    // Find users by role and account status with pagination (e.g., pending instructors)
    Page<User> findByRoleAndAccountStatus(String role, String accountStatus, Pageable pageable);

    // Find users by account status with pagination
    Page<User> findByAccountStatus(String accountStatus, Pageable pageable);

    // Filter users by role, status, and search string (matching name or email)
    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:status IS NULL OR u.accountStatus = :status) AND " +
           "(:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findUsersWithFilters(
            @Param("role") String role,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);
}
