package com.olp.controller;

import com.olp.dto.TranscoderWebhookRequest;
import com.olp.dto.VideoUploadResponse;
import com.olp.entity.VideoUploadStatus;
import com.olp.service.VideoUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoTranscoderWebhookControllerTest {

    @Mock
    private VideoUploadService videoUploadService;

    @InjectMocks
    private VideoTranscoderWebhookController controller;

    private TranscoderWebhookRequest request;
    private VideoUploadResponse response;

    @BeforeEach
    void setUp() {
        request = new TranscoderWebhookRequest("upload-99", VideoUploadStatus.READY, "/hls/master.m3u8", "/dash/manifest.mpd", null);
        response = new VideoUploadResponse();
        response.setUploadId("upload-99");
        response.setStatus(VideoUploadStatus.READY);
        response.setStatusBadge("READY");
    }

    @Test
    void testHandleWebhook_Success_WithXWebhookSecret() {
        when(videoUploadService.processTranscoderWebhook("my-secret", request)).thenReturn(response);

        ResponseEntity<VideoUploadResponse> entity = controller.handleWebhook("my-secret", null, request);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertNotNull(entity.getBody());
        assertEquals("upload-99", entity.getBody().getUploadId());
        assertEquals("READY", entity.getBody().getStatusBadge());
        verify(videoUploadService, times(1)).processTranscoderWebhook("my-secret", request);
    }

    @Test
    void testHandleWebhook_Success_WithBearerAuthorization() {
        when(videoUploadService.processTranscoderWebhook("bearer-token", request)).thenReturn(response);

        ResponseEntity<VideoUploadResponse> entity = controller.handleWebhook(null, "Bearer bearer-token", request);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertNotNull(entity.getBody());
        verify(videoUploadService, times(1)).processTranscoderWebhook("bearer-token", request);
    }

    @Test
    void testHandleWebhook_Fails_InvalidSecret() {
        when(videoUploadService.processTranscoderWebhook("bad-secret", request))
                .thenThrow(new AccessDeniedException("Invalid or missing transcoder webhook secret"));

        assertThrows(AccessDeniedException.class, () ->
                controller.handleWebhook("bad-secret", null, request));
    }
}
