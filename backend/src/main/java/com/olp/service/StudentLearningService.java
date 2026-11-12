package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.LectureBookmark;
import com.olp.entity.LectureMaterial;
import com.olp.entity.LectureProgress;
import com.olp.repository.EnrollmentCheckRepository;
import com.olp.repository.LectureBookmarkRepository;
import com.olp.repository.LectureMaterialRepository;
import com.olp.repository.LectureProgressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StudentLearningService {

    private final LectureProgressRepository lectureProgressRepository;
    private final LectureMaterialRepository lectureMaterialRepository;
    private final LectureBookmarkRepository lectureBookmarkRepository;
    private final EnrollmentCheckRepository enrollmentCheckRepository;
    private final JdbcTemplate jdbcTemplate;

    public StudentLearningService(
            LectureProgressRepository lectureProgressRepository,
            LectureMaterialRepository lectureMaterialRepository,
            LectureBookmarkRepository lectureBookmarkRepository,
            EnrollmentCheckRepository enrollmentCheckRepository,
            JdbcTemplate jdbcTemplate) {
        this.lectureProgressRepository = lectureProgressRepository;
        this.lectureMaterialRepository = lectureMaterialRepository;
        this.lectureBookmarkRepository = lectureBookmarkRepository;
        this.enrollmentCheckRepository = enrollmentCheckRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    private Integer getCourseIdForLecture(Integer lectureId) {
        String sql = "SELECT course_id FROM lectures WHERE id = ?";
        List<Integer> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getInt("course_id"), lectureId);
        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lecture not found");
        }
        return list.get(0);
    }

    private void verifyEnrollment(Integer studentId, Integer courseId) {
        if (!enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must be enrolled in this course to access learning content.");
        }
    }

    // FR-S13: Update playback position
    @Transactional
    public Map<String, Object> updatePlaybackPosition(Integer studentId, Integer lectureId, Integer seconds) {
        Integer courseId = getCourseIdForLecture(lectureId);
        verifyEnrollment(studentId, courseId);

        LectureProgress progress = lectureProgressRepository
                .findByStudentIdAndLectureId(studentId, lectureId)
                .orElseGet(() -> new LectureProgress(studentId, lectureId));

        progress.setPlaybackPositionSeconds(seconds);
        lectureProgressRepository.save(progress);

        return Map.of("lectureId", lectureId, "playbackPositionSeconds", seconds, "message", "Playback position updated");
    }

    // FR-S13: Get playback position
    @Transactional(readOnly = true)
    public Map<String, Object> getPlaybackPosition(Integer studentId, Integer lectureId) {
        Integer courseId = getCourseIdForLecture(lectureId);
        verifyEnrollment(studentId, courseId);

        int position = lectureProgressRepository.findByStudentIdAndLectureId(studentId, lectureId)
                .map(LectureProgress::getPlaybackPositionSeconds)
                .orElse(0);

        return Map.of("lectureId", lectureId, "playbackPositionSeconds", position);
    }

    // FR-S14: Access learning materials
    @Transactional(readOnly = true)
    public List<LectureMaterialResponse> getLectureMaterials(Integer studentId, Integer lectureId) {
        Integer courseId = getCourseIdForLecture(lectureId);
        verifyEnrollment(studentId, courseId);

        return lectureMaterialRepository.findByLectureId(lectureId).stream()
                .map(m -> new LectureMaterialResponse(
                        Long.valueOf(m.getId()),
                        Long.valueOf(m.getLectureId()),
                        m.getTitle(),
                        m.getFileUrl(),
                        m.getFileType(),
                        m.getIsDownloadable()
                ))
                .collect(Collectors.toList());
    }

    // FR-S17: Toggle lecture bookmark
    @Transactional
    public Map<String, Object> toggleLectureBookmark(Integer studentId, Integer lectureId) {
        Integer courseId = getCourseIdForLecture(lectureId);
        verifyEnrollment(studentId, courseId);

        if (lectureBookmarkRepository.existsByStudentIdAndLectureId(studentId, lectureId)) {
            lectureBookmarkRepository.deleteByStudentIdAndLectureId(studentId, lectureId);
            return Map.of("lectureId", lectureId, "isBookmarked", false, "message", "Lecture bookmark removed");
        } else {
            lectureBookmarkRepository.save(new LectureBookmark(studentId, lectureId));
            return Map.of("lectureId", lectureId, "isBookmarked", true, "message", "Lecture bookmarked successfully");
        }
    }

    // FR-S17: Get student's bookmarked lectures
    @Transactional(readOnly = true)
    public List<LectureBookmarkResponse> getBookmarkedLectures(Integer studentId) {
        String sql = "SELECT b.id AS bookmark_id, l.id AS lecture_id, l.title AS lecture_title, " +
                     "c.id AS course_id, c.title AS course_title, b.created_at " +
                     "FROM lecture_bookmarks b " +
                     "JOIN lectures l ON b.lecture_id = l.id " +
                     "JOIN courses c ON l.course_id = c.id " +
                     "WHERE b.student_id = ? " +
                     "ORDER BY b.created_at DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> new LectureBookmarkResponse(
                rs.getLong("bookmark_id"),
                rs.getLong("lecture_id"),
                rs.getString("lecture_title"),
                rs.getLong("course_id"),
                rs.getString("course_title"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), studentId);
    }

    // FR-S18-Content: Search topics or keywords within enrolled course lectures
    @Transactional(readOnly = true)
    public List<LectureSearchResult> searchCourseLectures(Integer studentId, Integer courseId, String keyword) {
        verifyEnrollment(studentId, courseId);

        String sql = "SELECT l.id AS lecture_id, l.title AS lecture_title, l.video_url, l.order_index, " +
                     "c.id AS course_id, c.title AS course_title " +
                     "FROM lectures l " +
                     "JOIN courses c ON l.course_id = c.id " +
                     "WHERE l.course_id = ? AND LOWER(l.title) LIKE LOWER(?) " +
                     "ORDER BY l.order_index ASC";

        String searchPattern = "%" + keyword.trim() + "%";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new LectureSearchResult(
                rs.getLong("lecture_id"),
                rs.getString("lecture_title"),
                rs.getString("video_url"),
                rs.getInt("order_index"),
                rs.getLong("course_id"),
                rs.getString("course_title")
        ), courseId, searchPattern);
    }
}