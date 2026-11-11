package com.olp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.dto.AuthResponse;
import com.olp.dto.CourseResponse;
import com.olp.dto.LoginRequest;
import com.olp.dto.RegisterRequest;
import com.olp.dto.SendOtpResponse;
import com.olp.dto.UserResponse;
import com.olp.entity.CourseStatus;
import com.olp.entity.User;
import com.olp.exception.AccountStatusException;
import com.olp.exception.DuplicateEmailException;
import com.olp.exception.GlobalExceptionHandler;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.service.AuthService;
import com.olp.service.InstructorCourseService;
import com.olp.service.OtpAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc tests (no Spring context, services mocked) that verify the HTTP contract the
 * direct-call controller tests skip: @Valid -> 400, malformed JSON -> 400, and the
 * exception -> status mapping in {@link GlobalExceptionHandler}.
 *
 * NOTE: standalone mode does not apply Spring Security, so @PreAuthorize and route-level role rules
 * are NOT exercised here. Authentication is only supplied via {@code .principal(...)} so controllers
 * that take an {@link Authentication} argument can be invoked.
 */
@ExtendWith(MockitoExtension.class)
class ApiErrorHandlingMockMvcTest {

    @Mock
    private AuthService authService;
    @Mock
    private OtpAuthService otpAuthService;
    @Mock
    private InstructorCourseService instructorCourseService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new AuthController(authService),
                        new OtpAuthController(otpAuthService),
                        new InstructorCourseController(instructorCourseService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MockHttpServletRequestBuilder postJson(String url, Object body) throws Exception {
        return post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }

    private Map<String, String> validRegisterBody() {
        return Map.of(
                "name", "Sam Student",
                "email", "sam@example.com",
                "password", "password123",
                "role", "student");
    }

    // ===== @Valid failures -> 400 "field: message" =====

    @Test
    void testRegister_InvalidEmailFormat_Returns400WithFieldMessage() throws Exception {
        Map<String, String> body = new java.util.HashMap<>(validRegisterBody());
        body.put("email", "not-an-email");

        mockMvc.perform(postJson("/api/auth/register", body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("email: Email must be a valid email address"))
                .andExpect(jsonPath("$.error").value("email: Email must be a valid email address"));

        verifyNoInteractions(authService);
    }

    @Test
    void testRegister_ShortPassword_Returns400() throws Exception {
        Map<String, String> body = new java.util.HashMap<>(validRegisterBody());
        body.put("password", "short");

        mockMvc.perform(postJson("/api/auth/register", body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("password: Password must be between 8 and 100 characters"));
    }

    @Test
    void testRegister_AdminRoleRejectedByPattern_Returns400() throws Exception {
        Map<String, String> body = new java.util.HashMap<>(validRegisterBody());
        body.put("role", "admin");

        mockMvc.perform(postJson("/api/auth/register", body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("role: Role must be 'student' or 'instructor'"));

        verifyNoInteractions(authService);
    }

    @Test
    void testLogin_BlankPassword_Returns400() throws Exception {
        mockMvc.perform(postJson("/api/auth/login", Map.of("email", "sam@example.com", "password", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("password: Password is required"));

        verifyNoInteractions(authService);
    }

    @Test
    void testResetPassword_OtpNotSixDigits_Returns400() throws Exception {
        Map<String, String> body = Map.of(
                "email", "sam@example.com",
                "otpCode", "12ab",
                "newPassword", "newpass1",
                "confirmPassword", "newpass1");

        mockMvc.perform(postJson("/api/auth/password-reset/reset", body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("otpCode: OTP must be exactly 6 digits"));

        verifyNoInteractions(otpAuthService);
    }

    @Test
    void testCreateCourse_BlankTitle_Returns400() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(instructorUser(5), null);

        mockMvc.perform(postJson("/api/instructor/courses", Map.of("title", "", "description", "d")).principal(auth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title: Title is required"));

        verifyNoInteractions(instructorCourseService);
    }

    // ===== Malformed body -> 400 =====

    @Test
    void testLogin_MalformedJson_Returns400MalformedRequestBody() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed request body"));

        verifyNoInteractions(authService);
    }

    // ===== Exception -> HTTP status mapping =====

    @Test
    void testRegister_DuplicateEmail_Returns409() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateEmailException("An account with this email already exists"));

        mockMvc.perform(postJson("/api/auth/register", validRegisterBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));
    }

    @Test
    void testLogin_InvalidCredentials_Returns401() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(postJson("/api/auth/login", Map.of("email", "sam@example.com", "password", "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void testLogin_SuspendedAccount_Returns403() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AccountStatusException("Your account has been suspended. Please contact support"));

        mockMvc.perform(postJson("/api/auth/login", Map.of("email", "sam@example.com", "password", "password123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Your account has been suspended. Please contact support"));
    }

    @Test
    void testPasswordResetSendOtp_UnknownEmail_Returns404() throws Exception {
        when(otpAuthService.sendPasswordResetOtp("ghost@example.com"))
                .thenThrow(new ResourceNotFoundException("Email does not exist. No account is registered with this email address."));

        mockMvc.perform(postJson("/api/auth/password-reset/send-otp", Map.of("email", "ghost@example.com")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Email does not exist. No account is registered with this email address."));
    }

    @Test
    void testRegistrationSendOtp_UndeliverableDomain_Returns400() throws Exception {
        when(otpAuthService.sendRegistrationOtp(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Email does not exist. The domain is invalid or cannot receive emails."));

        mockMvc.perform(postJson("/api/auth/register/send-otp", validRegisterBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Email does not exist. The domain is invalid or cannot receive emails."));
    }

    @Test
    void testInstructorCourses_QueryParamForAnotherInstructor_Returns403() throws Exception {
        User instructor = instructorUser(5);
        Authentication auth = new UsernamePasswordAuthenticationToken(instructor, null);
        when(instructorCourseService.resolveUser(any(Authentication.class))).thenReturn(instructor);

        mockMvc.perform(get("/api/instructor/courses").param("instructorId", "99").principal(auth))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Cannot access another instructor's courses"));

        verify(instructorCourseService, never()).getInstructorCourses(any());
    }

    @Test
    void testInstructorCourses_UserIdQueryParamForAnotherInstructor_Returns403() throws Exception {
        User instructor = instructorUser(5);
        Authentication auth = new UsernamePasswordAuthenticationToken(instructor, null);
        when(instructorCourseService.resolveUser(any(Authentication.class))).thenReturn(instructor);

        mockMvc.perform(get("/api/instructors/courses").param("userId", "42").principal(auth))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Cannot access another instructor's courses"));
    }

    @Test
    void testInstructorCourses_MatchingInstructorId_Returns200WithCourses() throws Exception {
        User instructor = instructorUser(5);
        Authentication auth = new UsernamePasswordAuthenticationToken(instructor, null);
        CourseResponse course = new CourseResponse(10, 5, "Spring Boot", "Desc", CourseStatus.DRAFT, null);
        when(instructorCourseService.resolveUser(any(Authentication.class))).thenReturn(instructor);
        when(instructorCourseService.getInstructorCourses(instructor)).thenReturn(List.of(course));

        mockMvc.perform(get("/api/instructor/courses").param("instructorId", "5").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].title").value("Spring Boot"))
                .andExpect(jsonPath("$[0].instructorId").value(5));
    }

    // ===== Success response shapes =====

    @Test
    void testRegister_Success_Returns201WithAuthResponseShape() throws Exception {
        AuthResponse authResponse = new AuthResponse("Registration successful", "jwt-token",
                new UserResponse(1, "Sam Student", "sam@example.com", "student"));
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(postJson("/api/auth/register", validRegisterBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.email").value("sam@example.com"))
                .andExpect(jsonPath("$.user.role").value("student"));
    }

    @Test
    void testPasswordResetSendOtp_Success_Returns200WithMaskedEmail() throws Exception {
        when(otpAuthService.sendPasswordResetOtp("sam@example.com"))
                .thenReturn(new SendOtpResponse("Reset code sent", "sa***@example.com"));

        mockMvc.perform(postJson("/api/auth/password-reset/send-otp", Map.of("email", "sam@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reset code sent"))
                .andExpect(jsonPath("$.maskedEmail").value("sa***@example.com"));
    }

    @Test
    void testPasswordResetReset_Success_Returns200WithMessage() throws Exception {
        Map<String, String> body = Map.of(
                "email", "sam@example.com",
                "otpCode", "123456",
                "newPassword", "newpass1",
                "confirmPassword", "newpass1");

        mockMvc.perform(postJson("/api/auth/password-reset/reset", body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Password has been successfully reset. You may now log in with your new password."));

        verify(otpAuthService, times(1)).resetPasswordWithOtp(any());
    }

    @Test
    void testVerifyRegistrationOtp_Success_Returns201() throws Exception {
        AuthResponse authResponse = new AuthResponse("Account successfully verified! Welcome to Focotech.", "jwt-token",
                new UserResponse(1, "Sam Student", "sam@example.com", "student"));
        when(otpAuthService.verifyRegistrationOtp(any())).thenReturn(authResponse);

        mockMvc.perform(postJson("/api/auth/register/verify-otp", Map.of("email", "sam@example.com", "otpCode", "123456")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void testLogout_Returns200WithMessage() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    private User instructorUser(Integer id) {
        User user = new User("Dr. Chen", "chen@example.com", "hash", "instructor");
        user.setId(id);
        return user;
    }
}
