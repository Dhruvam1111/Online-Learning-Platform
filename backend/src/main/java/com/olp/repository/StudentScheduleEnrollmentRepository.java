package com.olp.repository;

import com.olp.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Targeted lookup to avoid scanning every student's enrollment. */
public interface StudentScheduleEnrollmentRepository extends JpaRepository<Enrollment, Integer> {
    List<Enrollment> findByStudentId(Integer studentId);
}
