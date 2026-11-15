package com.olp.repository;

import com.olp.entity.StudentQuestionReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentQuestionReportRepository extends JpaRepository<StudentQuestionReport, Integer> {

    @Query("SELECT sqr FROM StudentQuestionReport sqr WHERE sqr.reportedQuestion.id = :reportedQuestionId ORDER BY sqr.reportedAt DESC")
    List<StudentQuestionReport> findByReportedQuestionId(@Param("reportedQuestionId") Integer reportedQuestionId);

    @Query("SELECT sqr FROM StudentQuestionReport sqr WHERE sqr.reportedQuestion.id = :reportedQuestionId AND sqr.student.id = :studentId")
    Optional<StudentQuestionReport> findByReportedQuestionIdAndStudentId(
            @Param("reportedQuestionId") Integer reportedQuestionId,
            @Param("studentId") Integer studentId);

    @Query("SELECT COUNT(sqr) FROM StudentQuestionReport sqr WHERE sqr.reportedQuestion.id = :reportedQuestionId")
    long countByReportedQuestionId(@Param("reportedQuestionId") Integer reportedQuestionId);
}
