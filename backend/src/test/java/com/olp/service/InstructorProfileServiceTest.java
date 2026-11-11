package com.olp.service;

import com.olp.dto.InstructorProfileResponse;
import com.olp.dto.InstructorSummaryResponse;
import com.olp.dto.UpdateInstructorProfileRequest;
import com.olp.entity.InstructorProfile;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.InstructorProfileRepository;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstructorProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private InstructorProfileRepository instructorProfileRepository;

    private InstructorProfileService service;

    private User instructorUser;
    private User studentUser;
    private InstructorProfile existingProfile;

    @BeforeEach
    void setUp() {
        service = new InstructorProfileService(userRepository, instructorProfileRepository);

        instructorUser = new User("Dr. Alan Turing", "turing@olp.com", "hash123", "instructor", "active");
        instructorUser.setId(10);

        studentUser = new User("Ada Lovelace", "ada@olp.com", "hash456", "student", "active");
        studentUser.setId(20);

        existingProfile = new InstructorProfile(instructorUser, "Ph.D. in Computer Science", "AI, Algorithms", "Passionate educator.");
        existingProfile.setId(100);
        existingProfile.setCreatedAt(LocalDateTime.now().minusDays(5));
        existingProfile.setUpdatedAt(LocalDateTime.now().minusDays(1));
    }

    // =========================================================================
    // 1. GET PROFILE TESTS
    // =========================================================================

    @Test
    void testGetProfile_Success() {
        when(userRepository.findById(10)).thenReturn(Optional.of(instructorUser));
        when(instructorProfileRepository.findByInstructorId(10)).thenReturn(Optional.of(existingProfile));

        InstructorProfileResponse response = service.getProfile(instructorUser);

        assertNotNull(response);
        assertEquals(10, response.getId());
        assertEquals("Dr. Alan Turing", response.getName());
        assertEquals("turing@olp.com", response.getEmail());
        assertEquals("instructor", response.getRole());
        assertEquals("Ph.D. in Computer Science", response.getQualifications());
        assertEquals("AI, Algorithms", response.getExpertiseAreas());
        assertEquals("Passionate educator.", response.getBio());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    void testGetProfile_NoProfileRecordYet_ReturnsBasicDetailsWithNullProfileFields() {
        when(userRepository.findById(10)).thenReturn(Optional.of(instructorUser));
        when(instructorProfileRepository.findByInstructorId(10)).thenReturn(Optional.empty());

        InstructorProfileResponse response = service.getProfile(instructorUser);

        assertNotNull(response);
        assertEquals(10, response.getId());
        assertEquals("Dr. Alan Turing", response.getName());
        assertNull(response.getQualifications());
        assertNull(response.getExpertiseAreas());
        assertNull(response.getBio());
    }

    @Test
    void testGetProfile_NullUser_ThrowsInvalidCredentials() {
        assertThrows(InvalidCredentialsException.class, () -> service.getProfile(null));
    }

    @Test
    void testGetProfile_StudentRole_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class, () -> service.getProfile(studentUser));
    }

    // =========================================================================
    // 2. UPDATE PROFILE TESTS & INPUT VALIDATION
    // =========================================================================

    @Test
    void testUpdateProfile_Success_UpdatesFieldsAndTimestamp() {
        when(userRepository.findById(10)).thenReturn(Optional.of(instructorUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(instructorProfileRepository.findByInstructorId(10)).thenReturn(Optional.of(existingProfile));
        when(instructorProfileRepository.save(any(InstructorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime beforeUpdate = existingProfile.getUpdatedAt();

        UpdateInstructorProfileRequest request = new UpdateInstructorProfileRequest(
                "Prof. Alan Turing",
                "D.Sc. in Machine Intelligence",
                "Deep Learning, Quantum Computing",
                "Updated biography for Alan Turing."
        );

        InstructorProfileResponse response = service.updateProfile(instructorUser, request);

        assertNotNull(response);
        assertEquals("Prof. Alan Turing", response.getName());
        assertEquals("D.Sc. in Machine Intelligence", response.getQualifications());
        assertEquals("Deep Learning, Quantum Computing", response.getExpertiseAreas());
        assertEquals("Updated biography for Alan Turing.", response.getBio());
        assertNotNull(response.getUpdatedAt());
        assertTrue(response.getUpdatedAt().isAfter(beforeUpdate) || response.getUpdatedAt().isEqual(beforeUpdate));

        verify(userRepository).save(instructorUser);
        verify(instructorProfileRepository).save(existingProfile);
    }

    @Test
    void testUpdateProfile_CreatesProfileWhenNoneExisted() {
        when(userRepository.findById(10)).thenReturn(Optional.of(instructorUser));
        when(instructorProfileRepository.findByInstructorId(10)).thenReturn(Optional.empty());
        when(instructorProfileRepository.save(any(InstructorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateInstructorProfileRequest request = new UpdateInstructorProfileRequest(
                "Masters in Software Engineering",
                "Cloud Computing",
                "First time creating bio."
        );

        InstructorProfileResponse response = service.updateProfile(instructorUser, request);

        assertNotNull(response);
        assertEquals("Masters in Software Engineering", response.getQualifications());
        assertEquals("Cloud Computing", response.getExpertiseAreas());
        assertEquals("First time creating bio.", response.getBio());
        assertNotNull(response.getUpdatedAt());

        ArgumentCaptor<InstructorProfile> captor = ArgumentCaptor.forClass(InstructorProfile.class);
        verify(instructorProfileRepository).save(captor.capture());
        assertEquals(instructorUser, captor.getValue().getInstructor());
    }

    @Test
    void testUpdateProfile_EmptyName_ThrowsIllegalArgumentException() {
        when(userRepository.findById(10)).thenReturn(Optional.of(instructorUser));

        UpdateInstructorProfileRequest request = new UpdateInstructorProfileRequest(
                "   ",
                "Valid qual",
                "Valid exp",
                "Valid bio"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateProfile(instructorUser, request));
        assertTrue(ex.getMessage().contains("Name cannot be empty"));
    }

    @Test
    void testUpdateProfile_QualificationsTooLong_ThrowsIllegalArgumentException() {
        when(userRepository.findById(10)).thenReturn(Optional.of(instructorUser));
        when(instructorProfileRepository.findByInstructorId(10)).thenReturn(Optional.of(existingProfile));

        String longQual = "A".repeat(1001);
        UpdateInstructorProfileRequest request = new UpdateInstructorProfileRequest(longQual, "Valid exp", "Valid bio");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateProfile(instructorUser, request));
        assertTrue(ex.getMessage().contains("Qualifications must be 1000 characters or fewer"));
    }

    @Test
    void testUpdateProfile_ExpertiseTooLong_ThrowsIllegalArgumentException() {
        when(userRepository.findById(10)).thenReturn(Optional.of(instructorUser));
        when(instructorProfileRepository.findByInstructorId(10)).thenReturn(Optional.of(existingProfile));

        String longExp = "B".repeat(1001);
        UpdateInstructorProfileRequest request = new UpdateInstructorProfileRequest("Valid qual", longExp, "Valid bio");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateProfile(instructorUser, request));
        assertTrue(ex.getMessage().contains("Expertise areas must be 1000 characters or fewer"));
    }

    @Test
    void testUpdateProfile_BioTooLong_ThrowsIllegalArgumentException() {
        when(userRepository.findById(10)).thenReturn(Optional.of(instructorUser));
        when(instructorProfileRepository.findByInstructorId(10)).thenReturn(Optional.of(existingProfile));

        String longBio = "C".repeat(5001);
        UpdateInstructorProfileRequest request = new UpdateInstructorProfileRequest("Valid qual", "Valid exp", longBio);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateProfile(instructorUser, request));
        assertTrue(ex.getMessage().contains("Bio must be 5000 characters or fewer"));
    }

    @Test
    void testUpdateProfile_MismatchedId_ThrowsAccessDeniedException() {
        UpdateInstructorProfileRequest request = new UpdateInstructorProfileRequest("qual", "exp", "bio");
        request.setId(999); // Malicious external ID

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> service.updateProfile(instructorUser, request));
        assertTrue(ex.getMessage().contains("Cannot modify another instructor's profile"));
        verify(instructorProfileRepository, never()).save(any());
    }

    @Test
    void testUpdateProfile_StudentRole_ThrowsAccessDeniedException() {
        UpdateInstructorProfileRequest request = new UpdateInstructorProfileRequest("qual", "exp", "bio");

        assertThrows(AccessDeniedException.class, () -> service.updateProfile(studentUser, request));
    }

    // =========================================================================
    // 3. INSTRUCTOR SUMMARY TESTS (FOR COURSE CARDS)
    // =========================================================================

    @Test
    void testGetInstructorSummary_Success() {
        when(userRepository.findById(10)).thenReturn(Optional.of(instructorUser));
        when(instructorProfileRepository.findByInstructorId(10)).thenReturn(Optional.of(existingProfile));

        InstructorSummaryResponse summary = service.getInstructorSummary(10);

        assertNotNull(summary);
        assertEquals(10, summary.getId());
        assertEquals(10, summary.getInstructorId());
        assertEquals("Dr. Alan Turing", summary.getName());
        assertEquals("turing@olp.com", summary.getEmail());
        assertEquals("Ph.D. in Computer Science", summary.getQualifications());
        assertEquals("AI, Algorithms", summary.getExpertiseAreas());
        assertEquals("Passionate educator.", summary.getBio());
        assertNotNull(summary.getCreatedAt());
        assertNotNull(summary.getUpdatedAt());
    }

    @Test
    void testGetInstructorSummary_InstructorNotFound_ThrowsResourceNotFound() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getInstructorSummary(99));
    }

    @Test
    void testGetInstructorSummary_UserNotAnInstructor_ThrowsResourceNotFound() {
        when(userRepository.findById(20)).thenReturn(Optional.of(studentUser));

        assertThrows(ResourceNotFoundException.class, () -> service.getInstructorSummary(20));
    }
}

