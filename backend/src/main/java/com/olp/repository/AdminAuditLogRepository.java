package com.olp.repository;

import com.olp.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    @Query(value = "SELECT a FROM AdminAuditLog a " +
                   "LEFT JOIN FETCH a.actor " +
                   "WHERE (:actorId IS NULL OR a.actor.id = :actorId) " +
                   "AND (CAST(:action AS string) IS NULL OR LOWER(a.action) = LOWER(CAST(:action AS string))) " +
                   "AND (CAST(:targetType AS string) IS NULL OR LOWER(a.targetType) = LOWER(CAST(:targetType AS string))) " +
                   "AND (CAST(:startDate AS java.time.LocalDateTime) IS NULL OR a.createdAt >= :startDate) " +
                   "AND (CAST(:endDate AS java.time.LocalDateTime) IS NULL OR a.createdAt <= :endDate) " +
                   "AND (CAST(:search AS string) IS NULL OR " +
                   "     LOWER(a.targetName) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%') OR " +
                   "     LOWER(a.targetId) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%') OR " +
                   "     LOWER(a.details) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))",
           countQuery = "SELECT COUNT(a) FROM AdminAuditLog a " +
                   "WHERE (:actorId IS NULL OR a.actor.id = :actorId) " +
                   "AND (CAST(:action AS string) IS NULL OR LOWER(a.action) = LOWER(CAST(:action AS string))) " +
                   "AND (CAST(:targetType AS string) IS NULL OR LOWER(a.targetType) = LOWER(CAST(:targetType AS string))) " +
                   "AND (CAST(:startDate AS java.time.LocalDateTime) IS NULL OR a.createdAt >= :startDate) " +
                   "AND (CAST(:endDate AS java.time.LocalDateTime) IS NULL OR a.createdAt <= :endDate) " +
                   "AND (CAST(:search AS string) IS NULL OR " +
                   "     LOWER(a.targetName) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%') OR " +
                   "     LOWER(a.targetId) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%') OR " +
                   "     LOWER(a.details) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))")
    Page<AdminAuditLog> findWithFilters(
            @Param("actorId") Integer actorId,
            @Param("action") String action,
            @Param("targetType") String targetType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("search") String search,
            Pageable pageable);
}
