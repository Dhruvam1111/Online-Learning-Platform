package com.olp.dto;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @BeforeEach
    @AfterEach
    void cleanMdc() {
        MDC.clear();
    }

    @Test
    void testConstructorWithStatusAndError_PicksUpTraceIdFromMdc() {
        MDC.put("traceId", "trace-mdc-42");
        ErrorResponse response = new ErrorResponse(400, "Bad Request");

        assertEquals(400, response.getStatus());
        assertEquals("Bad Request", response.getError());
        assertEquals("Bad Request", response.getMessage());
        assertEquals("trace-mdc-42", response.getTraceId());
        assertNotNull(response.getTimestamp());
        assertTrue(response.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testConstructorWithStatusAndError_LeavesTraceIdNullWhenMdcEmpty() {
        ErrorResponse response = new ErrorResponse(404, "Not Found");

        assertEquals(404, response.getStatus());
        assertEquals("Not Found", response.getError());
        assertNull(response.getTraceId());
    }

    @Test
    void testConstructorWithExplicitTraceId() {
        ErrorResponse response = new ErrorResponse(500, "Internal Server Error", "explicit-trace-99");

        assertEquals(500, response.getStatus());
        assertEquals("Internal Server Error", response.getError());
        assertEquals("explicit-trace-99", response.getTraceId());
        assertNotNull(response.getTimestamp());
    }
}
