package com.olp.repository;

import com.olp.entity.ContentDeclaration;
import com.olp.entity.TakedownStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentDeclarationRepository extends JpaRepository<ContentDeclaration, Integer> {

    Optional<ContentDeclaration> findFirstByLessonIdOrderByDeclaredAtDesc(Integer lessonId);

    Optional<ContentDeclaration> findFirstByCourseIdAndLessonIsNullOrderByDeclaredAtDesc(Integer courseId);

    List<ContentDeclaration> findByCourseIdOrderByDeclaredAtDesc(Integer courseId);

    List<ContentDeclaration> findByInstructorIdOrderByDeclaredAtDesc(Integer instructorId);

    boolean existsByLessonIdAndTakedownStatusNot(Integer lessonId, TakedownStatus status);

    boolean existsByCourseIdAndLessonIsNullAndTakedownStatusNot(Integer courseId, TakedownStatus status);

    @Query("SELECT cd FROM ContentDeclaration cd " +
           "WHERE cd.course.id = :courseId " +
           "AND cd.takedownStatus = :status " +
           "ORDER BY cd.declaredAt DESC")
    List<ContentDeclaration> findByCourseIdAndTakedownStatus(
            @Param("courseId") Integer courseId,
            @Param("status") TakedownStatus status);
}

