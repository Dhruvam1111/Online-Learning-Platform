package com.olp.exception;

import com.olp.dto.ErrorResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void handlesAllCustomAndStandardExceptionsWithExpectedStatusAndBody() {
        ResponseEntity<ErrorResponse> r409Dup = handler.handleDuplicateEmail(new DuplicateEmailException("Email in use"));
        assertEquals(409, r409Dup.getStatusCode().value());
        assertEquals("Email in use", r409Dup.getBody().getError());

        ResponseEntity<ErrorResponse> r401 = handler.handleInvalidCredentials(new InvalidCredentialsException("Bad login"));
        assertEquals(401, r401.getStatusCode().value());
        assertEquals("Bad login", r401.getBody().getError());

        ResponseEntity<ErrorResponse> r403Status = handler.handleAccountStatus(new AccountStatusException("Suspended"));
        assertEquals(403, r403Status.getStatusCode().value());
        assertEquals("Suspended", r403Status.getBody().getError());

        ResponseEntity<ErrorResponse> r404 = handler.handleResourceNotFound(new ResourceNotFoundException("Not found"));
        assertEquals(404, r404.getStatusCode().value());

        ResponseEntity<ErrorResponse> r400Arg = handler.handleIllegalArgument(new IllegalArgumentException("Bad input"));
        assertEquals(400, r400Arg.getStatusCode().value());

        ResponseEntity<ErrorResponse> r409State = handler.handleConflict(new IllegalStateException("Already submitted"));
        assertEquals(409, r409State.getStatusCode().value());

        ResponseEntity<ErrorResponse> r403DeniedNull = handler.handleAccessDenied(new AccessDeniedException(null));
        assertEquals(403, r403DeniedNull.getStatusCode().value());
        assertEquals("Access denied", r403DeniedNull.getBody().getError());

        HttpInputMessage msg = new MockHttpInputMessage("{bad".getBytes(StandardCharsets.UTF_8));
        ResponseEntity<ErrorResponse> r400Readable = handler.handleMessageNotReadable(
                new HttpMessageNotReadableException("Parse error", msg));
        assertEquals(400, r400Readable.getStatusCode().value());
        assertEquals("Malformed request body", r400Readable.getBody().getError());

        ResponseEntity<ErrorResponse> rRseReason = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.GONE, "Expired link"));
        assertEquals(410, rRseReason.getStatusCode().value());
        assertEquals("Expired link", rRseReason.getBody().getError());

        ResponseEntity<ErrorResponse> rRseNullReason = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE));
        assertEquals(406, rRseNullReason.getStatusCode().value());
        assertNotNull(rRseNullReason.getBody().getError());
    }

    @Test
    void handleGenericException_UsesTraceIdFromMdc() {
        MDC.put("traceId", "mdc-trace-123");
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(
                new RuntimeException("Database timeout"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("mdc-trace-123", response.getBody().getTraceId());
        assertTrue(response.getBody().getError().contains("mdc-trace-123"));
    }

    @Test
    void handleGenericException_UsesTraceIdFromRequestAttributeWhenMdcEmpty() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("traceId", "request-attr-trace-456");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(
                new NullPointerException("Null pointer"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("request-attr-trace-456", response.getBody().getTraceId());
        assertTrue(response.getBody().getError().contains("request-attr-trace-456"));
    }

    @Test
    void handleGenericException_GeneratesUuidWhenNoTracePresent() {
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(
                new IllegalStateException("Unexpected crash"), null);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getTraceId());
        assertFalse(response.getBody().getTraceId().trim().isEmpty());
        assertTrue(response.getBody().getError().contains(response.getBody().getTraceId()));
    }

    @Test
    void handleValidation_WithFieldErrorsAndWithoutFieldErrors() throws Exception {
        org.springframework.core.MethodParameter parameter = new org.springframework.core.MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("handleValidation_WithFieldErrorsAndWithoutFieldErrors"), -1);

        org.springframework.validation.BeanPropertyBindingResult bindingResult =
                new org.springframework.validation.BeanPropertyBindingResult(new Object(), "user");
        bindingResult.addError(new FieldError("user", "email", "must not be blank"));
        MethodArgumentNotValidException exWithErrors = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> res = handler.handleValidation(exWithErrors);
        assertEquals(400, res.getStatusCode().value());
        assertEquals("email: must not be blank", res.getBody().getError());

        org.springframework.validation.BeanPropertyBindingResult bindingResultEmpty =
                new org.springframework.validation.BeanPropertyBindingResult(new Object(), "user");
        MethodArgumentNotValidException exEmpty = new MethodArgumentNotValidException(parameter, bindingResultEmpty);

        ResponseEntity<ErrorResponse> resEmpty = handler.handleValidation(exEmpty);
        assertEquals(400, resEmpty.getStatusCode().value());
        assertEquals("Validation failed", resEmpty.getBody().getError());
    }
}
