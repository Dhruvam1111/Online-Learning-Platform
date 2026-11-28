package com.olp.security;

import com.olp.entity.Course;
import com.olp.entity.CourseStatus;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Test
    void resolveUser_HandlesUserStringUserDetailsAndInvalidPrincipals() {
        User instructor = new User("Dr. A", "a@olp.com", "hash", "instructor", "active");
        instructor.setId(10);

        assertSame(instructor, SecurityUtils.resolveUser(
                new UsernamePasswordAuthenticationToken(instructor, null), userRepository));

        when(userRepository.findByEmail("a@olp.com")).thenReturn(Optional.of(instructor));
        assertSame(instructor, SecurityUtils.resolveUser(
                new UsernamePasswordAuthenticationToken("a@olp.com", null), userRepository));

        UserDetails springUser = org.springframework.security.core.userdetails.User
                .withUsername("a@olp.com").password("hash").authorities(List.of()).build();
        assertSame(instructor, SecurityUtils.resolveUser(
                new UsernamePasswordAuthenticationToken(springUser, null), userRepository));

        assertThrows(InvalidCredentialsException.class, () -> SecurityUtils.resolveUser(null, userRepository));

        Authentication nullPrincipal = mock(Authentication.class);
        when(nullPrincipal.getPrincipal()).thenReturn(null);
        assertThrows(InvalidCredentialsException.class, () -> SecurityUtils.resolveUser(nullPrincipal, userRepository));

        assertThrows(InvalidCredentialsException.class, () -> SecurityUtils.resolveUser(
                new UsernamePasswordAuthenticationToken(999, null), userRepository));

        when(userRepository.findByEmail("missing@olp.com")).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class, () -> SecurityUtils.resolveUser(
                new UsernamePasswordAuthenticationToken("missing@olp.com", null), userRepository));
    }

    @Test
    void resolveUserByName_ResolvesViaAuthNameOrRejectsMissingUser() {
        User student = new User("Bob", "bob@olp.com", "hash", "student", "active");
        when(userRepository.findByEmail("bob@olp.com")).thenReturn(Optional.of(student));

        Authentication auth = new UsernamePasswordAuthenticationToken("bob@olp.com", null);
        assertSame(student, SecurityUtils.resolveUserByName(auth, userRepository));
        assertThrows(InvalidCredentialsException.class, () -> SecurityUtils.resolveUserByName(null, userRepository));
    }

    @Test
    void requireActiveInstructorOrAdmin_EnforcesRoleAndStatus() {
        User activeInstructor = new User("Inst", "inst@olp.com", "hash", "instructor", "active");
        User pendingInstructor = new User("Pend", "pend@olp.com", "hash", "instructor", "pending");
        User pendingAdmin = new User("Admin", "admin@olp.com", "hash", "admin", "pending");
        User student = new User("Stu", "stu@olp.com", "hash", "student", "active");

        assertDoesNotThrow(() -> SecurityUtils.requireActiveInstructorOrAdmin(activeInstructor, "Denied"));
        assertDoesNotThrow(() -> SecurityUtils.requireActiveInstructorOrAdmin(pendingAdmin, "Denied"));
        assertThrows(InvalidCredentialsException.class,
                () -> SecurityUtils.requireActiveInstructorOrAdmin(null, "Denied"));
        assertThrows(AccessDeniedException.class,
                () -> SecurityUtils.requireActiveInstructorOrAdmin(student, "Denied"));
        assertThrows(AccessDeniedException.class,
                () -> SecurityUtils.requireActiveInstructorOrAdmin(pendingInstructor, "Denied"));
    }

    @Test
    void isCourseOwnerOrCoInstructorOrAdmin_HandlesAllCasesSafely() {
        User owner = new User("Owner", "o@olp.com", "hash", "instructor", "active");
        owner.setId(1);
        User coInstructor = new User("Co", "c@olp.com", "hash", "instructor", "active");
        coInstructor.setId(2);
        User stranger = new User("Other", "x@olp.com", "hash", "instructor", "active");
        stranger.setId(3);
        User admin = new User("Admin", "admin@olp.com", "hash", "admin", "active");
        admin.setId(99);

        Course course = new Course(owner, "Course", "Desc", CourseStatus.PUBLISHED);
        course.setId(50);

        when(courseInstructorRepository.existsByCourseIdAndInstructorId(50, 2)).thenReturn(true);
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(50, 3)).thenReturn(false);

        assertTrue(SecurityUtils.isCourseOwnerOrCoInstructorOrAdmin(course, owner, courseInstructorRepository));
        assertTrue(SecurityUtils.isCourseOwnerOrCoInstructorOrAdmin(course, coInstructor, courseInstructorRepository));
        assertTrue(SecurityUtils.isCourseOwnerOrCoInstructorOrAdmin(course, admin, courseInstructorRepository));
        assertFalse(SecurityUtils.isCourseOwnerOrCoInstructorOrAdmin(course, stranger, courseInstructorRepository));
        assertFalse(SecurityUtils.isCourseOwnerOrCoInstructorOrAdmin(course, coInstructor, null));
        assertFalse(SecurityUtils.isCourseOwnerOrCoInstructorOrAdmin(null, owner, courseInstructorRepository));
        assertFalse(SecurityUtils.isCourseOwnerOrCoInstructorOrAdmin(course, null, courseInstructorRepository));
    }
}
