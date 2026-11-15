package com.olp.service;

import com.olp.dto.MyCourseResponse;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.repository.EnrollmentRepository;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyCoursesServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private MyCoursesService service;

    private User student;

    @BeforeEach
    void setUp() {
        student = new User("Sam", "sam@olp.com", "hash", "student", "active");
        student.setId(5);
    }

    // ===== resolveUser =====

    @Test
    void testResolveUser_NullAuthentication_Throws() {
        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(null));
    }

    @Test
    void testResolveUser_NullPrincipal_Throws() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(null, null);

        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(auth));
    }

    @Test
    void testResolveUser_UserPrincipal_ReturnsSameInstanceWithoutRepositoryCall() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(student, null);

        assertSame(student, service.resolveUser(auth));
        verifyNoInteractions(userRepository);
    }

    @Test
    void testResolveUser_NonUserPrincipal_LooksUpByEmail() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("sam@olp.com", null);
        when(userRepository.findByEmail("sam@olp.com")).thenReturn(Optional.of(student));

        assertSame(student, service.resolveUser(auth));
    }

    @Test
    void testResolveUser_EmailNotFound_Throws() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("ghost@olp.com", null);
        when(userRepository.findByEmail("ghost@olp.com")).thenReturn(Optional.empty());

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(auth));
        assertTrue(ex.getMessage().contains("User not found"));
    }

    // ===== getMyCourses =====

    @Test
    void testGetMyCourses_NullUser_Throws() {
        assertThrows(InvalidCredentialsException.class, () -> service.getMyCourses(null));
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void testGetMyCourses_Instructor_ThrowsAccessDenied() {
        User instructor = new User("Ivy", "ivy@olp.com", "hash", "instructor", "active");
        instructor.setId(10);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> service.getMyCourses(instructor));
        assertTrue(ex.getMessage().contains("only students"));
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void testGetMyCourses_Student_ReturnsEnrollmentRepositoryResult() {
        List<MyCourseResponse> courses = List.of(
                new MyCourseResponse(1, "Java", "Ivy", LocalDateTime.now()),
                new MyCourseResponse(2, "SQL", "Ivy", LocalDateTime.now().minusDays(1)));
        when(enrollmentRepository.findMyCourses(5)).thenReturn(courses);

        List<MyCourseResponse> result = service.getMyCourses(student);

        assertEquals(2, result.size());
        assertSame(courses, result);
        verifyNoInteractions(userRepository);
    }

    @Test
    void testGetMyCourses_NoEnrollments_ReturnsEmptyList() {
        when(enrollmentRepository.findMyCourses(5)).thenReturn(List.of());

        assertTrue(service.getMyCourses(student).isEmpty());
    }

    @Test
    void testGetMyCourses_RoleCheckIsCaseInsensitive() {
        student.setRole("STUDENT");
        when(enrollmentRepository.findMyCourses(5)).thenReturn(List.of());

        assertNotNull(service.getMyCourses(student));
        verify(enrollmentRepository).findMyCourses(5);
    }
}
