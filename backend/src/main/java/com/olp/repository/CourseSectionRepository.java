package com.olp.repository;

import com.olp.entity.CourseSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseSectionRepository extends JpaRepository<CourseSection, Integer> {

    @Query("SELECT s FROM CourseSection s WHERE s.module.id = :moduleId ORDER BY s.orderIndex ASC")
    List<CourseSection> findByModuleIdOrderByOrderIndexAsc(@Param("moduleId") Integer moduleId);

    @Query("SELECT s FROM CourseSection s WHERE s.id = :id AND s.module.id = :moduleId")
    Optional<CourseSection> findByIdAndModuleId(@Param("id") Integer id, @Param("moduleId") Integer moduleId);

    @Query("SELECT COALESCE(MAX(s.orderIndex), 0) FROM CourseSection s WHERE s.module.id = :moduleId")
    int findMaxOrderIndexByModuleId(@Param("moduleId") Integer moduleId);
}

