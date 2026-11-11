package com.olp.service;

import com.olp.dto.ApiResponse;
import com.olp.dto.AuthResponse;
import com.olp.dto.LoginRequest;
import com.olp.dto.RegisterRequest;
import com.olp.entity.InstructorProfile;
import com.olp.entity.User;
import com.olp.exception.AccountStatusException;
import com.olp.exception.DuplicateEmailException;
import com.olp.exception.InvalidCredentialsException;
import com.olp.repository.InstructorProfileRepository;
import com.olp.repository.UserRepository;
import com.olp.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private InstructorProfileRepository instructorProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User activeStudent;
    private User pendingInstructor;

    @BeforeEach
    void setUp() {
        activeStudent = new User("Alice", "alice@test.com", "encodedHash", "student", "active");
        activeStudent.setId(1);

        pendingInstructor = new User("Dr. Smith", "smith@test.com", "encodedHash", "instructor", "pending");
        pendingInstructor.setId(2);
    }

    @Test
    void testRegister_Instructor_PendingApproval_SavesProfileAndLink() {
        RegisterRequest request = new RegisterRequest(
                "Dr. Smith",
                "smith@test.com",
                "Password123!",
                "instructor",
                "Distributed Systems",
                "PhD in Computer Science"
        );

        when(userRepository.existsByEmail("smith@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2);
            return u;
        });
        when(jwtUtil.generateToken(eq(2), eq("VERIFY_EMAIL"))).thenReturn("verifyToken123");
        when(jwtUtil.generateToken(eq(2), eq("instructor"))).thenReturn("jwtToken123");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwtToken123", response.getToken());
        assertTrue(response.getMessage().toLowerCase().contains("awaiting administrator approval") ||
                   response.getMessage().toLowerCase().contains("pending"));
        assertNotNull(response.getVerificationLink());
        assertTrue(response.getVerificationLink().contains("verifyToken123"));

        // Verify User was saved with accountStatus = "pending"
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("pending", userCaptor.getValue().getAccountStatus());
        assertEquals("instructor", userCaptor.getValue().getRole());

        // Verify InstructorProfile was saved with expertise and qualifications
        ArgumentCaptor<InstructorProfile> profileCaptor = ArgumentCaptor.forClass(InstructorProfile.class);
        verify(instructorProfileRepository).save(profileCaptor.capture());
        assertEquals("Distributed Systems", profileCaptor.getValue().getExpertiseAreas());
        assertEquals("PhD in Computer Science", profileCaptor.getValue().getQualifications());
    }

    @Test
    void testRegister_Student_ActiveByDefault_NoInstructorProfile() {
        RegisterRequest request = new RegisterRequest(
                "Bob",
                "bob@test.com",
                "Password123!",
                "student"
        );

        when(userRepository.existsByEmail("bob@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(3);
            return u;
        });
        when(jwtUtil.generateToken(eq(3), eq("student"))).thenReturn("studentJwt");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("Registration successful", response.getMessage());
        assertNull(response.getVerificationLink());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("active", userCaptor.getValue().getAccountStatus());
        verify(instructorProfileRepository, never()).save(any());
    }

    @Test
    void testRegister_DuplicateEmail_ThrowsException() {
        RegisterRequest request = new RegisterRequest("Dup", "dup@test.com", "pass12345", "instructor");
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testLogin_PendingInstructor_ThrowsAccountStatusExceptionWithAwaitingApprovalMessage() {
        LoginRequest request = new LoginRequest("smith@test.com", "password123");

        when(userRepository.findByEmail("smith@test.com")).thenReturn(Optional.of(pendingInstructor));
        when(passwordEncoder.matches("password123", "encodedHash")).thenReturn(true);

        AccountStatusException ex = assertThrows(AccountStatusException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("Awaiting admin approval"));
    }

    @Test
    void testLogin_ActiveStudent_Success() {
        LoginRequest request = new LoginRequest("alice@test.com", "password123");

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(activeStudent));
        when(passwordEncoder.matches("password123", "encodedHash")).thenReturn(true);
        when(jwtUtil.generateToken(1, "student")).thenReturn("token123");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("Login successful", response.getMessage());
        assertEquals("token123", response.getToken());
    }

    @Test
    void testVerifyEmail_ValidToken_ReturnsSuccess() {
        String token = "validVerifyToken";
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(2);
        when(userRepository.findById(2)).thenReturn(Optional.of(pendingInstructor));

        ApiResponse response = authService.verifyEmail(token);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("Email verified successfully"));
    }

    @Test
    void testVerifyEmail_InvalidToken_ThrowsException() {
        String token = "badToken";
        when(jwtUtil.validateToken(token)).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.verifyEmail(token));
    }
}

