package com.olp.repository;

import com.olp.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findByCourseIdOrderByCreatedAtDesc(Integer courseId);

    boolean existsByCourseIdAndStudentId(Integer courseId, Integer studentId);

    Optional<Review> findByCourseIdAndStudentId(Integer courseId, Integer studentId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.courseId = :courseId")
    Double findAverageRatingByCourseId(@Param("courseId") Integer courseId);
}