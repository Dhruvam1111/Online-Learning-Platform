package com.olp.repository;

import com.olp.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing examinations (FR-I25, Step 38).
 */
@Repository
public interface ExamRepository extends JpaRepository<Exam, Integer> {

    @Query("SELECT e FROM Exam e WHERE e.course.id = :courseId ORDER BY e.startWindow ASC")
    List<Exam> findByCourseId(@Param("courseId") Integer courseId);

    @Query("SELECT e FROM Exam e WHERE e.course.id = :courseId AND e.status = :status ORDER BY e.startWindow ASC")
    List<Exam> findByCourseIdAndStatus(@Param("courseId") Integer courseId, @Param("status") String status);

    @Query("SELECT e FROM Exam e WHERE e.course.id = :courseId AND e.status IN :statuses ORDER BY e.startWindow ASC")
    List<Exam> findByCourseIdAndStatusIn(@Param("courseId") Integer courseId, @Param("statuses") Collection<String> statuses);

    @Query("SELECT e FROM Exam e WHERE e.id = :id AND e.course.id = :courseId")
    Optional<Exam> findByIdAndCourseId(@Param("id") Integer id, @Param("courseId") Integer courseId);

    @Query("SELECT COUNT(e) FROM Exam e WHERE e.course.id = :courseId")
    long countByCourseId(@Param("courseId") Integer courseId);
}

