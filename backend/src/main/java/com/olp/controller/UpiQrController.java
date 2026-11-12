package com.olp.controller;

import com.olp.dto.UpiQrResponse;
import com.olp.service.UpiQrCodeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller exposing dynamic NPCI UPI QR code generation, raw visual PNG streams,
 * and the interactive Scan & Pay checkout page (FR-PG01, FR-PG03).
 * Owned by Shubh (Digital Payment Instruments).
 */
@RestController
@RequestMapping("/api/payments/upi-qr")
public class UpiQrController {

    private final UpiQrCodeService upiQrCodeService;

    public UpiQrController(UpiQrCodeService upiQrCodeService) {
        this.upiQrCodeService = upiQrCodeService;
    }

    /**
     * Returns structured JSON payload with Base64 QR code and deep-link UPI URI.
     */
    @org.springframework.security.access.prepost.PreAuthorize("@paymentAccess.owns(authentication, #orderId)")
    @GetMapping("/{orderId}")
    public ResponseEntity<UpiQrResponse> getUpiQrDetails(@PathVariable String orderId) {
        UpiQrResponse response = upiQrCodeService.generateUpiQr(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Serves raw PNG image of the UPI QR code for direct visual display in browsers and apps.
     */
    @org.springframework.security.access.prepost.PreAuthorize("@paymentAccess.owns(authentication, #orderId)")
    @GetMapping(value = "/{orderId}/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrImage(@PathVariable String orderId) {
        byte[] imageBytes = upiQrCodeService.getQrPngBytes(orderId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"upi-qr-" + orderId + ".png\"")
                .body(imageBytes);
    }

    /**
     * Serves the full interactive "Scan & Pay" HTML checkout portal with live polling.
     */
    @org.springframework.security.access.prepost.PreAuthorize("@paymentAccess.owns(authentication, #orderId)")
    @GetMapping(value = "/{orderId}/portal", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getScanAndPayPortal(@PathVariable String orderId) {
        String html = upiQrCodeService.generateScanAndPayPortalHtml(orderId);
        return ResponseEntity.ok(html);
    }
}
