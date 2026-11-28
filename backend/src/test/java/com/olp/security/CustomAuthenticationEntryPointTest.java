package com.olp.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.junit.jupiter.api.Assertions.*;

class CustomAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void commence_writes401JsonWithProvidedMessage() throws Exception {
        CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest(), response,
                new BadCredentialsException("Expired JWT token"));

        assertEquals(401, response.getStatus());
        assertNotNull(response.getContentType());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        assertEquals("UTF-8", response.getCharacterEncoding());

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals(401, json.get("status").asInt());
        assertEquals("Expired JWT token", json.get("error").asText());
        assertEquals("Expired JWT token", json.get("message").asText());
        assertNotNull(json.get("timestamp"));
    }

    @Test
    void commence_usesFallbackMessageWhenAuthMessageIsBlankOrNull() throws Exception {
        CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint(null);

        MockHttpServletResponse blankResponse = new MockHttpServletResponse();
        entryPoint.commence(new MockHttpServletRequest(), blankResponse,
                new InsufficientAuthenticationException("   "));
        JsonNode blankJson = objectMapper.readTree(blankResponse.getContentAsString());
        assertEquals("Full authentication is required to access this resource", blankJson.get("error").asText());

        MockHttpServletResponse nullResponse = new MockHttpServletResponse();
        entryPoint.commence(new MockHttpServletRequest(), nullResponse,
                new InsufficientAuthenticationException(null));
        JsonNode nullJson = objectMapper.readTree(nullResponse.getContentAsString());
        assertEquals("Full authentication is required to access this resource", nullJson.get("message").asText());
    }
}
