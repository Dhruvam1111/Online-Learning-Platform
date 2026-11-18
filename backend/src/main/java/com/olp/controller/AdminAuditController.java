package com.olp.controller;

import com.olp.dto.AdminAuditLogResponse;
import com.olp.dto.PageResponse;
import com.olp.service.AdminAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Controller for administrative audit log queries (FR-A31, ADM-015).
 * Strictly read-only to preserve immutable audit trail.
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('admin')")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    @Autowired
    public AdminAuditController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    /**
     * Retrieve paginated and filterable admin audit log history.
     */
    @GetMapping
    public ResponseEntity<PageResponse<AdminAuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) Integer actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PageResponse<AdminAuditLogResponse> response = adminAuditService.getAuditLogs(
                actorId, action, targetType, startDate, endDate, search, page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }
}
