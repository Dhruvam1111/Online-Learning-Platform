
package com.olp.repository;

import com.olp.entity.Course;
import com.olp.entity.CourseStatus;
import com.olp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {

    // Instructor-specific course management
    List<Course> findByInstructor(User instructor);

    List<Course> findByInstructorId(Integer instructorId);

    List<Course> findByInstructorIdOrderByIdAsc(Integer instructorId);

    Optional<Course> findByIdAndInstructorId(Integer id, Integer instructorId);

    boolean existsByIdAndInstructorId(Integer id, Integer instructorId);

    // Paginated course retrieval by status
    @Query(
        value = "SELECT c FROM Course c JOIN FETCH c.instructor WHERE c.status = :status",
        countQuery = "SELECT COUNT(c) FROM Course c WHERE c.status = :status"
    )
    Page<Course> findByStatus(
        @Param("status") CourseStatus status,
        Pageable pageable
    );

    // Paginated course search and filtering
    @Query(
        value = "SELECT c FROM Course c JOIN FETCH c.instructor WHERE " +
                "(:status IS NULL OR c.status = :status) AND " +
                "(CAST(:search AS string) IS NULL OR " +
                "LOWER(c.title) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%') OR " +
                "LOWER(c.description) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))",
        countQuery = "SELECT COUNT(c) FROM Course c WHERE " +
                     "(:status IS NULL OR c.status = :status) AND " +
                     "(CAST(:search AS string) IS NULL OR " +
                     "LOWER(c.title) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%') OR " +
                     "LOWER(c.description) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))"
    )
    Page<Course> findCoursesWithFilters(
        @Param("status") CourseStatus status,
        @Param("search") String search,
        Pageable pageable
    );

    // Search courses available for instructor association (FR-I58)
    @Query("SELECT c FROM Course c JOIN FETCH c.instructor WHERE c.status = :status ORDER BY c.id ASC")
    List<Course> findByCourseStatusOrderByIdAsc(@Param("status") CourseStatus status);

    @Query("SELECT c FROM Course c JOIN FETCH c.instructor WHERE c.status = :status AND (" +
           "LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(COALESCE(c.category, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.id ASC")
    List<Course> searchByCourseStatusAndKeyword(
        @Param("status") CourseStatus status,
        @Param("search") String search
    );

    // FR-I60: Paginated retrieval of courses associated with an instructor (OWNER or CO_INSTRUCTOR)
    @Query(
        value = "SELECT c FROM Course c JOIN FETCH c.instructor " +
                "WHERE (c.instructor.id = :instructorId OR EXISTS (" +
                "  SELECT ci.id FROM CourseInstructor ci " +
                "  WHERE ci.course.id = c.id AND ci.instructor.id = :instructorId" +
                "))",
        countQuery = "SELECT COUNT(c) FROM Course c " +
                     "WHERE (c.instructor.id = :instructorId OR EXISTS (" +
                     "  SELECT ci.id FROM CourseInstructor ci " +
                     "  WHERE ci.course.id = c.id AND ci.instructor.id = :instructorId" +
                     "))"
    )
    Page<Course> findAssociatedCoursesByInstructor(
        @Param("instructorId") Integer instructorId,
        Pageable pageable
    );

    @Query(
        value = "SELECT c FROM Course c JOIN FETCH c.instructor " +
                "WHERE (c.instructor.id = :instructorId OR EXISTS (" +
                "  SELECT ci.id FROM CourseInstructor ci " +
                "  WHERE ci.course.id = c.id AND ci.instructor.id = :instructorId" +
                ")) AND c.status = :status",
        countQuery = "SELECT COUNT(c) FROM Course c " +
                     "WHERE (c.instructor.id = :instructorId OR EXISTS (" +
                     "  SELECT ci.id FROM CourseInstructor ci " +
                     "  WHERE ci.course.id = c.id AND ci.instructor.id = :instructorId" +
                     ")) AND c.status = :status"
    )
    Page<Course> findAssociatedCoursesByInstructorAndStatus(
        @Param("instructorId") Integer instructorId,
        @Param("status") CourseStatus status,
        Pageable pageable
    );

    @Query(
        value = "SELECT c FROM Course c JOIN FETCH c.instructor " +
                "WHERE (c.instructor.id = :instructorId OR EXISTS (" +
                "  SELECT ci.id FROM CourseInstructor ci " +
                "  WHERE ci.course.id = c.id AND ci.instructor.id = :instructorId" +
                ")) AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
                "       OR LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
                "       OR LOWER(COALESCE(c.category, '')) LIKE LOWER(CONCAT('%', :search, '%')))",
        countQuery = "SELECT COUNT(c) FROM Course c " +
                     "WHERE (c.instructor.id = :instructorId OR EXISTS (" +
                     "  SELECT ci.id FROM CourseInstructor ci " +
                     "  WHERE ci.course.id = c.id AND ci.instructor.id = :instructorId" +
                     ")) AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
                     "       OR LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
                     "       OR LOWER(COALESCE(c.category, '')) LIKE LOWER(CONCAT('%', :search, '%')))"
    )
    Page<Course> searchAssociatedCoursesByInstructor(
        @Param("instructorId") Integer instructorId,
        @Param("search") String search,
        Pageable pageable
    );

    @Query(
        value = "SELECT c FROM Course c JOIN FETCH c.instructor " +
                "WHERE (c.instructor.id = :instructorId OR EXISTS (" +
                "  SELECT ci.id FROM CourseInstructor ci " +
                "  WHERE ci.course.id = c.id AND ci.instructor.id = :instructorId" +
                ")) AND c.status = :status " +
                "AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
                "     OR LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
                "     OR LOWER(COALESCE(c.category, '')) LIKE LOWER(CONCAT('%', :search, '%')))",
        countQuery = "SELECT COUNT(c) FROM Course c " +
                     "WHERE (c.instructor.id = :instructorId OR EXISTS (" +
                     "  SELECT ci.id FROM CourseInstructor ci " +
                     "  WHERE ci.course.id = c.id AND ci.instructor.id = :instructorId" +
                     ")) AND c.status = :status " +
                     "AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
                     "     OR LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
                     "     OR LOWER(COALESCE(c.category, '')) LIKE LOWER(CONCAT('%', :search, '%')))"
    )
    Page<Course> searchAssociatedCoursesByInstructorAndStatus(
        @Param("instructorId") Integer instructorId,
        @Param("status") CourseStatus status,
        @Param("search") String search,
        Pageable pageable
    );
}
