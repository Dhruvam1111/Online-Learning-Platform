package com.olp.controller;

import com.olp.dto.InvoiceResponse;
import com.olp.entity.User;
import com.olp.service.InvoiceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller serving GST Tax Invoices and receipts (NFR-23, NFR-33).
 * Owned by Dhruvam (Invoice Engine).
 */
@RestController
@RequestMapping("/api/payments/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    /**
     * Returns a styled printable HTML tax receipt with GST and company registration details.
     */
    @GetMapping(value = {"/{orderId}/print", "/{orderId}/print/"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getPrintableInvoice(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String orderId) {
        String cleanId = orderId != null ? orderId.trim() : "";
        String html = invoiceService.generateInvoiceHtml(currentUser, cleanId);
        return ResponseEntity.ok(html);
    }

    /**
     * Returns structured JSON tax invoice metadata for API clients.
     */
    @GetMapping(value = {"/{orderId}", "/{orderId}/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InvoiceResponse> getInvoice(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String orderId) {
        String cleanId = orderId != null ? orderId.trim() : "";
        InvoiceResponse response = invoiceService.getInvoiceData(currentUser, cleanId);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns styled HTML receipt when accessed via browser.
     */
    @GetMapping(value = {"/{orderId}", "/{orderId}/"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getInvoiceHtmlDirect(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String orderId) {
        String cleanId = orderId != null ? orderId.trim() : "";
        String html = invoiceService.generateInvoiceHtml(currentUser, cleanId);
        return ResponseEntity.ok(html);
    }

    /**
     * Directly downloads the tax invoice receipt as an HTML file attachment to disk.
     */
    @GetMapping(value = {"/{orderId}/download", "/{orderId}/download/"})
    public ResponseEntity<byte[]> downloadInvoice(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String orderId) {
        String cleanId = orderId != null ? orderId.trim() : "";
        String html = invoiceService.generateInvoiceHtml(currentUser, cleanId);
        InvoiceResponse inv = invoiceService.getInvoiceData(currentUser, cleanId);
        String filename = "Receipt-" + inv.getInvoiceNumber() + ".html";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                .header("X-Content-Type-Options", "nosniff")
                .body(html.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
