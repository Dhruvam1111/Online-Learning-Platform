package com.olp.repository;

import com.olp.entity.AiLectureSynthesis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiLectureSynthesisRepository extends JpaRepository<AiLectureSynthesis, Integer> {

    @Query("SELECT s FROM AiLectureSynthesis s WHERE s.lessonId = :lessonId ORDER BY s.createdAt DESC")
    List<AiLectureSynthesis> findByLessonIdOrderByCreatedAtDesc(@Param("lessonId") Integer lessonId);

    @Query("SELECT s FROM AiLectureSynthesis s WHERE s.lessonId = :lessonId AND s.publishedToStudents = true ORDER BY s.publishedAt DESC")
    List<AiLectureSynthesis> findPublishedByLessonId(@Param("lessonId") Integer lessonId);

    @Query("SELECT s FROM AiLectureSynthesis s WHERE s.lessonId = :lessonId AND s.instructorId = :instructorId ORDER BY s.createdAt DESC")
    List<AiLectureSynthesis> findByLessonIdAndInstructorId(@Param("lessonId") Integer lessonId, @Param("instructorId") Integer instructorId);

    @Query("SELECT s FROM AiLectureSynthesis s WHERE s.id = :id AND s.instructorId = :instructorId")
    Optional<AiLectureSynthesis> findByIdAndInstructorId(@Param("id") Integer id, @Param("instructorId") Integer instructorId);
}
