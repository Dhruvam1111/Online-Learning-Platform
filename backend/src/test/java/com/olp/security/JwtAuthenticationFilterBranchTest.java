package com.olp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.dto.ErrorResponse;
import com.olp.entity.User;
import com.olp.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Writer;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Header/token/status branches of JwtAuthenticationFilter not covered by
 * JwtAuthenticationFilterTest.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterBranchTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtUtil, userRepository, objectMapper);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User userWithStatus(Integer id, String role, String status) {
        User user = new User("Test User", "user@example.com", "hash", role, status);
        user.setId(id);
        return user;
    }

    private void givenValidTokenFor(User user, String tokenRole) {
        request.addHeader("Authorization", "Bearer good-token");
        when(jwtUtil.validateToken("good-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("good-token")).thenReturn(user.getId());
        when(jwtUtil.getRoleFromToken("good-token")).thenReturn(tokenRole);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    @Test
    void testNoAuthorizationHeader_ContinuesChainUnauthenticated() throws Exception {
        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtil, userRepository);
    }

    @Test
    void testNonBearerHeader_ContinuesChainUnauthenticated() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtil, userRepository);
    }

    @Test
    void testLowercaseBearerPrefix_IsNotAccepted() throws Exception {
        request.addHeader("Authorization", "bearer some-token");

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void testInvalidToken_ContinuesChainUnauthenticated() throws Exception {
        request.addHeader("Authorization", "Bearer bad-token");
        when(jwtUtil.validateToken("bad-token")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtil, never()).getUserIdFromToken(anyString());
        verifyNoInteractions(userRepository);
    }

    @Test
    void testEmptyBearerToken_IsTreatedAsInvalid() throws Exception {
        request.addHeader("Authorization", "Bearer ");
        when(jwtUtil.validateToken("")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testValidTokenButUserDeleted_ContinuesChainUnauthenticated() throws Exception {
        request.addHeader("Authorization", "Bearer good-token");
        when(jwtUtil.validateToken("good-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("good-token")).thenReturn(9);
        when(jwtUtil.getRoleFromToken("good-token")).thenReturn("student");
        when(userRepository.findById(9)).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testValidTokenAndActiveUser_SetsUserPrincipalAndRoleAuthority() throws Exception {
        User user = userWithStatus(3, "student", "active");
        givenValidTokenFor(user, "student");

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
        assertSame(user, auth.getPrincipal());
        assertNull(auth.getCredentials());
        assertEquals(1, auth.getAuthorities().size());
        assertEquals("ROLE_student", auth.getAuthorities().iterator().next().getAuthority());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testAuthorityComesFromTokenRoleNotFromUserEntity() throws Exception {
        User user = userWithStatus(4, "student", "active");
        givenValidTokenFor(user, "instructor");

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("ROLE_instructor", auth.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void testPendingUser_Gets403AndChainIsNotContinued() throws Exception {
        assertBlockedWith("pending",
                "Awaiting admin approval: Your instructor account is pending administrator approval");
    }

    @Test
    void testRejectedUser_Gets403AndChainIsNotContinued() throws Exception {
        assertBlockedWith("rejected", "Your instructor application was rejected");
    }

    @Test
    void testSuspendedUser_ErrorBodyHasStatusAndMessage() throws Exception {
        assertBlockedWith("suspended", "Your account has been suspended. Please contact support");
    }

    @Test
    void testBlockedStatusCheckIsCaseInsensitive() throws Exception {
        assertBlockedWith("SUSPENDED", "Your account has been suspended. Please contact support");
    }

    private void assertBlockedWith(String status, String expectedMessage) throws Exception {
        givenValidTokenFor(userWithStatus(5, "instructor", status), "instructor");

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(Objects.requireNonNull(response.getContentType()).startsWith("application/json"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, never()).doFilter(any(), any());

        ArgumentCaptor<ErrorResponse> body = ArgumentCaptor.forClass(ErrorResponse.class);
        verify(objectMapper).writeValue(any(Writer.class), body.capture());
        assertEquals(403, body.getValue().getStatus());
        assertEquals(expectedMessage, body.getValue().getMessage());
    }
}
