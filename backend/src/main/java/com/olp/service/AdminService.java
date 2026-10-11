package com.olp.service;

import com.olp.dto.AdminUserResponse;
import com.olp.dto.PageResponse;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get all users with optional filtering by role, account status, and search keyword.
     * FR-A02: Account and Access Management
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getUsers(
            String role,
            String status,
            String search,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Sort sort = "desc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        String trimmedRole = (role != null && !role.isBlank()) ? role.trim().toLowerCase() : null;
        String trimmedStatus = (status != null && !status.isBlank()) ? status.trim().toLowerCase() : null;
        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;

        Page<User> usersPage = userRepository.findUsersWithFilters(trimmedRole, trimmedStatus, trimmedSearch, pageable);

        Page<AdminUserResponse> dtoPage = usersPage.map(this::toAdminUserResponse);
        return PageResponse.from(dtoPage);
    }

    /**
     * List all instructors currently awaiting admin approval.
     * FR-A03: Verify and approve new instructor registrations
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getPendingInstructors(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> pendingPage = userRepository.findByRoleAndAccountStatus("instructor", "pending", pageable);
        return PageResponse.from(pendingPage.map(this::toAdminUserResponse));
    }

    /**
     * Approve a pending instructor, granting them active course-creation access.
     * FR-A03, FR-I01
     */
    @Transactional
    public AdminUserResponse approveInstructor(Integer userId) {
        User user = findUserById(userId);

        if (!"instructor".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("Only users with role 'instructor' can be approved as instructors");
        }

        user.setAccountStatus("active");
        user = userRepository.save(user);
        return toAdminUserResponse(user);
    }

    /**
     * Reject an instructor applicant.
     * FR-A03
     */
    @Transactional
    public AdminUserResponse rejectInstructor(Integer userId) {
        User user = findUserById(userId);

        if (!"instructor".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("Only users with role 'instructor' can be rejected as instructors");
        }

        user.setAccountStatus("rejected");
        user = userRepository.save(user);
        return toAdminUserResponse(user);
    }

    /**
     * Suspend any user account (student or instructor).
     * FR-A02
     */
    @Transactional
    public AdminUserResponse suspendUser(Integer userId) {
        User user = findUserById(userId);

        if ("admin".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("Admin accounts cannot be suspended");
        }

        user.setAccountStatus("suspended");
        user = userRepository.save(user);
        return toAdminUserResponse(user);
    }

    /**
     * Reactivate a suspended user account.
     * FR-A02
     */
    @Transactional
    public AdminUserResponse activateUser(Integer userId) {
        User user = findUserById(userId);
        user.setAccountStatus("active");
        user = userRepository.save(user);
        return toAdminUserResponse(user);
    }

    /**
     * Permanently delete a user account.
     * FR-A02
     */
    @Transactional
    public void deleteUser(Integer userId) {
        User user = findUserById(userId);

        if ("admin".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("Admin accounts cannot be deleted");
        }

        userRepository.delete(user);
    }

    // ===== Helper Methods =====

    private User findUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getAccountStatus(),
                user.getCreatedAt()
        );
    }
}
