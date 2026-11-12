package com.olp.repository;

import com.olp.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {

    @Query("SELECT q FROM Question q WHERE q.course.id = :courseId ORDER BY q.createdAt DESC, q.id DESC")
    List<Question> findByCourseIdOrderByCreatedAtDesc(@Param("courseId") Integer courseId);

    @Query("SELECT q FROM Question q WHERE q.course.id = :courseId AND q.isBankQuestion = true ORDER BY q.createdAt DESC, q.id DESC")
    List<Question> findBankQuestionsByCourseId(@Param("courseId") Integer courseId);
}

