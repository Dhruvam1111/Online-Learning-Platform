package com.olp.controller;

import com.olp.dto.MyCourseResponse;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.service.MyCoursesService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentCourseControllerTest {

    @Mock
    private MyCoursesService myCoursesService;

    @InjectMocks
    private StudentCourseController studentCourseController;

    private User student;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        student = new User("Sam Student", "sam@example.com", "hash", "student");
        student.setId(7);
        authentication = new UsernamePasswordAuthenticationToken(student, null);
    }

    @Test
    void testGetMyCourses_ReturnsEnrolledCourses() {
        MyCourseResponse course = new MyCourseResponse(3, "Spring Boot", "Dr. Chen", LocalDateTime.now());
        when(myCoursesService.resolveUser(authentication)).thenReturn(student);
        when(myCoursesService.getMyCourses(student)).thenReturn(List.of(course));

        ResponseEntity<List<MyCourseResponse>> response = studentCourseController.getMyCourses(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("Spring Boot", response.getBody().get(0).title());
        assertEquals("Dr. Chen", response.getBody().get(0).instructorName());
    }

    @Test
    void testGetMyCourses_NoEnrollmentsReturnsEmptyList() {
        when(myCoursesService.resolveUser(authentication)).thenReturn(student);
        when(myCoursesService.getMyCourses(student)).thenReturn(List.of());

        ResponseEntity<List<MyCourseResponse>> response = studentCourseController.getMyCourses(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void testGetMyCourses_UnresolvableUserPropagates() {
        when(myCoursesService.resolveUser(authentication)).thenThrow(new InvalidCredentialsException("User not found"));

        assertThrows(InvalidCredentialsException.class, () -> studentCourseController.getMyCourses(authentication));
        verify(myCoursesService, never()).getMyCourses(any());
    }
}
