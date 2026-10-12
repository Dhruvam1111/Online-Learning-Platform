
package com.olp.repository;

import com.olp.entity.Course;
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
        @Param("status") String status,
        Pageable pageable
    );

    // Paginated course search and filtering
    @Query(
        value = "SELECT c FROM Course c JOIN FETCH c.instructor WHERE " +
                "(:status IS NULL OR c.status = :status) AND " +
                "(:search IS NULL OR " +
                "LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                "LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))",
        countQuery = "SELECT COUNT(c) FROM Course c WHERE " +
                     "(:status IS NULL OR c.status = :status) AND " +
                     "(:search IS NULL OR " +
                     "LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                     "LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))"
    )
    Page<Course> findCoursesWithFilters(
        @Param("status") String status,
        @Param("search") String search,
        Pageable pageable
    );
}
