package com.olp.repository;

import com.olp.entity.StudySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StudyScheduleRepository extends JpaRepository<StudySchedule, Integer> {

    @Query("SELECT s FROM StudySchedule s LEFT JOIN FETCH s.course WHERE s.student.id = :studentId ORDER BY s.scheduledStart ASC")
    List<StudySchedule> findByStudentIdOrderByStart(@Param("studentId") Integer studentId);

    @Query("SELECT s FROM StudySchedule s LEFT JOIN FETCH s.course WHERE s.student.id = :studentId AND s.scheduledStart >= :start AND s.scheduledEnd <= :end ORDER BY s.scheduledStart ASC")
    List<StudySchedule> findByStudentIdAndWindow(@Param("studentId") Integer studentId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    @Query("SELECT s FROM StudySchedule s WHERE s.student.id = :studentId AND s.course.id = :courseId AND s.itemType = :itemType AND s.referenceId = :refId")
    List<StudySchedule> findExistingSchedules(@Param("studentId") Integer studentId,
                                             @Param("courseId") Integer courseId,
                                             @Param("itemType") String itemType,
                                             @Param("refId") Integer refId);
}
