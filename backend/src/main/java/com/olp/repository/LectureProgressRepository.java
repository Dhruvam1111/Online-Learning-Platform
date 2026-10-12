package com.olp.repository;

import com.olp.entity.LectureProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LectureProgressRepository extends JpaRepository<LectureProgress, Integer> {

    boolean existsByStudentIdAndLectureId(Integer studentId, Integer lectureId);

    Optional<LectureProgress> findByStudentIdAndLectureId(Integer studentId, Integer lectureId);

    void deleteByStudentIdAndLectureId(Integer studentId, Integer lectureId);
}