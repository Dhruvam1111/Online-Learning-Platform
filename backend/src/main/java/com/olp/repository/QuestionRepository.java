package com.olp.repository;

import com.olp.entity.Question;
import com.olp.entity.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {

    @Query("SELECT q FROM Question q LEFT JOIN FETCH q.course LEFT JOIN FETCH q.createdBy WHERE q.id = :id")
    Optional<Question> findByIdWithDetails(@Param("id") Integer id);

    @Query("SELECT q FROM Question q WHERE q.course.id = :courseId ORDER BY q.createdAt DESC, q.id DESC")
    List<Question> findByCourseIdOrderByCreatedAtDesc(@Param("courseId") Integer courseId);

    @Query("SELECT q FROM Question q WHERE q.course.id = :courseId AND q.isBankQuestion = true ORDER BY q.createdAt DESC, q.id DESC")
    List<Question> findBankQuestionsByCourseId(@Param("courseId") Integer courseId);

    @Query("SELECT q FROM Question q WHERE q.createdBy.id = :instructorId ORDER BY q.createdAt DESC, q.id DESC")
    List<Question> findByCreatedByIdOrderByCreatedAtDesc(@Param("instructorId") Integer instructorId);

    @Query("SELECT q FROM Question q WHERE q.course.id = :courseId AND q.questionType = :questionType ORDER BY q.createdAt DESC, q.id DESC")
    List<Question> findByCourseIdAndQuestionTypeOrderByCreatedAtDesc(@Param("courseId") Integer courseId,
                                                                     @Param("questionType") QuestionType questionType);

    @Query("SELECT q FROM Question q WHERE q.createdBy.id = :instructorId " +
            "OR (q.course IS NOT NULL AND (q.course.instructor.id = :instructorId " +
            "OR EXISTS (SELECT ci FROM CourseInstructor ci WHERE ci.course.id = q.course.id AND ci.instructor.id = :instructorId))) " +
            "ORDER BY q.createdAt DESC, q.id DESC")
    List<Question> findAccessibleByInstructorId(@Param("instructorId") Integer instructorId);

    @Query("SELECT q FROM Question q WHERE q.requiresManualGrading = true AND (" +
            "q.createdBy.id = :instructorId " +
            "OR (q.course IS NOT NULL AND (q.course.instructor.id = :instructorId " +
            "OR EXISTS (SELECT ci FROM CourseInstructor ci WHERE ci.course.id = q.course.id AND ci.instructor.id = :instructorId)))) " +
            "ORDER BY q.createdAt DESC, q.id DESC")
    List<Question> findManualGradingQuestionsByInstructorId(@Param("instructorId") Integer instructorId);

    @Query("SELECT q FROM Question q WHERE q.requiresManualGrading = true AND q.course.id = :courseId ORDER BY q.createdAt DESC, q.id DESC")
    List<Question> findManualGradingQuestionsByCourseId(@Param("courseId") Integer courseId);

    @Query("SELECT q FROM Question q WHERE q.id = :questionId OR q.parentQuestion.id = :questionId ORDER BY q.version ASC, q.id ASC")
    List<Question> findVersionLineage(@Param("questionId") Integer questionId);

    @Query("SELECT q FROM Question q WHERE q.isBankQuestion = true AND (" +
            "q.createdBy.id = :instructorId " +
            "OR (q.course IS NOT NULL AND (q.course.instructor.id = :instructorId " +
            "OR EXISTS (SELECT ci FROM CourseInstructor ci WHERE ci.course.id = q.course.id AND ci.instructor.id = :instructorId)))) " +
            "ORDER BY q.createdAt DESC, q.id DESC")
    List<Question> findBankQuestionsAccessibleByInstructorId(@Param("instructorId") Integer instructorId);

    @Query("SELECT q FROM Question q WHERE (q.id = :rootId OR q.parentQuestion.id = :rootId) AND q.isLatest = true")
    Optional<Question> findLatestVersionByRootId(@Param("rootId") Integer rootId);

    @Query("SELECT q FROM Question q WHERE q.course.id = :courseId " +
            "AND (:topic IS NULL OR LOWER(q.topic) = LOWER(:topic)) " +
            "AND (:difficulty IS NULL OR LOWER(q.difficulty) = LOWER(:difficulty)) " +
            "ORDER BY q.id ASC")
    List<Question> findByCourseIdAndTopicAndDifficulty(@Param("courseId") Integer courseId,
                                                      @Param("topic") String topic,
                                                      @Param("difficulty") String difficulty);
}

