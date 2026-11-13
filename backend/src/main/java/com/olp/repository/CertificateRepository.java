package com.olp.repository;

import com.olp.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for certificate tracking and validation (FR-I12, DR-ED09).
 */
@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Integer> {

    long countByCourseId(Integer courseId);

    boolean existsByCourseId(Integer courseId);

    List<Certificate> findByCourseId(Integer courseId);

    List<Certificate> findByStudentId(Integer studentId);

    boolean existsByCourseIdAndStudentId(Integer courseId, Integer studentId);
}

