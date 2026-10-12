package com.olp.repository;

import com.olp.entity.InstructorProfile;
import com.olp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, Integer> {

    Optional<InstructorProfile> findByInstructor(User instructor);

    Optional<InstructorProfile> findByInstructorId(Integer instructorId);

    boolean existsByInstructorId(Integer instructorId);
}
