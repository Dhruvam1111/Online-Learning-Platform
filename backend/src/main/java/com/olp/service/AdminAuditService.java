package com.olp.service;

import com.olp.dto.AdminAuditLogResponse;
import com.olp.dto.PageResponse;
import com.olp.entity.AdminAuditLog;
import com.olp.entity.User;
import com.olp.repository.AdminAuditLogRepository;
import com.olp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for immutable tracking and retrieval of administrative audit logs (FR-A31, ADM-015).
 * Enforces append-only governance: no update or delete operations are provided.
 */
@Service
public class AdminAuditService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditService.class);

    private final AdminAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Autowired
    public AdminAuditService(AdminAuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Record an audit log entry explicitly specifying the actor.
     */
    @Transactional
    public AdminAuditLog recordLog(User actor, String action, String targetType, String targetId,
                                   String targetName, String details, String previousState,
                                   String newState, String ipAddress) {
        User effectiveActor = actor != null ? actor : resolveCurrentAdminUser().orElse(null);

        if (effectiveActor == null) {
            log.warn("Recording audit log for action '{}' without authenticated actor. Looking up default admin.", action);
            effectiveActor = userRepository.findAll().stream()
                    .filter(u -> "admin".equalsIgnoreCase(u.getRole()))
                    .findFirst()
                    .orElse(null);
        }

        if (effectiveActor == null) {
            log.error("Unable to resolve admin actor for audit log. Action: {}", action);
            return null;
        }

        AdminAuditLog auditLog = new AdminAuditLog(
                effectiveActor,
                action != null ? action.trim().toUpperCase() : "UNKNOWN",
                targetType != null ? targetType.trim().toUpperCase() : "GENERAL",
                targetId != null ? targetId : "",
                targetName,
                details,
                previousState,
                newState,
                ipAddress
        );

        AdminAuditLog saved = auditLogRepository.save(auditLog);
        log.info("Admin audit event logged: id={}, action={}, targetType={}, targetId={}, actor={}",
                saved.getId(), saved.getAction(), saved.getTargetType(), saved.getTargetId(), effectiveActor.getEmail());
        return saved;
    }

    /**
     * Record an audit log entry, resolving the acting administrator from the current security context.
     */
    @Transactional
    public AdminAuditLog recordLog(String action, String targetType, String targetId,
                                   String targetName, String details, String previousState,
                                   String newState) {
        return recordLog(null, action, targetType, targetId, targetName, details, previousState, newState, null);
    }

    /**
     * Retrieve audit log entries with multi-attribute filtering, sorting, and pagination.
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminAuditLogResponse> getAuditLogs(
            Integer actorId,
            String action,
            String targetType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String search,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        String validSortBy = (sortBy != null && !sortBy.isBlank()) ? sortBy.trim() : "createdAt";
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(validSortBy).ascending()
                : Sort.by(validSortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        String trimmedAction = (action != null && !action.isBlank()) ? action.trim() : null;
        String trimmedTargetType = (targetType != null && !targetType.isBlank()) ? targetType.trim() : null;
        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<AdminAuditLog> pageResult = auditLogRepository.findWithFilters(
                actorId,
                trimmedAction,
                trimmedTargetType,
                startDate,
                endDate,
                trimmedSearch,
                pageable
        );

        return PageResponse.from(pageResult.map(AdminAuditLogResponse::fromEntity));
    }

    /**
     * Helper to resolve current authenticated user from SecurityContext.
     */
    public Optional<User> resolveCurrentAdminUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return Optional.of(user);
        }

        String username = auth.getName();
        if (username != null && !username.isBlank()) {
            return userRepository.findByEmail(username);
        }

        return Optional.empty();
    }
}
