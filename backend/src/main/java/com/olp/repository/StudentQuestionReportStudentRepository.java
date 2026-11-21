package com.olp.repository;

import com.olp.entity.StudentQuestionReport;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

/** Narrow, student-owned read queries without changing instructor repositories. */
public interface StudentQuestionReportStudentRepository extends Repository<StudentQuestionReport, Integer> {

    @Query("SELECT r FROM StudentQuestionReport r JOIN FETCH r.reportedQuestion rq " +
           "WHERE r.student.id = :studentId ORDER BY r.reportedAt DESC")
    List<StudentQuestionReport> findMine(@Param("studentId") Integer studentId);

    @Query("SELECT r FROM StudentQuestionReport r JOIN FETCH r.reportedQuestion rq " +
           "WHERE r.id = :id AND r.student.id = :studentId")
    Optional<StudentQuestionReport> findMineById(@Param("studentId") Integer studentId,
                                                 @Param("id") Integer id);
}
