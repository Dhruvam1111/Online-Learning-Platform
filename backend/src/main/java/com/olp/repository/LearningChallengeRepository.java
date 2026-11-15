package com.olp.repository;

import com.olp.entity.LearningChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LearningChallengeRepository extends JpaRepository<LearningChallenge, Integer> {

    @Query("SELECT c FROM LearningChallenge c LEFT JOIN FETCH c.course WHERE c.isActive = true AND c.startDate <= :now AND c.endDate >= :now")
    List<LearningChallenge> findActiveChallenges(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM LearningChallenge c LEFT JOIN FETCH c.course WHERE c.isActive = true AND (c.course.id = :courseId OR c.course IS NULL) AND c.startDate <= :now AND c.endDate >= :now")
    List<LearningChallenge> findActiveChallengesByCourse(@Param("courseId") Integer courseId, @Param("now") LocalDateTime now);
}
