package com.olp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.dto.ErrorResponse;
import com.olp.entity.User;
import com.olp.exception.AccountStatusException;
import com.olp.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. Extract the token from the Authorization header
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token present — let the request continue (it will be
            // blocked later by SecurityConfig if the endpoint requires auth)
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        // 2. Validate the token
        if (!jwtUtil.validateToken(token)) {
            // Token is invalid or expired
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract user info from the token
        Integer userId = jwtUtil.getUserIdFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);

        // 4. Load the full user from the database
        //    (ensures the user still exists and hasn't been deleted since the token was issued)
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4b. Revoke access for accounts that are no longer active. The user row is already
        //     loaded above, so this check costs no extra database query. This closes the gap
        //     where a suspended user's JWT stayed valid until it expired.
        try {
            assertAccountActive(user);
        } catch (AccountStatusException ex) {
            SecurityContextHolder.clearContext();
            writeForbidden(response, ex.getMessage());
            return;
        }

        // 5. Set the authentication in Spring Security's context
        //    The "ROLE_" prefix is a Spring Security convention.
        //    Person 3 uses @PreAuthorize("hasRole('instructor')") — Spring adds the prefix automatically.
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user,                                                    // principal (the User object)
                        null,                                                    // credentials (not needed, we have a token)
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))      // authorities
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 6. Continue the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Mirrors the account-status rules enforced at login (AuthService.login).
     */
    private void assertAccountActive(User user) {
        String status = user.getAccountStatus();
        if ("suspended".equalsIgnoreCase(status)) {
            throw new AccountStatusException("Your account has been suspended. Please contact support");
        } else if ("pending".equalsIgnoreCase(status)) {
            throw new AccountStatusException("Your instructor account is pending administrator approval");
        } else if ("rejected".equalsIgnoreCase(status)) {
            throw new AccountStatusException("Your instructor application was rejected");
        }
    }

    /**
     * Filters run before Spring MVC, so @RestControllerAdvice cannot handle exceptions thrown here.
     * Write the same 403 JSON body that GlobalExceptionHandler produces for AccountStatusException.
     */
    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(403, message));
    }
}
