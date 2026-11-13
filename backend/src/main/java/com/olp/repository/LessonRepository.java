package com.olp.repository;

import com.olp.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Integer> {

    @Query("SELECT l FROM Lesson l WHERE l.section.id = :sectionId ORDER BY l.orderIndex ASC")
    List<Lesson> findBySectionIdOrderByOrderIndexAsc(@Param("sectionId") Integer sectionId);

    @Query("SELECT l FROM Lesson l WHERE l.id = :id AND l.section.id = :sectionId")
    Optional<Lesson> findByIdAndSectionId(@Param("id") Integer id, @Param("sectionId") Integer sectionId);

    @Query("SELECT COALESCE(MAX(l.orderIndex), 0) FROM Lesson l WHERE l.section.id = :sectionId")
    int findMaxOrderIndexBySectionId(@Param("sectionId") Integer sectionId);

    @Query("SELECT l FROM Lesson l WHERE l.section.module.course.id = :courseId ORDER BY l.section.module.orderIndex ASC, l.section.orderIndex ASC, l.orderIndex ASC")
    List<Lesson> findByCourseId(@Param("courseId") Integer courseId);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.section.module.course.id = :courseId")
    long countByCourseId(@Param("courseId") Integer courseId);
}

