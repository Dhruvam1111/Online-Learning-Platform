package com.olp.controller;

import com.olp.dto.BookmarkedCourseResponse;
import com.olp.entity.User;
import com.olp.service.BookmarkService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    /**
     * POST /api/courses/{courseId}/bookmark
     * Add course to bookmarks.
     */
    @PostMapping("/api/courses/{courseId}/bookmark")
    @PreAuthorize("hasRole('student')")
    public ResponseEntity<Map<String, Object>> addBookmark(
            @PathVariable Integer courseId,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        bookmarkService.addBookmark(currentUser.getId(), courseId);

        return ResponseEntity.ok(Map.of(
                "message", "Course bookmarked successfully",
                "courseId", courseId,
                "bookmarked", true
        ));
    }

    /**
     * DELETE /api/courses/{courseId}/bookmark
     * Remove bookmark.
     */
    @DeleteMapping("/api/courses/{courseId}/bookmark")
    @PreAuthorize("hasRole('student')")
    public ResponseEntity<Map<String, Object>> removeBookmark(
            @PathVariable Integer courseId,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        bookmarkService.removeBookmark(currentUser.getId(), courseId);

        return ResponseEntity.ok(Map.of(
                "message", "Bookmark removed successfully",
                "courseId", courseId,
                "bookmarked", false
        ));
    }

    /**
     * GET /api/students/bookmarks
     * Return all courses bookmarked by the logged-in student.
     */
    @GetMapping("/api/students/bookmarks")
    @PreAuthorize("hasRole('student')")
    public ResponseEntity<List<BookmarkedCourseResponse>> getMyBookmarks(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        List<BookmarkedCourseResponse> bookmarks = bookmarkService.getBookmarkedCourses(currentUser.getId());
        return ResponseEntity.ok(bookmarks);
    }
}