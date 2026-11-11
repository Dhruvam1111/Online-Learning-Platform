package com.olp.repository;

import com.olp.entity.InstructorProfile;
import com.olp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, Integer> {

    Optional<InstructorProfile> findByInstructor(User instructor);

    Optional<InstructorProfile> findByInstructorId(Integer instructorId);

    @Query("SELECT ip FROM InstructorProfile ip JOIN FETCH ip.instructor WHERE ip.instructor.id IN :instructorIds")
    List<InstructorProfile> findByInstructorIdIn(@Param("instructorIds") List<Integer> instructorIds);

    boolean existsByInstructorId(Integer instructorId);
}
