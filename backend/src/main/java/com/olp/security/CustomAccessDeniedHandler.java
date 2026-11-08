package com.olp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.dto.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Reusable Access Denied Handler for Role-Based Access Control (RBAC).
 * Returns consistent 403 Forbidden JSON when an authenticated user lacks the
 * required role.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler() {
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String message = accessDeniedException.getMessage() != null && !accessDeniedException.getMessage().isBlank()
                ? accessDeniedException.getMessage()
                : "Access denied: insufficient permissions to access this resource";

        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.FORBIDDEN.value(), message);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
