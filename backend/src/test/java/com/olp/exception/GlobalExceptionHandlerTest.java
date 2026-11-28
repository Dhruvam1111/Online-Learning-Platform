package com.olp.exception;

import com.olp.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

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
}
