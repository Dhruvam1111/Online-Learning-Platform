package com.olp.service;

import com.olp.dto.BookmarkedCourseResponse;
import com.olp.entity.Bookmark;
import com.olp.repository.BookmarkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    private static final Integer STUDENT = 5;
    private static final Integer COURSE = 1;

    @Mock
    private BookmarkRepository bookmarkRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private BookmarkService service;

    // ===== addBookmark =====

    @Test
    void testAddBookmark_NotYetBookmarked_SavesNewBookmark() {
        when(bookmarkRepository.existsByStudentIdAndCourseId(STUDENT, COURSE)).thenReturn(false);

        service.addBookmark(STUDENT, COURSE);

        ArgumentCaptor<Bookmark> saved = ArgumentCaptor.forClass(Bookmark.class);
        verify(bookmarkRepository, times(1)).save(saved.capture());
        assertEquals(STUDENT, saved.getValue().getStudentId());
        assertEquals(COURSE, saved.getValue().getCourseId());
        assertNotNull(saved.getValue().getCreatedAt());
    }

    @Test
    void testAddBookmark_AlreadyBookmarked_IsIdempotent() {
        when(bookmarkRepository.existsByStudentIdAndCourseId(STUDENT, COURSE)).thenReturn(true);

        service.addBookmark(STUDENT, COURSE);

        verify(bookmarkRepository, never()).save(any(Bookmark.class));
    }

    // ===== removeBookmark / isBookmarked =====

    @Test
    void testRemoveBookmark_DeletesByStudentAndCourse() {
        service.removeBookmark(STUDENT, COURSE);

        verify(bookmarkRepository, times(1)).deleteByStudentIdAndCourseId(STUDENT, COURSE);
    }

    @Test
    void testIsBookmarked_ReturnsTrueWhenBookmarkExists() {
        when(bookmarkRepository.existsByStudentIdAndCourseId(STUDENT, COURSE)).thenReturn(true);

        assertTrue(service.isBookmarked(STUDENT, COURSE));
    }

    @Test
    void testIsBookmarked_ReturnsFalseWhenAbsent() {
        when(bookmarkRepository.existsByStudentIdAndCourseId(STUDENT, COURSE)).thenReturn(false);

        assertFalse(service.isBookmarked(STUDENT, COURSE));
    }

    // ===== getBookmarkedCourses =====

    @Test
    void testGetBookmarkedCourses_RowMapperMapsAllColumns() throws Exception {
        LocalDateTime bookmarkedAt = LocalDateTime.of(2026, 1, 15, 10, 30);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp("bookmarked_at")).thenReturn(Timestamp.valueOf(bookmarkedAt));
        when(rs.getInt("course_id")).thenReturn(1);
        when(rs.getString("title")).thenReturn("Java");
        when(rs.getString("description")).thenReturn("Intro to Java");
        when(rs.getString("instructor_name")).thenReturn("Ivy");
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<BookmarkedCourseResponse>>any(), eq(STUDENT)))
                .thenAnswer(invocation -> {
                    RowMapper<BookmarkedCourseResponse> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(rs, 0));
                });

        List<BookmarkedCourseResponse> result = service.getBookmarkedCourses(STUDENT);

        assertEquals(1, result.size());
        BookmarkedCourseResponse bookmark = result.get(0);
        assertEquals(1, bookmark.getCourseId());
        assertEquals("Java", bookmark.getTitle());
        assertEquals("Intro to Java", bookmark.getDescription());
        assertEquals("Ivy", bookmark.getInstructorName());
        assertEquals(bookmarkedAt, bookmark.getBookmarkedAt());
    }

    @Test
    void testGetBookmarkedCourses_NullTimestampMapsToNullDate() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp("bookmarked_at")).thenReturn(null);
        when(rs.getInt("course_id")).thenReturn(2);
        when(rs.getString("title")).thenReturn("SQL");
        when(rs.getString("description")).thenReturn(null);
        when(rs.getString("instructor_name")).thenReturn("Ivy");
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<BookmarkedCourseResponse>>any(), eq(STUDENT)))
                .thenAnswer(invocation -> {
                    RowMapper<BookmarkedCourseResponse> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(rs, 0));
                });

        BookmarkedCourseResponse bookmark = service.getBookmarkedCourses(STUDENT).get(0);

        assertNull(bookmark.getBookmarkedAt());
        assertNull(bookmark.getDescription());
        assertEquals(2, bookmark.getCourseId());
    }

    @Test
    void testGetBookmarkedCourses_NoBookmarks_ReturnsEmptyList() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<BookmarkedCourseResponse>>any(), eq(STUDENT)))
                .thenReturn(List.of());

        assertTrue(service.getBookmarkedCourses(STUDENT).isEmpty());
    }

    @Test
    void testGetBookmarkedCourses_QueryIsScopedToStudentAndNewestFirst() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<BookmarkedCourseResponse>>any(), eq(STUDENT)))
                .thenReturn(List.of());

        service.getBookmarkedCourses(STUDENT);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(), ArgumentMatchers.<RowMapper<BookmarkedCourseResponse>>any(), eq(STUDENT));
        assertTrue(sql.getValue().contains("b.student_id = ?"));
        assertTrue(sql.getValue().contains("ORDER BY b.created_at DESC"));
    }
}
