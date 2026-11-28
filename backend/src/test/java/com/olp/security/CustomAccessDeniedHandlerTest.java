package com.olp.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

class CustomAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void handle_writes403JsonWithProvidedMessage() throws Exception {
        CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("Admin privileges required"));

        assertEquals(403, response.getStatus());
        assertNotNull(response.getContentType());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        assertEquals("UTF-8", response.getCharacterEncoding());

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals(403, json.get("status").asInt());
        assertEquals("Admin privileges required", json.get("error").asText());
        assertEquals("Admin privileges required", json.get("message").asText());
        assertNotNull(json.get("timestamp"));
    }

    @Test
    void handle_usesFallbackMessageWhenExceptionMessageIsBlank() throws Exception {
        CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler(null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("   "));

        assertEquals(403, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Access denied: insufficient permissions to access this resource", json.get("error").asText());
    }

    @Test
    void handle_usesFallbackMessageWhenExceptionMessageIsNull() throws Exception {
        CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler(objectMapper);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException(null));

        assertEquals(403, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Access denied: insufficient permissions to access this resource", json.get("message").asText());
    }
}
