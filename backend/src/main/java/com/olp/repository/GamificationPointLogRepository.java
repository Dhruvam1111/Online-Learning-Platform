package com.olp.repository;

import com.olp.entity.GamificationPointLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GamificationPointLogRepository extends JpaRepository<GamificationPointLog, Integer> {

    @Query("SELECT l FROM GamificationPointLog l WHERE l.student.id = :studentId ORDER BY l.createdAt DESC")
    List<GamificationPointLog> findByStudentId(@Param("studentId") Integer studentId);

    @Query("SELECT COUNT(l) FROM GamificationPointLog l WHERE l.student.id = :studentId AND l.activityType = :activityType AND l.referenceId = :refId")
    long countByStudentAndActivityAndRef(@Param("studentId") Integer studentId,
                                        @Param("activityType") String activityType,
                                        @Param("refId") Integer refId);
}
