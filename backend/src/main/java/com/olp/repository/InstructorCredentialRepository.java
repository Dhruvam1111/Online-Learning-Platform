package com.olp.repository;

import com.olp.entity.InstructorCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstructorCredentialRepository extends JpaRepository<InstructorCredential, Integer> {

    @Query("SELECT c FROM InstructorCredential c WHERE c.instructor.id = :instructorId AND c.status = :status ORDER BY c.uploadedAt DESC")
    List<InstructorCredential> findByInstructorIdAndStatus(@Param("instructorId") Integer instructorId,
                                                          @Param("status") String status);

    @Query("SELECT c FROM InstructorCredential c WHERE c.instructor.id = :instructorId ORDER BY c.uploadedAt DESC")
    List<InstructorCredential> findAllByInstructorId(@Param("instructorId") Integer instructorId);

    @Query("SELECT c FROM InstructorCredential c WHERE c.id = :id AND c.instructor.id = :instructorId")
    Optional<InstructorCredential> findByIdAndInstructorId(@Param("id") Integer id,
                                                           @Param("instructorId") Integer instructorId);
}

