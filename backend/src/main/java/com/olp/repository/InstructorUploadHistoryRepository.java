package com.olp.repository;

import com.olp.entity.InstructorUploadHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstructorUploadHistoryRepository extends JpaRepository<InstructorUploadHistory, Integer> {

    @Query("SELECT h FROM InstructorUploadHistory h WHERE h.instructor.id = :instructorId ORDER BY h.createdAt DESC")
    List<InstructorUploadHistory> findByInstructorIdOrderByCreatedAtDesc(@Param("instructorId") Integer instructorId);
}

