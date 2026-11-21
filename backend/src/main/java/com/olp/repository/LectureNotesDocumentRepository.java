package com.olp.repository;

import com.olp.entity.LectureNotesDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LectureNotesDocumentRepository extends JpaRepository<LectureNotesDocument, Integer> {

    @Query("SELECT d FROM LectureNotesDocument d WHERE d.lessonId = :lessonId ORDER BY d.createdAt DESC")
    List<LectureNotesDocument> findByLessonIdOrderByCreatedAtDesc(@Param("lessonId") Integer lessonId);

    @Query("SELECT d FROM LectureNotesDocument d WHERE d.lessonId = :lessonId AND d.uploaderId = :uploaderId ORDER BY d.createdAt DESC")
    List<LectureNotesDocument> findByLessonIdAndUploaderId(@Param("lessonId") Integer lessonId, @Param("uploaderId") Integer uploaderId);

    @Query("SELECT d FROM LectureNotesDocument d WHERE d.id = :id AND d.lessonId = :lessonId")
    Optional<LectureNotesDocument> findByIdAndLessonId(@Param("id") Integer id, @Param("lessonId") Integer lessonId);
}
