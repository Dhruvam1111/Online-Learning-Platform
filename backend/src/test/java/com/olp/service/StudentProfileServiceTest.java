package com.olp.service;

import com.olp.dto.StudentProfileResponse;
import com.olp.dto.UpdateStudentProfileRequest;
import com.olp.entity.StudentProfile;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.repository.StudentProfileRepository;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;

    @InjectMocks
    private StudentProfileService service;

    private User student;

    @BeforeEach
    void setUp() {
        student = new User("Sam", "sam@olp.com", "hash", "student", "active");
        student.setId(5);
    }

    private UpdateStudentProfileRequest request(String name, String edu, String interests, String goals, String bio) {
        return new UpdateStudentProfileRequest(name, edu, interests, goals, bio);
    }

    // ===== resolveUser =====

    @Test
    void testResolveUser_NullAuthentication_Throws() {
        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(null));
    }

    @Test
    void testResolveUser_NullPrincipal_Throws() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(null, null);

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(auth));
        assertTrue(ex.getMessage().contains("not authenticated"));
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

    // ===== getProfile =====

    @Test
    void testGetProfile_NullUser_Throws() {
        assertThrows(InvalidCredentialsException.class, () -> service.getProfile(null));
    }

    @Test
    void testGetProfile_NonStudent_ThrowsAccessDenied() {
        User instructor = new User("Ivy", "ivy@olp.com", "hash", "instructor", "active");
        instructor.setId(10);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> service.getProfile(instructor));
        assertTrue(ex.getMessage().contains("view"));
        verifyNoInteractions(userRepository, studentProfileRepository);
    }

    @Test
    void testGetProfile_RoleCheckIsCaseInsensitive() {
        student.setRole("STUDENT");
        when(userRepository.findById(5)).thenReturn(Optional.of(student));
        when(studentProfileRepository.findByStudentId(5)).thenReturn(Optional.empty());

        assertNotNull(service.getProfile(student));
    }

    @Test
    void testGetProfile_UserMissingInRepository_Throws() {
        when(userRepository.findById(5)).thenReturn(Optional.empty());

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> service.getProfile(student));
        assertTrue(ex.getMessage().contains("User not found"));
    }

    @Test
    void testGetProfile_NoProfileYet_ReturnsNullProfileFields() {
        when(userRepository.findById(5)).thenReturn(Optional.of(student));
        when(studentProfileRepository.findByStudentId(5)).thenReturn(Optional.empty());

        StudentProfileResponse response = service.getProfile(student);

        assertEquals(5, response.id());
        assertEquals("Sam", response.name());
        assertEquals("sam@olp.com", response.email());
        assertEquals("student", response.role());
        assertNull(response.educationLevel());
        assertNull(response.interests());
        assertNull(response.learningGoals());
        assertNull(response.bio());
    }

    @Test
    void testGetProfile_ExistingProfile_MapsAllFields() {
        StudentProfile profile = new StudentProfile();
        profile.setStudent(student);
        profile.setEducationLevel("Bachelor");
        profile.setInterests("Java, SQL");
        profile.setLearningGoals("Get a job");
        profile.setBio("Hello");
        when(userRepository.findById(5)).thenReturn(Optional.of(student));
        when(studentProfileRepository.findByStudentId(5)).thenReturn(Optional.of(profile));

        StudentProfileResponse response = service.getProfile(student);

        assertEquals("Bachelor", response.educationLevel());
        assertEquals("Java, SQL", response.interests());
        assertEquals("Get a job", response.learningGoals());
        assertEquals("Hello", response.bio());
    }

    // ===== updateProfile =====

    @Test
    void testUpdateProfile_NullUser_Throws() {
        assertThrows(InvalidCredentialsException.class,
                () -> service.updateProfile(null, request(null, null, null, null, null)));
    }

    @Test
    void testUpdateProfile_NonStudent_ThrowsAccessDenied() {
        User admin = new User("Admin", "admin@olp.com", "hash", "admin", "active");
        admin.setId(1);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> service.updateProfile(admin, request("x", null, null, null, null)));
        assertTrue(ex.getMessage().contains("update"));
        verifyNoInteractions(userRepository, studentProfileRepository);
    }

    @Test
    void testUpdateProfile_UserMissingInRepository_Throws() {
        when(userRepository.findById(5)).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> service.updateProfile(student, request(null, null, null, null, null)));
    }

    @Test
    void testUpdateProfile_BlankName_ThrowsAndSavesNothing() {
        when(userRepository.findById(5)).thenReturn(Optional.of(student));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateProfile(student, request("   ", null, null, null, null)));

        assertTrue(ex.getMessage().contains("Name cannot be empty"));
        verify(userRepository, never()).save(any(User.class));
        verify(studentProfileRepository, never()).save(any(StudentProfile.class));
    }

    @Test
    void testUpdateProfile_NullName_DoesNotSaveUser() {
        when(userRepository.findById(5)).thenReturn(Optional.of(student));
        when(studentProfileRepository.findByStudentId(5)).thenReturn(Optional.empty());
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(i -> i.getArgument(0));

        StudentProfileResponse response = service.updateProfile(student, request(null, "Master", null, null, null));

        verify(userRepository, never()).save(any(User.class));
        assertEquals("Sam", response.name());
        assertEquals("Master", response.educationLevel());
    }

    @Test
    void testUpdateProfile_ValidName_IsTrimmedAndUserSaved() {
        when(userRepository.findById(5)).thenReturn(Optional.of(student));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(studentProfileRepository.findByStudentId(5)).thenReturn(Optional.empty());
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(i -> i.getArgument(0));

        StudentProfileResponse response = service.updateProfile(student, request("  Samuel  ", null, null, null, null));

        assertEquals("Samuel", response.name());
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("Samuel", saved.getValue().getName());
    }

    @Test
    void testUpdateProfile_NoExistingProfile_CreatesOneLinkedToStudentWithTrimmedFields() {
        when(userRepository.findById(5)).thenReturn(Optional.of(student));
        when(studentProfileRepository.findByStudentId(5)).thenReturn(Optional.empty());
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(i -> i.getArgument(0));

        StudentProfileResponse response = service.updateProfile(student,
                request(null, " Bachelor ", " AI ", " Learn ML ", " Bio text "));

        ArgumentCaptor<StudentProfile> saved = ArgumentCaptor.forClass(StudentProfile.class);
        verify(studentProfileRepository).save(saved.capture());
        assertSame(student, saved.getValue().getStudent());
        assertEquals("Bachelor", saved.getValue().getEducationLevel());
        assertEquals("AI", saved.getValue().getInterests());
        assertEquals("Learn ML", saved.getValue().getLearningGoals());
        assertEquals("Bio text", saved.getValue().getBio());
        assertEquals("Bachelor", response.educationLevel());
        assertEquals("Bio text", response.bio());
    }

    @Test
    void testUpdateProfile_ExistingProfile_NullFieldsAreLeftUnchanged() {
        StudentProfile existing = new StudentProfile();
        existing.setStudent(student);
        existing.setEducationLevel("Bachelor");
        existing.setInterests("Java");
        existing.setLearningGoals("Job");
        existing.setBio("Old bio");
        when(userRepository.findById(5)).thenReturn(Optional.of(student));
        when(studentProfileRepository.findByStudentId(5)).thenReturn(Optional.of(existing));
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(i -> i.getArgument(0));

        StudentProfileResponse response = service.updateProfile(student, request(null, null, "Python", null, null));

        assertEquals("Bachelor", response.educationLevel());
        assertEquals("Python", response.interests());
        assertEquals("Job", response.learningGoals());
        assertEquals("Old bio", response.bio());
        verify(studentProfileRepository).save(existing);
    }

    @Test
    void testUpdateProfile_EmptyStringField_OverwritesExistingValue() {
        StudentProfile existing = new StudentProfile();
        existing.setStudent(student);
        existing.setBio("Old bio");
        when(userRepository.findById(5)).thenReturn(Optional.of(student));
        when(studentProfileRepository.findByStudentId(5)).thenReturn(Optional.of(existing));
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(i -> i.getArgument(0));

        StudentProfileResponse response = service.updateProfile(student, request(null, null, null, null, "   "));

        assertEquals("", response.bio());
    }
}
