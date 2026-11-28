package com.olp.repository;

import com.olp.entity.StudentGamification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentGamificationRepository extends JpaRepository<StudentGamification, Integer> {

    @Query("SELECT g FROM StudentGamification g JOIN FETCH g.student WHERE g.student.id = :studentId")
    Optional<StudentGamification> findByStudentId(@Param("studentId") Integer studentId);

    @Query("SELECT g FROM StudentGamification g JOIN FETCH g.student ORDER BY g.totalPoints DESC, g.currentStreakDays DESC")
    List<StudentGamification> findLeaderboard(Pageable pageable);

    @Query("SELECT COUNT(g) + 1 FROM StudentGamification g WHERE g.totalPoints > :points")
    int computeRankByPoints(@Param("points") Integer points);
}
