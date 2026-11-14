package com.olp.repository;

import com.olp.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for tracking student lesson progress (FR-I56).
 */
@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Integer> {

    boolean existsByStudentIdAndLessonId(Integer studentId, Integer lessonId);

    boolean existsByLessonId(Integer lessonId);

    long countByLessonId(Integer lessonId);

    Optional<LessonProgress> findByStudentIdAndLessonId(Integer studentId, Integer lessonId);

    List<LessonProgress> findByStudentId(Integer studentId);

    @Query("SELECT lp.lessonId FROM LessonProgress lp WHERE lp.studentId = :studentId AND lp.lessonId IN :lessonIds AND lp.isCompleted = true")
    List<Integer> findCompletedLessonIdsByStudentId(@Param("studentId") Integer studentId, @Param("lessonIds") Collection<Integer> lessonIds);

    @Query("SELECT COUNT(lp) FROM LessonProgress lp WHERE lp.studentId = :studentId AND lp.lessonId IN :lessonIds AND lp.isCompleted = true")
    long countCompletedByStudentIdAndLessonIds(@Param("studentId") Integer studentId, @Param("lessonIds") Collection<Integer> lessonIds);
}
