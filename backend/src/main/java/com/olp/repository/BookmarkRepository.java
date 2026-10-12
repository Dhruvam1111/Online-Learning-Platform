package com.olp.repository;

import com.olp.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Integer> {

    boolean existsByStudentIdAndCourseId(Integer studentId, Integer courseId);

    Optional<Bookmark> findByStudentIdAndCourseId(Integer studentId, Integer courseId);

    void deleteByStudentIdAndCourseId(Integer studentId, Integer courseId);
}