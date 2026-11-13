package com.olp.controller;

import com.olp.dto.TranscoderWebhookRequest;
import com.olp.dto.VideoUploadResponse;
import com.olp.service.VideoUploadService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller receiving asynchronous webhook callbacks from external video transcoding pipeline (FR-I15, Step 18).
 */
@RestController
@RequestMapping({"/api/webhooks/transcoder", "/api/videos/webhook/transcode"})
public class VideoTranscoderWebhookController {

    private final VideoUploadService videoUploadService;

    @Autowired
    public VideoTranscoderWebhookController(VideoUploadService videoUploadService) {
        this.videoUploadService = videoUploadService;
    }

    /**
     * POST /api/webhooks/transcoder
     * Ingests transcoding completion or failure updates with secret token authentication and idempotency.
     */
    @PostMapping
    public ResponseEntity<VideoUploadResponse> handleWebhook(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String webhookSecret,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody TranscoderWebhookRequest request) {

        String secret = (webhookSecret != null && !webhookSecret.isBlank()) ? webhookSecret : authHeader;
        if (secret != null && secret.startsWith("Bearer ")) {
            secret = secret.substring(7);
        }

        VideoUploadResponse response = videoUploadService.processTranscoderWebhook(secret, request);
        return ResponseEntity.ok(response);
    }
}
