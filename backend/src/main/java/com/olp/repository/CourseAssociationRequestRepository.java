package com.olp.repository;

import com.olp.entity.AssociationRequestStatus;
import com.olp.entity.CourseAssociationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseAssociationRequestRepository extends JpaRepository<CourseAssociationRequest, Integer> {

    @Query("SELECT r FROM CourseAssociationRequest r " +
           "JOIN FETCH r.course c " +
           "JOIN FETCH c.instructor " +
           "JOIN FETCH r.instructor " +
           "LEFT JOIN FETCH r.reviewedBy " +
           "WHERE r.instructor.id = :instructorId " +
           "ORDER BY r.createdAt DESC, r.id DESC")
    List<CourseAssociationRequest> findByInstructorIdOrderByCreatedAtDesc(@Param("instructorId") Integer instructorId);

    @Query("SELECT r FROM CourseAssociationRequest r " +
           "JOIN FETCH r.course c " +
           "JOIN FETCH c.instructor " +
           "JOIN FETCH r.instructor " +
           "LEFT JOIN FETCH r.reviewedBy " +
           "WHERE r.instructor.id = :instructorId AND r.status = :status " +
           "ORDER BY r.createdAt DESC, r.id DESC")
    List<CourseAssociationRequest> findByInstructorIdAndStatusOrderByCreatedAtDesc(
            @Param("instructorId") Integer instructorId,
            @Param("status") AssociationRequestStatus status);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
           "FROM CourseAssociationRequest r " +
           "WHERE r.course.id = :courseId AND r.instructor.id = :instructorId AND r.status = :status")
    boolean existsByCourseIdAndInstructorIdAndStatus(
            @Param("courseId") Integer courseId,
            @Param("instructorId") Integer instructorId,
            @Param("status") AssociationRequestStatus status);

    @Query("SELECT r FROM CourseAssociationRequest r " +
           "JOIN FETCH r.course c " +
           "JOIN FETCH c.instructor " +
           "JOIN FETCH r.instructor " +
           "LEFT JOIN FETCH r.reviewedBy " +
           "WHERE r.status = :status " +
           "ORDER BY r.createdAt DESC, r.id DESC")
    List<CourseAssociationRequest> findByStatusOrderByCreatedAtDesc(@Param("status") AssociationRequestStatus status);

    @Query("SELECT r FROM CourseAssociationRequest r " +
           "JOIN FETCH r.course c " +
           "JOIN FETCH c.instructor " +
           "JOIN FETCH r.instructor " +
           "LEFT JOIN FETCH r.reviewedBy " +
           "ORDER BY r.createdAt DESC, r.id DESC")
    List<CourseAssociationRequest> findAllWithDetailsOrderByCreatedAtDesc();

    @Query("SELECT r FROM CourseAssociationRequest r " +
           "JOIN FETCH r.course c " +
           "JOIN FETCH c.instructor " +
           "JOIN FETCH r.instructor " +
           "LEFT JOIN FETCH r.reviewedBy " +
           "WHERE r.id = :id")
    Optional<CourseAssociationRequest> findByIdWithDetails(@Param("id") Integer id);

    @Query("SELECT r FROM CourseAssociationRequest r " +
           "JOIN FETCH r.course c " +
           "JOIN FETCH c.instructor " +
           "JOIN FETCH r.instructor " +
           "LEFT JOIN FETCH r.reviewedBy " +
           "WHERE r.course.id = :courseId " +
           "ORDER BY r.createdAt DESC, r.id DESC")
    List<CourseAssociationRequest> findByCourseIdOrderByCreatedAtDesc(@Param("courseId") Integer courseId);
}

