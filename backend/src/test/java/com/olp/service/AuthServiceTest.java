package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.exception.AccountStatusException;
import com.olp.exception.DuplicateEmailException;
import com.olp.exception.InvalidCredentialsException;
import com.olp.repository.UserRepository;
import com.olp.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    // ---------- helpers ----------

    private RegisterRequest registerRequest(String name, String email, String password, String role) {
        RegisterRequest r = new RegisterRequest();
        r.setName(name);
        r.setEmail(email);
        r.setPassword(password);
        r.setRole(role);
        return r;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }

    private User storedUser(Integer id, String role, String status) {
        User u = new User("Sam", "sam@olp.com", "hash", role, status);
        u.setId(id);
        return u;
    }

    private void stubSaveAssigningId(int id) {
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(id);
            return u;
        });
    }

    // =====================================================================
    // register
    // =====================================================================

    @Test
    void testRegister_DuplicateEmail_ThrowsAndSavesNothing() {
        when(userRepository.existsByEmail("dup@olp.com")).thenReturn(true);

        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class,
                () -> authService.register(registerRequest("Dup", "dup@olp.com", "pw", "student")));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, jwtUtil);
    }

    @Test
    void testRegister_DuplicateCheckRunsBeforeAdminRoleCheck() {
        when(userRepository.existsByEmail("dup@olp.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class,
                () -> authService.register(registerRequest("Dup", "dup@olp.com", "pw", "admin")));
    }

    @Test
    void testRegister_AdminRole_IsRejected() {
        when(userRepository.existsByEmail("root@olp.com")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(registerRequest("Root", "root@olp.com", "pw", "admin")));

        assertTrue(ex.getMessage().contains("administrator"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegister_AdminRoleWithDifferentCaseAndSpaces_IsRejected() {
        when(userRepository.existsByEmail("root@olp.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> authService.register(registerRequest("Root", "root@olp.com", "pw", "  ADMIN ")));

        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegister_Student_IsActiveWithNormalisedEmailAndHashedPassword() {
        when(userRepository.existsByEmail(" Stu@OLP.com ")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("encoded");
        stubSaveAssigningId(11);
        when(jwtUtil.generateToken(11, "student")).thenReturn("jwt");

        AuthResponse response = authService.register(registerRequest("Stu", " Stu@OLP.com ", "pw", "student"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("Stu", saved.getName());
        assertEquals("stu@olp.com", saved.getEmail());
        assertEquals("encoded", saved.getPasswordHash());
        assertEquals("student", saved.getRole());
        assertEquals("active", saved.getAccountStatus());

        assertEquals("Registration successful", response.getMessage());
        assertEquals("jwt", response.getToken());
        assertEquals(11, response.getUser().getId());
        assertEquals("stu@olp.com", response.getUser().getEmail());
        assertEquals("student", response.getUser().getRole());
    }

    @Test
    void testRegister_Instructor_IsPendingAndStillReceivesToken() {
        when(userRepository.existsByEmail("inst@olp.com")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("encoded");
        stubSaveAssigningId(12);
        when(jwtUtil.generateToken(12, "instructor")).thenReturn("jwt-inst");

        AuthResponse response = authService.register(registerRequest("Inst", "inst@olp.com", "pw", "Instructor"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("instructor", captor.getValue().getRole());
        assertEquals("pending", captor.getValue().getAccountStatus());
        assertEquals("Registration submitted. Your instructor account is pending administrator approval.",
                response.getMessage());
        assertEquals("jwt-inst", response.getToken());
    }

    // =====================================================================
    // login
    // =====================================================================

    @Test
    void testLogin_UnknownEmail_ThrowsInvalidCredentials() {
        when(userRepository.findByEmail("ghost@olp.com")).thenReturn(Optional.empty());

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                () -> authService.login(loginRequest("ghost@olp.com", "pw")));

        assertEquals("Invalid email or password", ex.getMessage());
        verifyNoInteractions(passwordEncoder, jwtUtil);
    }

    @Test
    void testLogin_WrongPassword_ThrowsSameInvalidCredentialsMessage() {
        when(userRepository.findByEmail("sam@olp.com")).thenReturn(Optional.of(storedUser(1, "student", "active")));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                () -> authService.login(loginRequest("sam@olp.com", "bad")));

        assertEquals("Invalid email or password", ex.getMessage());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void testLogin_PendingStudent_AsksToVerifyEmail() {
        when(userRepository.findByEmail("sam@olp.com")).thenReturn(Optional.of(storedUser(1, "student", "pending")));
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);

        AccountStatusException ex = assertThrows(AccountStatusException.class,
                () -> authService.login(loginRequest("sam@olp.com", "pw")));

        assertTrue(ex.getMessage().contains("verify your email address"));
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void testLogin_PendingInstructor_ReportsAwaitingApproval() {
        when(userRepository.findByEmail("sam@olp.com")).thenReturn(Optional.of(storedUser(1, "instructor", "pending")));
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);

        AccountStatusException ex = assertThrows(AccountStatusException.class,
                () -> authService.login(loginRequest("sam@olp.com", "pw")));

        assertEquals("Your instructor account is pending administrator approval", ex.getMessage());
    }

    @Test
    void testLogin_SuspendedUser_IsBlocked() {
        when(userRepository.findByEmail("sam@olp.com")).thenReturn(Optional.of(storedUser(1, "student", "SUSPENDED")));
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);

        AccountStatusException ex = assertThrows(AccountStatusException.class,
                () -> authService.login(loginRequest("sam@olp.com", "pw")));

        assertEquals("Your account has been suspended. Please contact support", ex.getMessage());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void testLogin_RejectedInstructor_IsBlocked() {
        when(userRepository.findByEmail("sam@olp.com")).thenReturn(Optional.of(storedUser(1, "instructor", "rejected")));
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);

        AccountStatusException ex = assertThrows(AccountStatusException.class,
                () -> authService.login(loginRequest("sam@olp.com", "pw")));

        assertEquals("Your instructor application was rejected", ex.getMessage());
    }

    @Test
    void testLogin_ActiveUser_ReturnsJwtAndUserDetails() {
        when(userRepository.findByEmail("sam@olp.com")).thenReturn(Optional.of(storedUser(7, "student", "active")));
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
        when(jwtUtil.generateToken(7, "student")).thenReturn("jwt-7");

        AuthResponse response = authService.login(loginRequest("  SAM@olp.com ", "pw"));

        assertEquals("Login successful", response.getMessage());
        assertEquals("jwt-7", response.getToken());
        assertEquals(7, response.getUser().getId());
        assertEquals("Sam", response.getUser().getName());
        assertEquals("sam@olp.com", response.getUser().getEmail());
        assertEquals("student", response.getUser().getRole());
    }

    // =====================================================================
    // getCurrentUser
    // =====================================================================

    @Test
    void testGetCurrentUser_MapsEntityToResponse() {
        UserResponse response = authService.getCurrentUser(storedUser(3, "instructor", "active"));

        assertEquals(3, response.getId());
        assertEquals("Sam", response.getName());
        assertEquals("sam@olp.com", response.getEmail());
        assertEquals("instructor", response.getRole());
        verifyNoInteractions(userRepository);
    }

    @Test
    void testGetCurrentUser_NullUser_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> authService.getCurrentUser(null));
    }

    // =====================================================================
    // loginOrRegisterWithGoogle
    // =====================================================================

    @Test
    void testGoogleLogin_NewUser_AutoProvisionsActiveStudent() {
        when(userRepository.findByEmail("gina@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");
        stubSaveAssigningId(21);
        when(jwtUtil.generateToken(21, "student")).thenReturn("jwt-google");

        AuthResponse response = authService.loginOrRegisterWithGoogle(
                new GoogleAuthRequest(" Gina@Gmail.COM ", "  Gina  ", "id-token"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("Gina", saved.getName());
        assertEquals("gina@gmail.com", saved.getEmail());
        assertEquals("student", saved.getRole());
        assertEquals("active", saved.getAccountStatus());
        assertEquals("random-hash", saved.getPasswordHash());

        ArgumentCaptor<String> rawPassword = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(rawPassword.capture());
        assertTrue(rawPassword.getValue().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));

        assertEquals("Authenticated successfully via Google", response.getMessage());
        assertEquals("jwt-google", response.getToken());
        assertEquals(21, response.getUser().getId());
    }

    @Test
    void testGoogleLogin_BlankName_FallsBackToEmailPrefix() {
        when(userRepository.findByEmail("john.doe@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("h");
        stubSaveAssigningId(22);
        when(jwtUtil.generateToken(22, "student")).thenReturn("t");

        authService.loginOrRegisterWithGoogle(new GoogleAuthRequest("john.doe@gmail.com", "   ", "id-token"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("john.doe", captor.getValue().getName());
    }

    @Test
    void testGoogleLogin_NullName_FallsBackToEmailPrefix() {
        when(userRepository.findByEmail("jane@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("h");
        stubSaveAssigningId(23);
        when(jwtUtil.generateToken(23, "student")).thenReturn("t");

        authService.loginOrRegisterWithGoogle(new GoogleAuthRequest("jane@gmail.com", null, "id-token"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("jane", captor.getValue().getName());
    }

    @Test
    void testGoogleLogin_ExistingActiveUser_LogsInWithoutSavingOrReencoding() {
        when(userRepository.findByEmail("sam@olp.com")).thenReturn(Optional.of(storedUser(7, "instructor", "active")));
        when(jwtUtil.generateToken(7, "instructor")).thenReturn("jwt-7");

        AuthResponse response = authService.loginOrRegisterWithGoogle(new GoogleAuthRequest("sam@olp.com", "Sam", "tok"));

        assertEquals("jwt-7", response.getToken());
        assertEquals("instructor", response.getUser().getRole());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void testGoogleLogin_ExistingPendingUser_IsActivated() {
        User pending = storedUser(8, "student", "pending");
        when(userRepository.findByEmail("sam@olp.com")).thenReturn(Optional.of(pending));
        when(userRepository.save(pending)).thenReturn(pending);
        when(jwtUtil.generateToken(8, "student")).thenReturn("jwt-8");

        AuthResponse response = authService.loginOrRegisterWithGoogle(new GoogleAuthRequest("sam@olp.com", "Sam", "tok"));

        assertEquals("active", pending.getAccountStatus());
        assertEquals("jwt-8", response.getToken());
        verify(userRepository).save(pending);
    }

    @Test
    void testGoogleLogin_SuspendedUser_IsBlocked() {
        when(userRepository.findByEmail("sam@olp.com")).thenReturn(Optional.of(storedUser(9, "student", "suspended")));

        AccountStatusException ex = assertThrows(AccountStatusException.class,
                () -> authService.loginOrRegisterWithGoogle(new GoogleAuthRequest("sam@olp.com", "Sam", "tok")));

        assertEquals("Your account has been suspended. Please contact support", ex.getMessage());
        verifyNoInteractions(jwtUtil);
        verify(userRepository, never()).save(any());
    }

    @Test
    void testGoogleLogin_RejectedUser_IsBlocked() {
        when(userRepository.findByEmail("sam@olp.com")).thenReturn(Optional.of(storedUser(9, "instructor", "rejected")));

        AccountStatusException ex = assertThrows(AccountStatusException.class,
                () -> authService.loginOrRegisterWithGoogle(new GoogleAuthRequest("sam@olp.com", "Sam", "tok")));

        assertEquals("Your instructor application was rejected", ex.getMessage());
        verifyNoInteractions(jwtUtil);
    }
}
