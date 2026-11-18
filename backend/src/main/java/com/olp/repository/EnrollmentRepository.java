package com.olp.repository;

import com.olp.dto.MyCourseResponse;
import com.olp.entity.Enrollment;
import com.olp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {

    /**
     * One query, no N+1: selects only the fields the dashboard needs.
     * Most recently enrolled courses come first.
     */
    @Query("SELECT new com.olp.dto.MyCourseResponse(c.id, c.title, i.name, e.enrolledAt) " +
           "FROM Enrollment e " +
           "JOIN e.course c " +
           "JOIN c.instructor i " +
           "WHERE e.student.id = :studentId " +
           "AND c.status <> com.olp.entity.CourseStatus.DRAFT " +
           "ORDER BY e.enrolledAt DESC")
    List<MyCourseResponse> findMyCourses(@Param("studentId") Integer studentId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Enrollment e WHERE e.student.id = :studentId AND e.course.id = :courseId")
    boolean existsByStudentIdAndCourseId(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.course.id = :courseId")
    Optional<Enrollment> findByStudentIdAndCourseId(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    @Modifying
    @Query("DELETE FROM Enrollment e WHERE e.student.id = :studentId AND e.course.id = :courseId")
    void deleteByStudentIdAndCourseId(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId")
    long countByCourseId(@Param("courseId") Integer courseId);

    @Query("SELECT e.course.id, COUNT(e) FROM Enrollment e WHERE e.course.id IN :courseIds GROUP BY e.course.id")
    List<Object[]> countEnrollmentsByCourseIds(@Param("courseIds") List<Integer> courseIds);

    @Query("SELECT e.student FROM Enrollment e WHERE e.course.id = :courseId ORDER BY e.enrolledAt ASC")
    List<User> findEnrolledStudentsByCourseId(@Param("courseId") Integer courseId);

    @Query(value = "SELECT e FROM Enrollment e JOIN FETCH e.student s WHERE e.course.id = :courseId " +
                   "AND (:search IS NULL OR TRIM(:search) = '' OR " +
                   "LOWER(s.name) LIKE LOWER(CONCAT('%', TRIM(:search), '%')) OR " +
                   "LOWER(s.email) LIKE LOWER(CONCAT('%', TRIM(:search), '%')))",
           countQuery = "SELECT COUNT(e) FROM Enrollment e JOIN e.student s WHERE e.course.id = :courseId " +
                        "AND (:search IS NULL OR TRIM(:search) = '' OR " +
                        "LOWER(s.name) LIKE LOWER(CONCAT('%', TRIM(:search), '%')) OR " +
                        "LOWER(s.email) LIKE LOWER(CONCAT('%', TRIM(:search), '%')))")
    Page<Enrollment> findByCourseIdWithSearch(
            @Param("courseId") Integer courseId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.student s WHERE e.course.id = :courseId ORDER BY e.enrolledAt DESC")
    List<Enrollment> findByCourseIdOrderByEnrolledAtDesc(@Param("courseId") Integer courseId);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.student s WHERE e.course.id = :courseId " +
           "AND (:search IS NULL OR TRIM(:search) = '' OR " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', TRIM(:search), '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', TRIM(:search), '%')))")
    List<Enrollment> findAllByCourseIdWithSearch(
            @Param("courseId") Integer courseId,
            @Param("search") String search);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId AND e.enrolledAt >= :since")
    long countByCourseIdAndEnrolledAtGreaterThanEqual(
            @Param("courseId") Integer courseId,
            @Param("since") java.time.LocalDateTime since);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId AND e.enrolledAt >= :startDate AND e.enrolledAt <= :endDate")
    long countByCourseIdAndEnrolledAtBetween(
            @Param("courseId") Integer courseId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId AND e.enrolledAt < :before")
    long countByCourseIdAndEnrolledAtBefore(
            @Param("courseId") Integer courseId,
            @Param("before") java.time.LocalDateTime before);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id IN :courseIds AND e.enrolledAt >= :since")
    long countByCourseIdInAndEnrolledAtGreaterThanEqual(
            @Param("courseIds") java.util.Collection<Integer> courseIds,
            @Param("since") java.time.LocalDateTime since);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id IN :courseIds AND e.enrolledAt >= :startDate AND e.enrolledAt <= :endDate")
    long countByCourseIdInAndEnrolledAtBetween(
            @Param("courseIds") java.util.Collection<Integer> courseIds,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id IN :courseIds AND e.enrolledAt < :before")
    long countByCourseIdInAndEnrolledAtBefore(
            @Param("courseIds") java.util.Collection<Integer> courseIds,
            @Param("before") java.time.LocalDateTime before);

    @Query("SELECT e.enrolledAt FROM Enrollment e WHERE e.course.id = :courseId AND e.enrolledAt >= :startDate AND e.enrolledAt <= :endDate ORDER BY e.enrolledAt ASC")
    List<java.time.LocalDateTime> findEnrolledAtByCourseIdAndRange(
            @Param("courseId") Integer courseId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT e.enrolledAt FROM Enrollment e WHERE e.course.id IN :courseIds AND e.enrolledAt >= :startDate AND e.enrolledAt <= :endDate ORDER BY e.enrolledAt ASC")
    List<java.time.LocalDateTime> findEnrolledAtByCourseIdsAndRange(
            @Param("courseIds") java.util.Collection<Integer> courseIds,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);
}
