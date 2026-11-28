package com.olp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_TRACE_ID_KEY = "traceId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String traceId = resolveTraceId(request);
        MDC.put(MDC_TRACE_ID_KEY, traceId);
        request.setAttribute(MDC_TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID_KEY);
        }
    }

    protected String resolveTraceId(HttpServletRequest request) {
        String header = request.getHeader(TRACE_ID_HEADER);
        if (header == null || header.trim().isEmpty()) {
            header = request.getHeader(CORRELATION_ID_HEADER);
        }
        if (header == null || header.trim().isEmpty()) {
            header = request.getHeader(REQUEST_ID_HEADER);
        }

        if (header != null && !header.trim().isEmpty()) {
            String sanitized = header.trim().replaceAll("[^a-zA-Z0-9-_]", "");
            if (!sanitized.isEmpty() && sanitized.length() <= 64) {
                return sanitized;
            }
        }
        return UUID.randomUUID().toString();
    }
}
