package com.olp.repository;

import com.olp.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, Integer> {

    Optional<Badge> findByCode(String code);
}
