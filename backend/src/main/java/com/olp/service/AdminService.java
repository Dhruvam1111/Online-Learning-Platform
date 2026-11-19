package com.olp.service;

import com.olp.dto.AssignCourseInstructorRequest;
import com.olp.dto.CourseInstructorResponse;
import com.olp.entity.CourseInstructor;
import com.olp.entity.CourseInstructorRole;
import com.olp.repository.CourseInstructorRepository;
import com.olp.dto.ApiResponse;
import com.olp.security.JwtUtil;
import com.olp.dto.AdminUpdateUserRequest;
import com.olp.dto.AdminCreateUserRequest;
import com.olp.exception.DuplicateEmailException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.olp.dto.AdminCourseResponse;
import com.olp.dto.AdminUserResponse;
import com.olp.dto.CourseReviewRequest;
import com.olp.dto.PageResponse;
import com.olp.dto.RejectCourseRequest;
import com.olp.entity.Course;
import com.olp.entity.CourseStatus;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseRepository;
import com.olp.repository.CourseSubmissionRepository;
import com.olp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseSubmissionRepository courseSubmissionRepository;
    private final EmailService emailService;
    private final AdminAuditService adminAuditService;
    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;
    public AdminService(UserRepository userRepository, CourseRepository courseRepository) {
        this(userRepository, courseRepository, null, null, null);
    }
    @Autowired(required = false)
    private JwtUtil jwtUtil;
    @Autowired(required = false)
private CourseInstructorRepository courseInstructorRepository;
    public AdminService(
            UserRepository userRepository,
            CourseRepository courseRepository,
            CourseSubmissionRepository courseSubmissionRepository,
            EmailService emailService) {
        this(userRepository, courseRepository, courseSubmissionRepository, emailService, null);
    }

    @Autowired
    public AdminService(
            UserRepository userRepository,
            CourseRepository courseRepository,
            @Autowired(required = false) CourseSubmissionRepository courseSubmissionRepository,
            @Autowired(required = false) EmailService emailService,
            @Autowired(required = false) AdminAuditService adminAuditService) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.courseSubmissionRepository = courseSubmissionRepository;
        this.emailService = emailService;
        this.adminAuditService = adminAuditService;
    }

    /**
     * Get all users with optional filtering by role, account status, and search
     * keyword.
     * FR-A02: Account and Access Management
     */

    /**
   * Create a new student or instructor account on behalf of an admin.
   * FR-A02 (ADM-021)
   */
  @Transactional
  public AdminUserResponse createUser(AdminCreateUserRequest request) {
      if (request == null) {
          throw new IllegalArgumentException("User creation request cannot be null");
      }
      String name = request.getName() != null ? request.getName().trim() : "";
      String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
      String role = request.getRole() != null ? request.getRole().trim().toLowerCase() : "";

      if (name.isBlank()) {
          throw new IllegalArgumentException("Name is required");
      }
      if (email.isBlank()) {
          throw new IllegalArgumentException("Email is required");
      }
      if (!"student".equals(role) && !"instructor".equals(role)) {
          throw new IllegalArgumentException("Role must be 'student' or 'instructor'");
      }
      if (userRepository.existsByEmail(email)) {
          throw new DuplicateEmailException("An account with this email already exists");
      }

      String rawPassword = (request.getPassword() != null && !request.getPassword().isBlank())
              ? request.getPassword()
              : generateTemporaryPassword();
      if (rawPassword.length() < 8) {
          throw new IllegalArgumentException("Password must be at least 8 characters");
      }

      PasswordEncoder encoder = (passwordEncoder != null) ? passwordEncoder : new BCryptPasswordEncoder();
      User user = new User(name, email, encoder.encode(rawPassword), role, "active");
      user = userRepository.save(user);

      if (adminAuditService != null) {
          adminAuditService.recordLog("USER_CREATED", "USER", String.valueOf(user.getId()),
                  user.getName(), "Admin created " + role + " account (" + email + ")", null, "active");
      }

      if (emailService != null) {
          try {
              emailService.sendAccountCreatedEmail(user.getEmail(), user.getName(), role, rawPassword);
          } catch (Exception e) {
              log.warn("Failed to send account creation email to {}: {}", user.getEmail(), e.getMessage());
          }
      }

      return toAdminUserResponse(user);
  }

  private String generateTemporaryPassword() {
      String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$";
      SecureRandom random = new SecureRandom();
      StringBuilder sb = new StringBuilder(12);
      for (int i = 0; i < 12; i++) {
          sb.append(chars.charAt(random.nextInt(chars.length())));
      }
      return sb.toString();
  }

    /**
   * Update allowed user details (name, email, role except admin promotion).
   * FR-A02 (ADM-022)
   */
  @Transactional
  public AdminUserResponse updateUser(Integer userId, AdminUpdateUserRequest request) {
      if (request == null) {
          throw new IllegalArgumentException("User update request cannot be null");
      }
      if (request.getName() == null && request.getEmail() == null && request.getRole() == null) {
          throw new IllegalArgumentException("At least one field (name, email, or role) must be provided");
      }

      User user = findUserById(userId);
      String beforeState = String.format("name=%s, email=%s, role=%s",
              user.getName(), user.getEmail(), user.getRole());

      if (request.getName() != null) {
          String trimmedName = request.getName().trim();
          if (trimmedName.isBlank()) {
              throw new IllegalArgumentException("Name cannot be blank");
          }
          user.setName(trimmedName);
      }

      if (request.getEmail() != null) {
          String normalizedEmail = request.getEmail().trim().toLowerCase();
          if (normalizedEmail.isBlank()) {
              throw new IllegalArgumentException("Email cannot be blank");
          }
          if (!normalizedEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(normalizedEmail)) {
              throw new DuplicateEmailException("An account with this email already exists");
          }
          user.setEmail(normalizedEmail);
      }

      if (request.getRole() != null) {
          String normalizedRole = request.getRole().trim().toLowerCase();
          if ("admin".equals(normalizedRole)) {
              throw new IllegalArgumentException("Cannot promote or assign admin role via this endpoint");
          }
          if (!"student".equals(normalizedRole) && !"instructor".equals(normalizedRole)) {
              throw new IllegalArgumentException("Role must be 'student' or 'instructor'");
          }
          if ("admin".equalsIgnoreCase(user.getRole())) {
              throw new IllegalArgumentException("Admin role cannot be modified via this endpoint");
          }
          user.setRole(normalizedRole);
      }

      user = userRepository.save(user);
      String afterState = String.format("name=%s, email=%s, role=%s",
              user.getName(), user.getEmail(), user.getRole());

      if (adminAuditService != null) {
          adminAuditService.recordLog("USER_UPDATED", "USER", String.valueOf(user.getId()),
                  user.getName(), "Updated user details", beforeState, afterState);
      }

      return toAdminUserResponse(user);
  }

    /**
   * Trigger a password reset / account recovery email for a user without exposing any password.
   * FR-A05 (ADM-023)
   */
  public ApiResponse triggerPasswordReset(Integer userId) {
      User user = findUserById(userId);
      if ("rejected".equalsIgnoreCase(user.getAccountStatus())) {
          throw new IllegalArgumentException("Cannot trigger password reset for a rejected account");
      }
      if (jwtUtil == null) {
          throw new IllegalStateException("JwtUtil is not configured");
      }

      String resetToken = jwtUtil.generatePasswordResetToken(user.getId(), 15 * 60 * 1000L);
      String resetLink = "http://localhost:8080/api/auth/password-reset/confirm?token=" + resetToken;

      if (emailService != null) {
          emailService.sendPasswordResetLinkEmail(user.getEmail(), user.getName(), resetLink, 15);
      }

      if (adminAuditService != null) {
          adminAuditService.recordLog("USER_PASSWORD_RESET_TRIGGERED", "USER", String.valueOf(user.getId()),
                  user.getName(), "Admin triggered password reset email for " + user.getEmail(), null, null);
      }

      return new ApiResponse(true, "Password reset email sent to " + user.getEmail());
  }
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

        String prevStatus = user.getAccountStatus();
        user.setAccountStatus("active");
        user = userRepository.save(user);

        if (adminAuditService != null) {
            adminAuditService.recordLog("INSTRUCTOR_APPROVED", "USER", String.valueOf(user.getId()),
                    user.getName(), "Approved instructor registration", prevStatus, "active");
        }
        if (emailService != null) {
          try {
              emailService.sendInstructorApprovalEmail(user.getEmail(), user.getName(), true);
          } catch (Exception e) {
              log.warn("Failed to send instructor approval email to {}: {}", user.getEmail(), e.getMessage());
          }
      }
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

        String prevStatus = user.getAccountStatus();
        user.setAccountStatus("rejected");
        user = userRepository.save(user);

        if (adminAuditService != null) {
            adminAuditService.recordLog("INSTRUCTOR_REJECTED", "USER", String.valueOf(user.getId()),
                    user.getName(), "Rejected instructor application", prevStatus, "rejected");
        }
        if (emailService != null) {
          try {
              emailService.sendInstructorApprovalEmail(user.getEmail(), user.getName(), false);
          } catch (Exception e) {
              log.warn("Failed to send instructor rejection email to {}: {}", user.getEmail(), e.getMessage());
          }
      }
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

        String prevStatus = user.getAccountStatus();
        user.setAccountStatus("suspended");
        user = userRepository.save(user);

        if (adminAuditService != null) {
            adminAuditService.recordLog("USER_SUSPENDED", "USER", String.valueOf(user.getId()),
                    user.getName(), "Suspended user account", prevStatus, "suspended");
        }

        return toAdminUserResponse(user);
    }

    /**
     * Reactivate a suspended user account.
     * FR-A02
     */
    @Transactional
    public AdminUserResponse activateUser(Integer userId) {
        User user = findUserById(userId);
        String prevStatus = user.getAccountStatus();
        if (!"suspended".equalsIgnoreCase(prevStatus)) {
            if ("pending".equalsIgnoreCase(prevStatus) || "rejected".equalsIgnoreCase(prevStatus)) {
                throw new IllegalArgumentException(
                        "Cannot activate a " + prevStatus.toLowerCase() + " account; only suspended accounts can be reactivated. Use instructor approval instead.");
            }
            throw new IllegalArgumentException(
                    "Only suspended accounts can be reactivated (current status: " + prevStatus + ")");
        }
        user.setAccountStatus("active");
        user = userRepository.save(user);

        if (adminAuditService != null) {
            adminAuditService.recordLog("USER_ACTIVATED", "USER", String.valueOf(user.getId()),
                    user.getName(), "Reactivated user account", prevStatus, "active");
        }

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

        String details = "Deleted user: " + user.getName() + " (" + user.getEmail() + ", role: " + user.getRole() + ")";
        String prevStatus = user.getAccountStatus();
        userRepository.delete(user);

        if (adminAuditService != null) {
            adminAuditService.recordLog("USER_DELETED", "USER", String.valueOf(userId),
                    user.getName(), details, prevStatus, "DELETED");
        }
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

        CourseStatus courseStatus = (status != null && !status.isBlank()) ? CourseStatus.fromString(status) : null;
        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;

        Page<Course> coursesPage = courseRepository.findCoursesWithFilters(courseStatus, trimmedSearch, pageable);
        return PageResponse.from(coursesPage.map(this::toAdminCourseResponse));
    }

    /**
     * List all courses currently awaiting admin approval (status =
     * 'pending_review').
     * FR-A06: Review submitted courses prior to publishing
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminCourseResponse> getPendingCourses(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Course> pendingPage = courseRepository.findByStatus(CourseStatus.PENDING_REVIEW, pageable);
        return PageResponse.from(pendingPage.map(this::toAdminCourseResponse));
    }

    /**
     * Approve a course, publishing it immediately for student catalog discovery.
     * Updates latest submission status to APPROVED, records reviewed timestamp,
     * and notifies instructor via email (FR-A07, FR-I71).
     */
    @Transactional
    public AdminCourseResponse approveCourse(Integer courseId) {
        Course course = findCourseById(courseId);
        requireCourseStatus(course, CourseStatus.PENDING_REVIEW, "approve");
        String prevStatus = course.getStatus() != null ? course.getStatus().name() : "UNKNOWN";
        course.setStatus(CourseStatus.PUBLISHED);
        course.setRejectionReason(null);
        course.setReviewComments(null);
        Course savedCourse = courseRepository.save(course);

        // Update latest course submission if present
        if (courseSubmissionRepository != null) {
            courseSubmissionRepository.findLatestByCourseId(courseId).ifPresent(sub -> {
                sub.setStatus("APPROVED");
                sub.setReviewedAt(LocalDateTime.now());
                courseSubmissionRepository.save(sub);
            });
        }

        // Notify instructor
        if (emailService != null && course.getInstructor() != null) {
            emailService.sendCourseReviewDecisionEmail(
                    course.getInstructor().getEmail(),
                    course.getInstructor().getName(),
                    course.getTitle(),
                    "APPROVED",
                    null);
        }

        if (adminAuditService != null) {
            adminAuditService.recordLog("COURSE_APPROVED", "COURSE", String.valueOf(course.getId()),
                    course.getTitle(), "Course approved and published", prevStatus, CourseStatus.PUBLISHED.name());
        }

        return toAdminCourseResponse(course);
    }

    /**
     * Reject a submitted course with structured feedback.
     * Records review comments, updates latest submission status to REJECTED,
     * and notifies instructor via email (FR-A07, FR-I71).
     */
    @Transactional
    public AdminCourseResponse rejectCourse(Integer courseId, RejectCourseRequest request) {
        Course course = findCourseById(courseId);
        requireCourseStatus(course, CourseStatus.PENDING_REVIEW, "reject");
        String prevStatus = course.getStatus() != null ? course.getStatus().name() : "UNKNOWN";
        String reason = (request != null && request.getReason() != null) ? request.getReason().trim() : "";
        if (reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason cannot be blank");
        }
        course.setStatus(CourseStatus.REJECTED);
        course.setRejectionReason(reason);
        course.setReviewComments(reason);
        Course savedCourse = courseRepository.save(course);

        // Update latest course submission if present
        if (courseSubmissionRepository != null) {
            courseSubmissionRepository.findLatestByCourseId(courseId).ifPresent(sub -> {
                sub.setStatus("REJECTED");
                sub.setReviewComments(reason);
                sub.setReviewedAt(LocalDateTime.now());
                courseSubmissionRepository.save(sub);
            });
        }

        // Notify instructor
        if (emailService != null && course.getInstructor() != null) {
            emailService.sendCourseReviewDecisionEmail(
                    course.getInstructor().getEmail(),
                    course.getInstructor().getName(),
                    course.getTitle(),
                    "REJECTED",
                    reason);
        }

        if (adminAuditService != null) {
            adminAuditService.recordLog("COURSE_REJECTED", "COURSE", String.valueOf(course.getId()),
                    course.getTitle(), "Course rejected: " + reason, prevStatus, CourseStatus.REJECTED.name());
        }

        return toAdminCourseResponse(course);
    }

    /**
     * Request changes on a submitted course with actionable feedback for the
     * instructor.
     * Transitions status to CHANGES_REQUESTED, records review feedback, updates
     * latest submission,
     * and notifies the instructor via email (FR-I71).
     */
    @Transactional
    public AdminCourseResponse requestChanges(Integer courseId, CourseReviewRequest request) {
        Course course = findCourseById(courseId);
        requireCourseStatus(course, CourseStatus.PENDING_REVIEW, "request changes on");
        String prevStatus = course.getStatus() != null ? course.getStatus().name() : "UNKNOWN";
        String feedback = (request != null) ? request.resolveFeedback() : "";
        if (feedback.isBlank()) {
            throw new IllegalArgumentException("Change request reason or comments must be provided");
        }

        course.setStatus(CourseStatus.CHANGES_REQUESTED);
        course.setRejectionReason(feedback);
        course.setReviewComments(feedback);
        course = courseRepository.save(course);

        // Update latest course submission if present
        if (courseSubmissionRepository != null) {
            courseSubmissionRepository.findLatestByCourseId(courseId).ifPresent(sub -> {
                sub.setStatus("CHANGES_REQUESTED");
                sub.setReviewComments(feedback);
                sub.setReviewedAt(LocalDateTime.now());
                courseSubmissionRepository.save(sub);
            });
        }

        // Notify instructor
        if (emailService != null && course.getInstructor() != null) {
            emailService.sendCourseReviewDecisionEmail(
                    course.getInstructor().getEmail(),
                    course.getInstructor().getName(),
                    course.getTitle(),
                    "CHANGES_REQUESTED",
                    feedback);
        }

        if (adminAuditService != null) {
            adminAuditService.recordLog("COURSE_CHANGES_REQUESTED", "COURSE", String.valueOf(course.getId()),
                    course.getTitle(), "Requested changes: " + feedback, prevStatus, CourseStatus.CHANGES_REQUESTED.name());
        }

        return toAdminCourseResponse(course);
    }

    /**
     * Unpublish an active course (reverts status to 'draft').
     * FR-A08: Unpublish live course
     */
    @Transactional
    public AdminCourseResponse unpublishCourse(Integer courseId) {
        Course course = findCourseById(courseId);
        requireCourseStatus(course, CourseStatus.PUBLISHED, "unpublish");
        String prevStatus = course.getStatus() != null ? course.getStatus().name() : "UNKNOWN";
        course.setStatus(CourseStatus.DRAFT);
        course = courseRepository.save(course);

        if (adminAuditService != null) {
            adminAuditService.recordLog("COURSE_UNPUBLISHED", "COURSE", String.valueOf(course.getId()),
                    course.getTitle(), "Course unpublished and returned to draft", prevStatus, CourseStatus.DRAFT.name());
        }

        return toAdminCourseResponse(course);
    }

    /**
     * Archive a published or draft course (hides from catalog, retains all data and enrollments).
     * FR-A08 (ADM-024)
     */
    @Transactional
    public AdminCourseResponse archiveCourse(Integer courseId) {
        Course course = findCourseById(courseId);
        CourseStatus current = course.getStatus();
        if (current != CourseStatus.PUBLISHED && current != CourseStatus.DRAFT) {
            String currentName = current != null ? current.name() : "UNKNOWN";
            throw new IllegalArgumentException(
                    "Cannot archive a course with status '" + currentName + "'. Only PUBLISHED or DRAFT courses can be archived.");
        }

        String prevStatus = current.name();
        course.setStatus(CourseStatus.ARCHIVED);
        course = courseRepository.save(course);

        if (adminAuditService != null) {
            adminAuditService.recordLog("COURSE_ARCHIVED", "COURSE", String.valueOf(course.getId()),
                    course.getTitle(), "Archived course (data and enrollments retained)", prevStatus, CourseStatus.ARCHIVED.name());
        }

        return toAdminCourseResponse(course);
    }
    /**
     * Permanently remove a course from the platform (cascades lectures &
     * enrollments).
     * FR-A08: Remove a live course
     */
    /**
   * Assign or reassign an active instructor to a course (OWNER or CO_INSTRUCTOR).
   * Notifies the previous owner when ownership is reassigned and audits the action.
   * FR-A10 (ADM-025)
   */
  @Transactional
  public CourseInstructorResponse assignCourseInstructor(Integer courseId, AssignCourseInstructorRequest request) {
      if (request == null || request.getInstructorId() == null) {
          throw new IllegalArgumentException("Instructor ID is required");
      }

      Course course = findCourseById(courseId);
      User newInstructor = findUserById(request.getInstructorId());

      if (!"instructor".equalsIgnoreCase(newInstructor.getRole())) {
          throw new IllegalArgumentException("Assigned user must have role 'instructor'");
      }
      if (!"active".equalsIgnoreCase(newInstructor.getAccountStatus())) {
          throw new IllegalArgumentException("Assigned instructor must have 'active' account status");
      }

      CourseInstructorRole assignedRole = (request.getRole() != null && !request.getRole().isBlank())
              ? CourseInstructorRole.fromString(request.getRole())
              : CourseInstructorRole.CO_INSTRUCTOR;

      String prevInstructorInfo = course.getInstructor() != null
              ? course.getInstructor().getId() + ":" + course.getInstructor().getEmail()
              : "none";

      if (assignedRole == CourseInstructorRole.OWNER) {
          User previousOwner = course.getInstructor();
          course.setInstructor(newInstructor);
          Course savedCourse = courseRepository.save(course);

          if (courseInstructorRepository != null) {
              if (previousOwner != null && !previousOwner.getId().equals(newInstructor.getId())) {
                  courseInstructorRepository.findByCourseIdAndInstructorId(courseId, previousOwner.getId())
                          .ifPresent(prev -> {
                              prev.setRole(CourseInstructorRole.CO_INSTRUCTOR);
                              courseInstructorRepository.save(prev);
                          });
              }
              CourseInstructor mapping = courseInstructorRepository
                      .findByCourseIdAndInstructorId(courseId, newInstructor.getId())
                      .orElseGet(() -> new CourseInstructor(savedCourse, newInstructor, CourseInstructorRole.OWNER));
              mapping.setRole(CourseInstructorRole.OWNER);
              courseInstructorRepository.save(mapping);
          }

          if (emailService != null && previousOwner != null && !previousOwner.getId().equals(newInstructor.getId())) {
              try {
                  emailService.sendCourseInstructorChangeEmail(
                          previousOwner.getEmail(),
                          previousOwner.getName(),
                          course.getTitle(),
                          "REASSIGNED",
                          "Primary ownership of this course has been reassigned to " + newInstructor.getName() + ".");
              } catch (Exception e) {
                  log.warn("Failed to notify previous instructor {}: {}", previousOwner.getEmail(), e.getMessage());
              }
          }
      } else {
          if (courseInstructorRepository != null) {
              if (courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, newInstructor.getId())) {
                  throw new IllegalStateException("Instructor is already assigned to this course");
              }
              courseInstructorRepository.save(new CourseInstructor(course, newInstructor, CourseInstructorRole.CO_INSTRUCTOR));
          }
      }

      if (emailService != null) {
          try {
              emailService.sendCourseInstructorChangeEmail(
                      newInstructor.getEmail(),
                      newInstructor.getName(),
                      course.getTitle(),
                      "ASSIGNED",
                      "You have been assigned as " + assignedRole.name() + " for this course.");
          } catch (Exception e) {
              log.warn("Failed to notify assigned instructor {}: {}", newInstructor.getEmail(), e.getMessage());
          }
      }

      if (adminAuditService != null) {
          adminAuditService.recordLog(
                  "COURSE_INSTRUCTOR_ASSIGNED",
                  "COURSE",
                  String.valueOf(course.getId()),
                  course.getTitle(),
                  "Assigned instructor " + newInstructor.getEmail() + " as " + assignedRole.name(),
                  prevInstructorInfo,
                  newInstructor.getId() + ":" + newInstructor.getEmail() + " (" + assignedRole.name() + ")");
      }

      return CourseInstructorResponse.fromUser(newInstructor, assignedRole, null, LocalDateTime.now());
  }

  /**
   * Remove a co-instructor from a course and notify them.
   * FR-A10 (ADM-025)
   */
  @Transactional
  public void removeCourseInstructor(Integer courseId, Integer instructorId) {
      Course course = findCourseById(courseId);
      User instructor = findUserById(instructorId);

      if (course.getInstructor() != null && course.getInstructor().getId().equals(instructorId)) {
          throw new IllegalArgumentException("Cannot remove the primary course owner; reassign course ownership first");
      }

      if (courseInstructorRepository != null) {
          CourseInstructor mapping = courseInstructorRepository
                  .findByCourseIdAndInstructorId(courseId, instructorId)
                  .orElseThrow(() -> new ResourceNotFoundException("Instructor is not assigned to this course"));
          courseInstructorRepository.delete(mapping);
      }

      if (emailService != null) {
          try {
              emailService.sendCourseInstructorChangeEmail(
                      instructor.getEmail(),
                      instructor.getName(),
                      course.getTitle(),
                      "REMOVED",
                      "You have been removed as an instructor from this course.");
          } catch (Exception e) {
              log.warn("Failed to notify removed instructor {}: {}", instructor.getEmail(), e.getMessage());
          }
      }

      if (adminAuditService != null) {
          adminAuditService.recordLog(
                  "COURSE_INSTRUCTOR_REMOVED",
                  "COURSE",
                  String.valueOf(course.getId()),
                  course.getTitle(),
                  "Removed instructor " + instructor.getEmail(),
                  instructor.getId() + ":" + instructor.getEmail(),
                  null);
      }
  }
    @Transactional
    public void deleteCourse(Integer courseId) {
        Course course = findCourseById(courseId);
        String details = "Deleted course: " + course.getTitle() + " (ID: " + courseId + ")";
        String prevStatus = course.getStatus() != null ? course.getStatus().name() : "";
        courseRepository.delete(course);

        if (adminAuditService != null) {
            adminAuditService.recordLog("COURSE_DELETED", "COURSE", String.valueOf(courseId),
                    course.getTitle(), details, prevStatus, "DELETED");
        }
    }

    // ===== Helper Methods =====

    private User findUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
    /**
 * Enforces allowed course status transitions for administrative actions:
 * - approve / reject / requestChanges: only from PENDING_REVIEW
 * - unpublish: only from PUBLISHED
 */
private void requireCourseStatus(Course course, CourseStatus expected, String action) {
    if (course.getStatus() != expected) {
        String current = course.getStatus() != null ? course.getStatus().name() : "UNKNOWN";
        throw new IllegalArgumentException(
                "Cannot " + action + " a course with status '" + current + "'. Expected status: " + expected.name());
    }
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
                user.getCreatedAt());
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
                course.getCreatedAt());
    }
}
