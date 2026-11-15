package com.olp.repository;

import com.olp.entity.Certificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for certificate tracking, reporting, and validation (FR-I12, FR-I56, FR-I57, DR-ED09).
 */
@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Integer> {

    long countByCourseId(Integer courseId);

    boolean existsByCourseId(Integer courseId);

    List<Certificate> findByCourseId(Integer courseId);

    List<Certificate> findByStudentId(Integer studentId);

    boolean existsByCourseIdAndStudentId(Integer courseId, Integer studentId);

    @Query("SELECT c FROM Certificate c JOIN FETCH c.course cr JOIN FETCH c.student s WHERE c.certificateCode = :certificateCode")
    Optional<Certificate> findByCertificateCode(@Param("certificateCode") String certificateCode);

    @Query(value = "SELECT c FROM Certificate c JOIN FETCH c.course cr JOIN FETCH c.student s " +
                   "WHERE cr.id IN :courseIds " +
                   "AND (CAST(:startDate AS timestamp) IS NULL OR c.issuedAt >= :startDate) " +
                   "AND (CAST(:endDate AS timestamp) IS NULL OR c.issuedAt <= :endDate) " +
                   "ORDER BY c.issuedAt DESC, c.id DESC",
           countQuery = "SELECT COUNT(c) FROM Certificate c " +
                        "WHERE c.course.id IN :courseIds " +
                        "AND (CAST(:startDate AS timestamp) IS NULL OR c.issuedAt >= :startDate) " +
                        "AND (CAST(:endDate AS timestamp) IS NULL OR c.issuedAt <= :endDate)")
    Page<Certificate> findIssuedCertificates(
            @Param("courseIds") Collection<Integer> courseIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT c FROM Certificate c JOIN FETCH c.course cr JOIN FETCH c.student s " +
           "WHERE cr.id IN :courseIds " +
           "AND (CAST(:startDate AS timestamp) IS NULL OR c.issuedAt >= :startDate) " +
           "AND (CAST(:endDate AS timestamp) IS NULL OR c.issuedAt <= :endDate) " +
           "ORDER BY c.issuedAt DESC, c.id DESC")
    List<Certificate> findIssuedCertificatesList(
            @Param("courseIds") Collection<Integer> courseIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
