package com.olp.repository;

import com.olp.entity.GradingAlertType;
import com.olp.entity.PendingGradingAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing pending grading alerts and notifications (FR-I33, Step 50).
 */
@Repository
public interface PendingGradingAlertRepository extends JpaRepository<PendingGradingAlert, Integer> {

    @Query("SELECT a FROM PendingGradingAlert a " +
           "JOIN FETCH a.course c " +
           "WHERE a.instructor.id = :instructorId " +
           "ORDER BY a.sentAt DESC")
    List<PendingGradingAlert> findByInstructorIdOrderBySentAtDesc(@Param("instructorId") Integer instructorId);

    @Query("SELECT a FROM PendingGradingAlert a " +
           "JOIN FETCH a.course c " +
           "WHERE a.instructor.id = :instructorId AND a.isRead = :isRead " +
           "ORDER BY a.sentAt DESC")
    List<PendingGradingAlert> findByInstructorIdAndIsReadOrderBySentAtDesc(
            @Param("instructorId") Integer instructorId,
            @Param("isRead") Boolean isRead);

    @Query("SELECT COUNT(a) FROM PendingGradingAlert a " +
           "WHERE a.instructor.id = :instructorId AND a.isRead = false")
    long countUnreadByInstructorId(@Param("instructorId") Integer instructorId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM PendingGradingAlert a " +
           "WHERE a.instructor.id = :instructorId " +
           "AND a.course.id = :courseId " +
           "AND a.alertType = :alertType " +
           "AND a.digestDate = :digestDate")
    boolean existsByInstructorIdAndCourseIdAndAlertTypeAndDigestDate(
            @Param("instructorId") Integer instructorId,
            @Param("courseId") Integer courseId,
            @Param("alertType") GradingAlertType alertType,
            @Param("digestDate") LocalDate digestDate);

    @Query("SELECT a FROM PendingGradingAlert a " +
           "JOIN FETCH a.course c " +
           "WHERE a.id = :id AND a.instructor.id = :instructorId")
    Optional<PendingGradingAlert> findByIdAndInstructorId(
            @Param("id") Integer id,
            @Param("instructorId") Integer instructorId);
}
