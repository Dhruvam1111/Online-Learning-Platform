package com.olp.repository;

import com.olp.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {

    @Query(value = "SELECT c FROM Course c JOIN FETCH c.instructor WHERE c.status = :status",
           countQuery = "SELECT COUNT(c) FROM Course c WHERE c.status = :status")
    Page<Course> findByStatus(@Param("status") String status, Pageable pageable);

    @Query(value = "SELECT c FROM Course c JOIN FETCH c.instructor WHERE " +
                   "(:status IS NULL OR c.status = :status) AND " +
                   "(:search IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(c) FROM Course c WHERE " +
                        "(:status IS NULL OR c.status = :status) AND " +
                        "(:search IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Course> findCoursesWithFilters(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);
}
