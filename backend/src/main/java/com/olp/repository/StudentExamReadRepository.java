package com.olp.repository;

import com.olp.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

/** Independent read interface; existing instructor repository is unchanged. */
public interface StudentExamReadRepository extends JpaRepository<Exam, Integer> {
    @Query("SELECT DISTINCT ex FROM Exam ex JOIN Enrollment en ON en.course.id = ex.course.id " +
           "WHERE en.student.id = :studentId ORDER BY ex.startWindow ASC")
    List<Exam> findEnrolledExams(@Param("studentId") Integer studentId);
}
