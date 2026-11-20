package com.olp.repository;

import com.olp.entity.StudentNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentNotificationRepository extends JpaRepository<StudentNotification, Integer> {

    @Query("SELECT n FROM StudentNotification n LEFT JOIN FETCH n.course WHERE n.student.id = :studentId ORDER BY n.createdAt DESC")
    List<StudentNotification> findByStudentIdOrderByCreatedAtDesc(@Param("studentId") Integer studentId);

    @Query("SELECT n FROM StudentNotification n LEFT JOIN FETCH n.course WHERE n.student.id = :studentId AND n.isRead = false ORDER BY n.createdAt DESC")
    List<StudentNotification> findUnreadByStudentId(@Param("studentId") Integer studentId);

    @Query("SELECT COUNT(n) FROM StudentNotification n WHERE n.student.id = :studentId AND n.isRead = false")
    long countUnreadByStudentId(@Param("studentId") Integer studentId);

    @Modifying
    @Query("UPDATE StudentNotification n SET n.isRead = true WHERE n.student.id = :studentId AND n.isRead = false")
    int markAllAsRead(@Param("studentId") Integer studentId);
}
