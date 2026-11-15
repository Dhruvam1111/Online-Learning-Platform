package com.olp.repository;

import com.olp.entity.AssessmentType;
import com.olp.entity.ResultVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for result versions and change audit history (FR-I74, Step 49).
 * Strictly read-only for consumers; upholds DR-ED04.
 */
@Repository
public interface ResultVersionRepository extends JpaRepository<ResultVersion, Integer> {

    @Query("SELECT rv FROM ResultVersion rv " +
           "WHERE rv.course.id = :courseId " +
           "AND (:studentId IS NULL OR rv.student.id = :studentId) " +
           "AND (:assessmentType IS NULL OR rv.assessmentType = :assessmentType) " +
           "AND (:assessmentId IS NULL OR rv.assessmentId = :assessmentId) " +
           "ORDER BY rv.createdAt DESC")
    List<ResultVersion> findFiltered(
            @Param("courseId") Integer courseId,
            @Param("studentId") Integer studentId,
            @Param("assessmentType") AssessmentType assessmentType,
            @Param("assessmentId") Integer assessmentId
    );

    @Query("SELECT rv FROM ResultVersion rv WHERE rv.course.id = :courseId ORDER BY rv.createdAt DESC")
    List<ResultVersion> findByCourseIdOrderByCreatedAtDesc(@Param("courseId") Integer courseId);

    @Query("SELECT rv FROM ResultVersion rv WHERE rv.student.id = :studentId ORDER BY rv.createdAt DESC")
    List<ResultVersion> findByStudentIdOrderByCreatedAtDesc(@Param("studentId") Integer studentId);

    @Query("SELECT rv FROM ResultVersion rv " +
           "WHERE rv.assessmentType = :assessmentType AND rv.assessmentId = :assessmentId " +
           "ORDER BY rv.createdAt DESC")
    List<ResultVersion> findByAssessmentOrderByCreatedAtDesc(
            @Param("assessmentType") AssessmentType assessmentType,
            @Param("assessmentId") Integer assessmentId
    );
}
