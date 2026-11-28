package com.olp.repository;

import com.olp.entity.StudentStudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentStudySessionRepository extends JpaRepository<StudentStudySession, Integer> {
    List<StudentStudySession> findByStudentIdAndStartsAtUtcGreaterThanEqualAndStartsAtUtcLessThanOrderByStartsAtUtcAsc(
            Integer studentId, LocalDateTime fromInclusive, LocalDateTime toExclusive);
    Optional<StudentStudySession> findByIdAndStudentId(Integer id, Integer studentId);
}
