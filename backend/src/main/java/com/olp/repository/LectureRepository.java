package com.olp.repository;

import com.olp.entity.Course;
import com.olp.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Integer> {

    @Query("SELECT l FROM Lecture l WHERE l.course.id = :courseId ORDER BY l.orderIndex ASC")
    List<Lecture> findByCourseIdOrderByOrderIndexAsc(@Param("courseId") Integer courseId);

    List<Lecture> findByCourseOrderByOrderIndexAsc(Course course);

    @Query("SELECT l FROM Lecture l WHERE l.id = :id AND l.course.id = :courseId")
    Optional<Lecture> findByIdAndCourseId(@Param("id") Integer id, @Param("courseId") Integer courseId);

    @Query("SELECT COUNT(l) FROM Lecture l WHERE l.course.id = :courseId")
    int countByCourseId(@Param("courseId") Integer courseId);

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Lecture l WHERE l.id = :id AND l.course.id = :courseId")
    boolean existsByIdAndCourseId(@Param("id") Integer id, @Param("courseId") Integer courseId);
}
