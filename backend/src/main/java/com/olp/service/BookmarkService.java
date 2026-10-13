package com.olp.service;

import com.olp.dto.BookmarkedCourseResponse;
import com.olp.entity.Bookmark;
import com.olp.repository.BookmarkRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Service
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final JdbcTemplate jdbcTemplate;

    public BookmarkService(BookmarkRepository bookmarkRepository, JdbcTemplate jdbcTemplate) {
        this.bookmarkRepository = bookmarkRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Bookmark a course for the student.
     * Idempotent: If already bookmarked, does not create duplicates.
     */
    @Transactional
    public void addBookmark(Integer studentId, Integer courseId) {
        if (!bookmarkRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            Bookmark bookmark = new Bookmark(studentId, courseId);
            bookmarkRepository.save(bookmark);
        }
    }

    /**
     * Remove a course from the student's bookmarks.
     */
    @Transactional
    public void removeBookmark(Integer studentId, Integer courseId) {
        bookmarkRepository.deleteByStudentIdAndCourseId(studentId, courseId);
    }

    /**
     * Check if a course is bookmarked by a student.
     */
    public boolean isBookmarked(Integer studentId, Integer courseId) {
        return bookmarkRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    /**
     * Returns all courses bookmarked by the student with instructor name.
     */
    public List<BookmarkedCourseResponse> getBookmarkedCourses(Integer studentId) {
        String sql = """
            SELECT c.id AS course_id, c.title, c.description, u.name AS instructor_name, b.created_at AS bookmarked_at
            FROM bookmarks b
            JOIN courses c ON b.course_id = c.id
            JOIN users u ON c.instructor_id = u.id
            WHERE b.student_id = ?
            ORDER BY b.created_at DESC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp ts = rs.getTimestamp("bookmarked_at");
            return new BookmarkedCourseResponse(
                    rs.getInt("course_id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("instructor_name"),
                    ts != null ? ts.toLocalDateTime() : null
            );
        }, studentId);
    }
}