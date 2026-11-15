package com.olp.repository;

import com.olp.entity.StudentChallengeProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentChallengeProgressRepository extends JpaRepository<StudentChallengeProgress, Integer> {

    @Query("SELECT p FROM StudentChallengeProgress p JOIN FETCH p.challenge WHERE p.student.id = :studentId")
    List<StudentChallengeProgress> findByStudentId(@Param("studentId") Integer studentId);

    @Query("SELECT p FROM StudentChallengeProgress p WHERE p.student.id = :studentId AND p.challenge.id = :challengeId")
    Optional<StudentChallengeProgress> findByStudentIdAndChallengeId(@Param("studentId") Integer studentId,
                                                                    @Param("challengeId") Integer challengeId);
}
