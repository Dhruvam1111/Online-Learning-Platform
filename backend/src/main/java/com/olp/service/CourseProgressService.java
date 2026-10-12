package com.olp.service;

import com.olp.dto.CourseProgressResponse;
import com.olp.entity.LectureProgress;
import com.olp.repository.EnrollmentCheckRepository;
import com.olp.repository.LectureProgressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CourseProgressService {

    private final LectureProgressRepository progressRepository;
    private final EnrollmentCheckRepository enrollmentCheckRepository;
    private final JdbcTemplate jdbcTemplate;

    public CourseProgressService(LectureProgressRepository progressRepository,
                                 EnrollmentCheckRepository enrollmentCheckRepository,
                                 JdbcTemplate jdbcTemplate) {
        this.progressRepository = progressRepository;
        this.enrollmentCheckRepository = enrollmentCheckRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Mark a lecture as completed for a student.
     * Enforces:
     * 1. Student must be enrolled in the course.
     * 2. Lecture must belong to this course.
     */
    @Transactional
    public void markLectureComplete(Integer studentId, Integer courseId, Integer lectureId) {
        // 1. Verify student enrollment
        if (!enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must be enrolled in this course to track progress.");
        }

        // 2. Verify lecture belongs to this course
        String checkLectureSql = "SELECT COUNT(*) FROM lectures WHERE id = ? AND course_id = ?";
        Integer lectureCount = jdbcTemplate.queryForObject(checkLectureSql, Integer.class, lectureId, courseId);
        if (lectureCount == null || lectureCount == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lecture not found in this course.");
        }

        // 3. Save progress idempotently
        if (!progressRepository.existsByStudentIdAndLectureId(studentId, lectureId)) {
            LectureProgress progress = new LectureProgress(studentId, lectureId);
            progressRepository.save(progress);
        }
    }

    /**
     * Unmark a lecture as completed.
     */
    @Transactional
    public void unmarkLectureComplete(Integer studentId, Integer courseId, Integer lectureId) {
        // Verify student enrollment
        if (!enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must be enrolled in this course to track progress.");
        }

        progressRepository.deleteByStudentIdAndLectureId(studentId, lectureId);
    }

    /**
     * Get overall progress for a course (completed count, total count, percentage, list of completed IDs).
     */
    public CourseProgressResponse getCourseProgress(Integer studentId, Integer courseId) {
        // 1. Total lectures in this course
        String totalSql = "SELECT COUNT(*) FROM lectures WHERE course_id = ?";
        Integer totalCount = jdbcTemplate.queryForObject(totalSql, Integer.class, courseId);
        int total = (totalCount != null) ? totalCount : 0;

        // 2. Completed lectures for this student in this course
        String completedSql = """
            SELECT lp.lecture_id
            FROM lecture_progress lp
            JOIN lectures l ON lp.lecture_id = l.id
            WHERE lp.student_id = ? AND l.course_id = ?
        """;
        List<Integer> completedIds = jdbcTemplate.query(completedSql,
                (rs, rowNum) -> rs.getInt("lecture_id"),
                studentId, courseId);

        int completed = completedIds.size();

        // 3. Calculate percentage rounded to 1 decimal place
        double percentage = (total == 0) ? 0.0 : Math.round(((double) completed / total) * 1000.0) / 10.0;

        return new CourseProgressResponse(courseId, completed, total, percentage, completedIds);
    }
}