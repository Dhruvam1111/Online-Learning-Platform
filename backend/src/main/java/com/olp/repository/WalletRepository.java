package com.olp.repository;

import com.olp.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id = :id")
    Optional<com.olp.entity.User> lockStudent(@org.springframework.data.repository.query.Param("id") Integer id);

    Optional<Wallet> findByStudentId(Integer studentId);
}
