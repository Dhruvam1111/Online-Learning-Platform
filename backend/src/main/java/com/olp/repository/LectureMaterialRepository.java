package com.olp.repository;

import com.olp.entity.LectureMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LectureMaterialRepository extends JpaRepository<LectureMaterial, Integer> {

    List<LectureMaterial> findByLectureId(Integer lectureId);

    List<LectureMaterial> findByLessonId(Integer lessonId);

    List<LectureMaterial> findByModuleId(Integer moduleId);

    List<LectureMaterial> findByUploaderId(Integer uploaderId);

    List<LectureMaterial> findByLessonIdAndDeletedAtIsNullOrderByOrderIndexAscIdAsc(Integer lessonId);

    List<LectureMaterial> findByModuleIdAndDeletedAtIsNullOrderByOrderIndexAscIdAsc(Integer moduleId);

    List<LectureMaterial> findByLectureIdAndDeletedAtIsNullOrderByOrderIndexAscIdAsc(Integer lectureId);

    Optional<LectureMaterial> findByIdAndDeletedAtIsNull(Integer id);

    @Query("SELECT COALESCE(MAX(m.orderIndex), 0) FROM LectureMaterial m WHERE m.lessonId = :lessonId AND m.deletedAt IS NULL")
    Integer findMaxOrderIndexByLessonId(@Param("lessonId") Integer lessonId);
}
