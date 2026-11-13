package com.olp.repository;

import com.olp.entity.LectureProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LectureProgressRepository extends JpaRepository<LectureProgress, Integer> {

    boolean existsByStudentIdAndLectureId(Integer studentId, Integer lectureId);

    Optional<LectureProgress> findByStudentIdAndLectureId(Integer studentId, Integer lectureId);

    void deleteByStudentIdAndLectureId(Integer studentId, Integer lectureId);

    @Query("SELECT lp.lectureId FROM LectureProgress lp WHERE lp.studentId = :studentId AND lp.lectureId IN :lectureIds")
    List<Integer> findCompletedLectureIdsByStudentId(@Param("studentId") Integer studentId, @Param("lectureIds") Collection<Integer> lectureIds);

    @Query("SELECT COUNT(lp) FROM LectureProgress lp WHERE lp.studentId = :studentId AND lp.lectureId IN :lectureIds")
    long countCompletedByStudentIdAndLectureIds(@Param("studentId") Integer studentId, @Param("lectureIds") Collection<Integer> lectureIds);
}