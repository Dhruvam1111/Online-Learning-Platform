package com.olp.repository;

import com.olp.entity.LectureBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LectureBookmarkRepository extends JpaRepository<LectureBookmark, Integer> {
    boolean existsByStudentIdAndLectureId(Integer studentId, Integer lectureId);
    Optional<LectureBookmark> findByStudentIdAndLectureId(Integer studentId, Integer lectureId);
    List<LectureBookmark> findByStudentIdOrderByCreatedAtDesc(Integer studentId);
    void deleteByStudentIdAndLectureId(Integer studentId, Integer lectureId);
}