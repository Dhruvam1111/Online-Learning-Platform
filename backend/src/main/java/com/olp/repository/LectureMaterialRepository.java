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

    @Query("SELECT m FROM LectureMaterial m WHERE m.isReleased = false AND m.releaseAt IS NOT NULL AND m.releaseAt <= :now AND m.deletedAt IS NULL")
    List<LectureMaterial> findDueUnreleasedMaterials(@Param("now") java.time.LocalDateTime now);

    @Query("SELECT m FROM LectureMaterial m WHERE m.lessonId = :lessonId AND m.deletedAt IS NULL AND (m.isReleased = true OR m.releaseAt IS NULL OR m.releaseAt <= :now) ORDER BY m.orderIndex ASC, m.id ASC")
    List<LectureMaterial> findAvailableByLessonId(@Param("lessonId") Integer lessonId, @Param("now") java.time.LocalDateTime now);

    @Query("SELECT m FROM LectureMaterial m WHERE m.moduleId = :moduleId AND m.deletedAt IS NULL AND (m.isReleased = true OR m.releaseAt IS NULL OR m.releaseAt <= :now) ORDER BY m.orderIndex ASC, m.id ASC")
    List<LectureMaterial> findAvailableByModuleId(@Param("moduleId") Integer moduleId, @Param("now") java.time.LocalDateTime now);

    @Query("SELECT m FROM LectureMaterial m WHERE m.lectureId = :lectureId AND m.deletedAt IS NULL AND (m.isReleased = true OR m.releaseAt IS NULL OR m.releaseAt <= :now) ORDER BY m.orderIndex ASC, m.id ASC")
    List<LectureMaterial> findAvailableByLectureId(@Param("lectureId") Integer lectureId, @Param("now") java.time.LocalDateTime now);
}
