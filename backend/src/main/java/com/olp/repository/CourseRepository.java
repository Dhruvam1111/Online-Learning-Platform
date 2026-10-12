package com.olp.repository;

import com.olp.entity.Course;
import com.olp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {

    List<Course> findByInstructor(User instructor);

    List<Course> findByInstructorId(Integer instructorId);

    List<Course> findByInstructorIdOrderByIdAsc(Integer instructorId);

    Optional<Course> findByIdAndInstructorId(Integer id, Integer instructorId);

    boolean existsByIdAndInstructorId(Integer id, Integer instructorId);
}
