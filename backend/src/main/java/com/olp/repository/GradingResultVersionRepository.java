package com.olp.repository;

import com.olp.entity.GradingResultVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradingResultVersionRepository extends JpaRepository<GradingResultVersion, Integer> {

    @Query("SELECT v FROM GradingResultVersion v " +
           "LEFT JOIN FETCH v.instructor " +
           "WHERE v.answer.id = :answerId " +
           "ORDER BY v.versionNumber ASC")
    List<GradingResultVersion> findByAnswerIdOrderByVersionNumberAsc(@Param("answerId") Integer answerId);

    @Query("SELECT v FROM GradingResultVersion v " +
           "LEFT JOIN FETCH v.instructor " +
           "WHERE v.answer.id = :answerId " +
           "ORDER BY v.versionNumber DESC")
    List<GradingResultVersion> findByAnswerIdOrderByVersionNumberDesc(@Param("answerId") Integer answerId);

    @Query("SELECT v FROM GradingResultVersion v " +
           "LEFT JOIN FETCH v.instructor " +
           "WHERE v.attempt.id = :attemptId " +
           "ORDER BY v.versionNumber ASC, v.createdAt ASC")
    List<GradingResultVersion> findByAttemptIdOrderByVersionNumberAsc(@Param("attemptId") Integer attemptId);

    @Query("SELECT v FROM GradingResultVersion v " +
           "LEFT JOIN FETCH v.instructor " +
           "WHERE v.attempt.id = :attemptId AND v.answer IS NULL " +
           "ORDER BY v.versionNumber ASC")
    List<GradingResultVersion> findAttemptLevelVersionsByAttemptId(@Param("attemptId") Integer attemptId);

    @Query("SELECT COUNT(v) FROM GradingResultVersion v WHERE v.answer.id = :answerId")
    int countByAnswerId(@Param("answerId") Integer answerId);

    @Query("SELECT COUNT(v) FROM GradingResultVersion v WHERE v.attempt.id = :attemptId AND v.answer IS NULL")
    int countByAttemptIdAndAnswerIsNull(@Param("attemptId") Integer attemptId);

    @Query("SELECT v FROM GradingResultVersion v " +
           "WHERE v.answer.id = :answerId " +
           "ORDER BY v.versionNumber DESC")
    List<GradingResultVersion> findTopByAnswerId(@Param("answerId") Integer answerId);

    default Optional<GradingResultVersion> findLatestByAnswerId(Integer answerId) {
        List<GradingResultVersion> list = findTopByAnswerId(answerId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
