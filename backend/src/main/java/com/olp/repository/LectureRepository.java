package com.olp.repository;

import com.olp.entity.Course;
import com.olp.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Integer> {

    List<Lecture> findByCourseIdOrderByOrderIndexAsc(Integer courseId);

    List<Lecture> findByCourseOrderByOrderIndexAsc(Course course);

    Optional<Lecture> findByIdAndCourseId(Integer id, Integer courseId);

    int countByCourseId(Integer courseId);

    boolean existsByIdAndCourseId(Integer id, Integer courseId);
}
