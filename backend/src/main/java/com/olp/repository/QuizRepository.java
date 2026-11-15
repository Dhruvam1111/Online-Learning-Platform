package com.olp.repository;

import com.olp.entity.Quiz;
import com.olp.entity.QuizStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Integer> {

    @Query("SELECT q FROM Quiz q WHERE q.course.id = :courseId ORDER BY q.createdAt DESC, q.id DESC")
    List<Quiz> findByCourseIdOrderByCreatedAtDesc(@Param("courseId") Integer courseId);

    @Query("SELECT q FROM Quiz q WHERE q.course.id = :courseId AND q.status = :status ORDER BY q.createdAt DESC, q.id DESC")
    List<Quiz> findByCourseIdAndStatusOrderByCreatedAtDesc(@Param("courseId") Integer courseId,
                                                           @Param("status") QuizStatus status);

    @Query("SELECT q FROM Quiz q WHERE q.module.id = :moduleId ORDER BY q.createdAt DESC, q.id DESC")
    List<Quiz> findByModuleIdOrderByCreatedAtDesc(@Param("moduleId") Integer moduleId);

    @Query("SELECT q FROM Quiz q WHERE q.lesson.id = :lessonId ORDER BY q.createdAt DESC, q.id DESC")
    List<Quiz> findByLessonIdOrderByCreatedAtDesc(@Param("lessonId") Integer lessonId);

    @Query("SELECT q FROM Quiz q JOIN FETCH q.course LEFT JOIN FETCH q.module LEFT JOIN FETCH q.lesson LEFT JOIN FETCH q.createdBy WHERE q.id = :id")
    Optional<Quiz> findByIdWithDetails(@Param("id") Integer id);

    @Query("SELECT q FROM Quiz q WHERE q.course.instructor.id = :instructorId " +
            "OR EXISTS (SELECT ci FROM CourseInstructor ci WHERE ci.course.id = q.course.id AND ci.instructor.id = :instructorId) " +
            "ORDER BY q.createdAt DESC, q.id DESC")
    List<Quiz> findByAssociatedInstructorId(@Param("instructorId") Integer instructorId);

    @Query("SELECT q FROM Quiz q WHERE (q.course.instructor.id = :instructorId " +
            "OR EXISTS (SELECT ci FROM CourseInstructor ci WHERE ci.course.id = q.course.id AND ci.instructor.id = :instructorId)) " +
            "AND q.status = :status ORDER BY q.createdAt DESC, q.id DESC")
    List<Quiz> findByAssociatedInstructorIdAndStatus(@Param("instructorId") Integer instructorId,
                                                     @Param("status") QuizStatus status);

    @Query("SELECT q FROM Quiz q WHERE q.course.id = :courseId AND q.negativeMarking = true ORDER BY q.createdAt DESC")
    List<Quiz> findByCourseIdAndNegativeMarkingTrue(@Param("courseId") Integer courseId);

    @Query("SELECT q FROM Quiz q WHERE q.course.id = :courseId AND q.partialMarking = true ORDER BY q.createdAt DESC")
    List<Quiz> findByCourseIdAndPartialMarkingTrue(@Param("courseId") Integer courseId);

    @Query("SELECT q FROM Quiz q WHERE q.course.id = :courseId AND q.isExam = true ORDER BY q.createdAt DESC")
    List<Quiz> findByCourseIdAndIsExamTrue(@Param("courseId") Integer courseId);

    @Query("SELECT q FROM Quiz q WHERE q.course.id = :courseId AND q.answerReleaseTiming = :timing ORDER BY q.createdAt DESC")
    List<Quiz> findByCourseIdAndAnswerReleaseTiming(@Param("courseId") Integer courseId,
                                                    @Param("timing") String timing);
}

