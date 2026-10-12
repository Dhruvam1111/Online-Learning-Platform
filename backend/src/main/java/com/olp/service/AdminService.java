package com.olp.service;

import com.olp.dto.AdminCourseResponse;
import com.olp.dto.AdminUserResponse;
import com.olp.dto.PageResponse;
import com.olp.dto.RejectCourseRequest;
import com.olp.entity.Course;
import com.olp.entity.CourseStatus;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseRepository;
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
    private final CourseRepository courseRepository;

    public AdminService(UserRepository userRepository, CourseRepository courseRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
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

    // =========================================================================
    // COURSE GOVERNANCE & MODERATION (Features 15, 16, 17)
    // =========================================================================

    /**
     * View master list of all courses with status and search filters.
     * FR-A09: Course approval and management directory
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminCourseResponse> getCourses(
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

        String trimmedStatus = (status != null && !status.isBlank()) ? status.trim().toLowerCase() : null;
        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;

        Page<Course> coursesPage = courseRepository.findCoursesWithFilters(trimmedStatus, trimmedSearch, pageable);
        return PageResponse.from(coursesPage.map(this::toAdminCourseResponse));
    }

    /**
     * List all courses currently awaiting admin approval (status = 'pending_review').
     * FR-A06: Review submitted courses prior to publishing
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminCourseResponse> getPendingCourses(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Course> pendingPage = courseRepository.findByStatus("pending_review", pageable);
        return PageResponse.from(pendingPage.map(this::toAdminCourseResponse));
    }

    /**
     * Approve a course, publishing it immediately for student catalog discovery.
     * FR-A07: Approve submitted course
     */
    @Transactional
    public AdminCourseResponse approveCourse(Integer courseId) {
        Course course = findCourseById(courseId);
        course.setStatus(CourseStatus.PUBLISHED);
        course.setRejectionReason(null);
        course = courseRepository.save(course);
        return toAdminCourseResponse(course);
    }

    /**
     * Reject a submitted course with structured feedback.
     * FR-A07: Reject submitted course with reasons provided
     */
    @Transactional
    public AdminCourseResponse rejectCourse(Integer courseId, RejectCourseRequest request) {
        Course course = findCourseById(courseId);
        course.setStatus(CourseStatus.REJECTED);
        course.setRejectionReason(request.getReason().trim());
        course = courseRepository.save(course);
        return toAdminCourseResponse(course);
    }

    /**
     * Unpublish an active course (reverts status to 'draft').
     * FR-A08: Unpublish live course
     */
    @Transactional
    public AdminCourseResponse unpublishCourse(Integer courseId) {
        Course course = findCourseById(courseId);
        course.setStatus(CourseStatus.DRAFT);
        course = courseRepository.save(course);
        return toAdminCourseResponse(course);
    }

    /**
     * Permanently remove a course from the platform (cascades lectures & enrollments).
     * FR-A08: Remove a live course
     */
    @Transactional
    public void deleteCourse(Integer courseId) {
        Course course = findCourseById(courseId);
        courseRepository.delete(course);
    }

    // ===== Helper Methods =====

    private User findUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private Course findCourseById(Integer courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
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

    private AdminCourseResponse toAdminCourseResponse(Course course) {
        User instructor = course.getInstructor();
        return new AdminCourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                instructor != null ? instructor.getId() : null,
                instructor != null ? instructor.getName() : null,
                instructor != null ? instructor.getEmail() : null,
                course.getStatus() != null ? course.getStatus().name().toLowerCase() : null,
                course.getRejectionReason(),
                course.getCreatedAt()
        );
    }
}
