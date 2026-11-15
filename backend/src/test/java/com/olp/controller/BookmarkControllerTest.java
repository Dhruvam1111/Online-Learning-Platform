package com.olp.controller;

import com.olp.dto.BookmarkedCourseResponse;
import com.olp.entity.User;
import com.olp.service.BookmarkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookmarkControllerTest {

    @Mock
    private BookmarkService bookmarkService;

    @InjectMocks
    private BookmarkController bookmarkController;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        User student = new User("Sam Student", "sam@example.com", "hash", "student");
        student.setId(7);
        authentication = new UsernamePasswordAuthenticationToken(student, null);
    }

    @Test
    void testAddBookmark_ReturnsBookmarkedTrue() {
        ResponseEntity<Map<String, Object>> response = bookmarkController.addBookmark(12, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Course bookmarked successfully", response.getBody().get("message"));
        assertEquals(12, response.getBody().get("courseId"));
        assertEquals(true, response.getBody().get("bookmarked"));
        verify(bookmarkService, times(1)).addBookmark(7, 12);
    }

    @Test
    void testRemoveBookmark_ReturnsBookmarkedFalse() {
        ResponseEntity<Map<String, Object>> response = bookmarkController.removeBookmark(12, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Bookmark removed successfully", response.getBody().get("message"));
        assertEquals(12, response.getBody().get("courseId"));
        assertEquals(false, response.getBody().get("bookmarked"));
        verify(bookmarkService, times(1)).removeBookmark(7, 12);
    }

    @Test
    void testGetMyBookmarks_ReturnsStudentsBookmarks() {
        BookmarkedCourseResponse bookmark =
                new BookmarkedCourseResponse(12, "Spring Boot", "Desc", "Dr. Chen", LocalDateTime.now());
        when(bookmarkService.getBookmarkedCourses(7)).thenReturn(List.of(bookmark));

        ResponseEntity<List<BookmarkedCourseResponse>> response = bookmarkController.getMyBookmarks(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("Spring Boot", response.getBody().get(0).getTitle());
    }

    @Test
    void testGetMyBookmarks_NoBookmarksReturnsEmptyList() {
        when(bookmarkService.getBookmarkedCourses(7)).thenReturn(List.of());

        ResponseEntity<List<BookmarkedCourseResponse>> response = bookmarkController.getMyBookmarks(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void testAddBookmark_NonUserPrincipalThrowsClassCastException() {
        Authentication wrong = new UsernamePasswordAuthenticationToken("someone", null);

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> bookmarkController.addBookmark(12, wrong));
        verifyNoInteractions(bookmarkService);
    }
}
