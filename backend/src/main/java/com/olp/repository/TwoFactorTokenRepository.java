package com.olp.repository;

import com.olp.entity.TwoFactorToken;
import com.olp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface TwoFactorTokenRepository extends JpaRepository<TwoFactorToken, Long> {

    @Query("""
            SELECT t FROM TwoFactorToken t
            WHERE t.user = :user
              AND t.purpose = :purpose
              AND t.used = false
            ORDER BY t.createdAt DESC
            LIMIT 1
            """)
    Optional<TwoFactorToken> findLatestUnusedByUserAndPurpose(
            @Param("user") User user,
            @Param("purpose") String purpose);

    @Modifying
    @Transactional
    @Query("UPDATE TwoFactorToken t SET t.used = true WHERE t.user = :user AND t.purpose = :purpose AND t.used = false")
    void invalidateAllForUserAndPurpose(
            @Param("user") User user,
            @Param("purpose") String purpose);
}
