package com.olp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.olp.entity.User;
import com.olp.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private UserRepository userRepository;
    @Mock private FilterChain chain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        filter = new JwtAuthenticationFilter(jwtUtil, userRepository, mapper);
        request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
        when(jwtUtil.validateToken("token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("token")).thenReturn(5);
        when(jwtUtil.getRoleFromToken("token")).thenReturn("student");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void givenUserWithStatus(String status) {
        User u = new User("Sam", "sam@olp.com", "hash", "student", status);
        u.setId(5);
        when(userRepository.findById(5)).thenReturn(Optional.of(u));
    }

    @Test
    void suspendedUserWithValidTokenIsRejectedWith403() throws Exception {
        givenUserWithStatus("suspended");
        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("suspended"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(chain);
    }

    @Test
    void activeUserIsAuthenticatedAndChainContinues() throws Exception {
        givenUserWithStatus("active");
        filter.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }
}
