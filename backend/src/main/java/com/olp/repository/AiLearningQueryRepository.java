package com.olp.repository;

import com.olp.entity.AiLearningQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiLearningQueryRepository extends JpaRepository<AiLearningQuery, Integer> {

    @Query("SELECT q FROM AiLearningQuery q WHERE q.student.id = :studentId AND q.course.id = :courseId ORDER BY q.createdAt DESC")
    List<AiLearningQuery> findByStudentIdAndCourseId(@Param("studentId") Integer studentId,
                                                    @Param("courseId") Integer courseId);
}
