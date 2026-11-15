package com.olp.repository;

import com.olp.entity.CourseAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseAuditRepository extends JpaRepository<CourseAudit, Integer> {

    List<CourseAudit> findByCourseIdOrderByCreatedAtDesc(Integer courseId);

    List<CourseAudit> findByCourseId(Integer courseId);

    long countByCourseId(Integer courseId);
}

