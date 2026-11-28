package com.olp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TraceIdFilterTest {

    private TraceIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new TraceIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void testExistingXTraceIdHeaderIsPropagatedAndSetInMdcAndResponse() throws ServletException, IOException {
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "custom-trace-12345");

        doAnswer(invocation -> {
            assertEquals("custom-trace-12345", MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY));
            assertEquals("custom-trace-12345", request.getAttribute(TraceIdFilter.MDC_TRACE_ID_KEY));
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        assertEquals("custom-trace-12345", response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
        assertNull(MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY), "MDC traceId must be cleared after request");
    }

    @Test
    void testFallbackToCorrelationIdWhenTraceIdMissing() throws ServletException, IOException {
        request.addHeader(TraceIdFilter.CORRELATION_ID_HEADER, "correlation-999");

        doAnswer(invocation -> {
            assertEquals("correlation-999", MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY));
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        assertEquals("correlation-999", response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
    }

    @Test
    void testFallbackToRequestIdWhenBothTraceAndCorrelationMissing() throws ServletException, IOException {
        request.addHeader(TraceIdFilter.REQUEST_ID_HEADER, "req-xyz-789");

        doAnswer(invocation -> {
            assertEquals("req-xyz-789", MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY));
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        assertEquals("req-xyz-789", response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
    }

    @Test
    void testGeneratesUuidWhenNoTraceHeaderPresent() throws ServletException, IOException {
        doAnswer(invocation -> {
            String traceId = MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY);
            assertNotNull(traceId);
            assertFalse(traceId.trim().isEmpty());
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        String responseHeader = response.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        assertNotNull(responseHeader);
        assertFalse(responseHeader.trim().isEmpty());
        assertNull(MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY));
    }

    @Test
    void testSanitizesSpecialCharactersInHeader() throws ServletException, IOException {
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "trace-id!@#$%^&*()_123");

        doAnswer(invocation -> {
            assertEquals("trace-id_123", MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY));
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        assertEquals("trace-id_123", response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
    }

    @Test
    void testMdcCleanedUpEvenWhenFilterChainThrowsException() throws Exception {
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "error-trace");
        doThrow(new RuntimeException("Simulated filter failure")).when(filterChain).doFilter(request, response);

        assertThrows(RuntimeException.class, () -> filter.doFilter(request, response, filterChain));
        assertNull(MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY));
    }
}
