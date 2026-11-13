package com.olp.repository;

import com.olp.entity.VideoUpload;
import com.olp.entity.VideoUploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoUploadRepository extends JpaRepository<VideoUpload, String> {

    List<VideoUpload> findByLessonIdOrderByCreatedAtDesc(Integer lessonId);

    Optional<VideoUpload> findFirstByLessonIdOrderByCreatedAtDesc(Integer lessonId);

    List<VideoUpload> findByUploaderIdOrderByCreatedAtDesc(Integer uploaderId);

    List<VideoUpload> findByStatus(VideoUploadStatus status);
}

