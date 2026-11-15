package com.olp.repository;

import com.olp.entity.StudentBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentBadgeRepository extends JpaRepository<StudentBadge, Integer> {

    @Query("SELECT sb FROM StudentBadge sb JOIN FETCH sb.badge WHERE sb.student.id = :studentId ORDER BY sb.awardedAt DESC")
    List<StudentBadge> findByStudentId(@Param("studentId") Integer studentId);

    @Query("SELECT COUNT(sb) > 0 FROM StudentBadge sb WHERE sb.student.id = :studentId AND sb.badge.code = :badgeCode")
    boolean existsByStudentIdAndBadgeCode(@Param("studentId") Integer studentId, @Param("badgeCode") String badgeCode);
}
