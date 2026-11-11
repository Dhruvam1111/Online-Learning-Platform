package com.olp.service;

import com.olp.dto.AvailableCourseAssociationResponse;
import com.olp.dto.CourseAssociationRequestDto;
import com.olp.dto.CourseAssociationRequestResponse;
import com.olp.dto.CourseAssociationReviewRequest;
import com.olp.dto.CourseInstructorResponse;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseAssociationRequestRepository;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.InstructorProfileRepository;
import com.olp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CourseAssociationService {

    private final CourseAssociationRequestRepository requestRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final InstructorProfileRepository instructorProfileRepository;

    public CourseAssociationService(
            CourseAssociationRequestRepository requestRepository,
            CourseInstructorRepository courseInstructorRepository,
            CourseRepository courseRepository,
            UserRepository userRepository) {
        this(requestRepository, courseInstructorRepository, courseRepository, userRepository, null, null);
    }

    public CourseAssociationService(
            CourseAssociationRequestRepository requestRepository,
            CourseInstructorRepository courseInstructorRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            EmailService emailService) {
        this(requestRepository, courseInstructorRepository, courseRepository, userRepository, emailService, null);
    }

    @Autowired
    public CourseAssociationService(
            CourseAssociationRequestRepository requestRepository,
            CourseInstructorRepository courseInstructorRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            @Autowired(required = false) EmailService emailService,
            @Autowired(required = false) InstructorProfileRepository instructorProfileRepository) {
        this.requestRepository = requestRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.instructorProfileRepository = instructorProfileRepository;
    }

    /**
     * Resolves the authenticated User entity from Spring Security Authentication.
     */
    public User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
    }

    /**
     * FR-I58 — Search and view existing published courses available for instructor association.
     */
    @Transactional(readOnly = true)
    public List<AvailableCourseAssociationResponse> searchAvailableCourses(
            User currentUser, String search, String category) {
        validateActiveInstructor(currentUser);

        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        List<Course> courses = (trimmedSearch == null)
                ? courseRepository.findByCourseStatusOrderByIdAsc(CourseStatus.PUBLISHED)
                : courseRepository.searchByCourseStatusAndKeyword(CourseStatus.PUBLISHED, trimmedSearch);

        String trimmedCategory = (category != null && !category.trim().isEmpty()) ? category.trim() : null;

        Set<Integer> associatedCourseIds = new HashSet<>();
        List<CourseInstructor> associations = courseInstructorRepository.findByInstructorId(currentUser.getId());
        if (associations != null) {
            for (CourseInstructor ci : associations) {
                if (ci != null && ci.getCourse() != null && ci.getCourse().getId() != null) {
                    associatedCourseIds.add(ci.getCourse().getId());
                }
            }
        }

        // Latest request per course (findByInstructorIdOrderByCreatedAtDesc returns newest first)
        Map<Integer, CourseAssociationRequest> latestRequestByCourse = new HashMap<>();
        List<CourseAssociationRequest> myRequests = requestRepository.findByInstructorIdOrderByCreatedAtDesc(currentUser.getId());
        if (myRequests != null) {
            for (CourseAssociationRequest req : myRequests) {
                if (req != null && req.getCourse() != null && req.getCourse().getId() != null) {
                    latestRequestByCourse.putIfAbsent(req.getCourse().getId(), req);
                }
            }
        }

        List<AvailableCourseAssociationResponse> results = new ArrayList<>();
        for (Course course : courses) {
            if (trimmedCategory != null
                    && (course.getCategory() == null || !course.getCategory().equalsIgnoreCase(trimmedCategory))) {
                continue;
            }

            boolean isOwner = course.getInstructor() != null
                    && currentUser.getId().equals(course.getInstructor().getId());
            boolean isCoInstructor = associatedCourseIds.contains(course.getId());
            boolean alreadyAssociated = isOwner || isCoInstructor;

            CourseAssociationRequest latestReq = latestRequestByCourse.get(course.getId());
            String associationStatus;
            Integer activeRequestId = null;

            if (isOwner) {
                associationStatus = "OWNER";
            } else if (isCoInstructor) {
                associationStatus = "ASSOCIATED";
            } else if (latestReq != null) {
                associationStatus = latestReq.getStatus().name();
                if (latestReq.getStatus() == AssociationRequestStatus.PENDING) {
                    activeRequestId = latestReq.getId();
                }
            } else {
                associationStatus = "AVAILABLE";
            }

            results.add(new AvailableCourseAssociationResponse(
                    course.getId(),
                    course.getTitle(),
                    course.getDescription(),
                    course.getCategory(),
                    course.getStatus() != null ? course.getStatus().name() : null,
                    course.getInstructor() != null ? course.getInstructor().getId() : null,
                    course.getInstructor() != null ? course.getInstructor().getName() : null,
                    alreadyAssociated,
                    associationStatus,
                    activeRequestId
            ));
        }

        return results;
    }

    /**
     * FR-I58 — Submit a request to join an existing course as a co-instructor (stored as PENDING).
     */
    @Transactional
    public CourseAssociationRequestResponse submitAssociationRequest(
            User currentUser,
            Integer pathCourseId,
            CourseAssociationRequestDto requestDto) {
        validateActiveInstructor(currentUser);

        Integer bodyCourseId = requestDto != null ? requestDto.getCourseId() : null;
        if (pathCourseId != null && bodyCourseId != null && !pathCourseId.equals(bodyCourseId)) {
            throw new IllegalArgumentException("Course ID in path and body do not match");
        }
        Integer targetCourseId = pathCourseId != null ? pathCourseId : bodyCourseId;
        if (targetCourseId == null || targetCourseId <= 0) {
            throw new IllegalArgumentException("Valid courseId is required");
        }

        String requestedRole = "co-instructor";
        if (requestDto != null && requestDto.getRequestedRole() != null
                && !requestDto.getRequestedRole().trim().isEmpty()) {
            CourseInstructorRole parsedRole = CourseInstructorRole.fromString(requestDto.getRequestedRole());
            if (parsedRole != CourseInstructorRole.CO_INSTRUCTOR) {
                throw new IllegalArgumentException("Requested role must be 'co-instructor' (CO_INSTRUCTOR)");
            }
            requestedRole = parsedRole.toDatabaseValue();
        }

        String cleanMessage = null;
        if (requestDto != null && requestDto.getMessage() != null) {
            String trimmed = requestDto.getMessage().trim();
            if (trimmed.length() > 1000) {
                throw new IllegalArgumentException("Request message must be 1000 characters or fewer");
            }
            cleanMessage = trimmed.isEmpty() ? null : trimmed;
        }

        Course course = courseRepository.findById(targetCourseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + targetCourseId));

        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot request association with an archived course");
        }

        // Course creators are auto-associated; prevent self-requesting or duplicate association
        if (course.getInstructor() != null && currentUser.getId().equals(course.getInstructor().getId())) {
            throw new IllegalStateException("You are already the creator and owner of this course");
        }

        if (courseInstructorRepository.existsByCourseIdAndInstructorId(targetCourseId, currentUser.getId())) {
            throw new IllegalStateException("You are already associated with this course");
        }

        if (requestRepository.existsByCourseIdAndInstructorIdAndStatus(
                targetCourseId, currentUser.getId(), AssociationRequestStatus.PENDING)) {
            throw new IllegalStateException("A pending association request already exists for this course");
        }

        CourseAssociationRequest associationRequest = new CourseAssociationRequest(
                course, currentUser, requestedRole, cleanMessage);

        CourseAssociationRequest saved = requestRepository.save(associationRequest);
        return CourseAssociationRequestResponse.fromEntity(saved);
    }

    /**
     * FR-I58 — List association requests submitted by the authenticated instructor.
     */
    @Transactional(readOnly = true)
    public List<CourseAssociationRequestResponse> getMyRequests(User currentUser, String statusFilter) {
        validateActiveInstructor(currentUser);

        List<CourseAssociationRequest> requests;
        if (statusFilter != null && !statusFilter.trim().isEmpty()) {
            AssociationRequestStatus status = AssociationRequestStatus.fromString(statusFilter);
            requests = requestRepository.findByInstructorIdAndStatusOrderByCreatedAtDesc(currentUser.getId(), status);
        } else {
            requests = requestRepository.findByInstructorIdOrderByCreatedAtDesc(currentUser.getId());
        }

        return requests.stream()
                .map(CourseAssociationRequestResponse::fromEntity)
                .toList();
    }

    /**
     * FR-I58 — View a single association request owned by the authenticated instructor.
     */
    @Transactional(readOnly = true)
    public CourseAssociationRequestResponse getMyRequestById(User currentUser, Integer requestId) {
        validateActiveInstructor(currentUser);
        CourseAssociationRequest req = findRequestOwnedByInstructor(requestId, currentUser);
        return CourseAssociationRequestResponse.fromEntity(req);
    }

    /**
     * FR-I58 — Cancel a PENDING association request owned by the authenticated instructor.
     */
    @Transactional
    public void cancelMyRequest(User currentUser, Integer requestId) {
        validateActiveInstructor(currentUser);
        CourseAssociationRequest req = findRequestOwnedByInstructor(requestId, currentUser);

        if (req.getStatus() != AssociationRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending association requests can be cancelled");
        }

        requestRepository.delete(req);
    }

    /**
     * FR-I58 — Admin lists all association requests, optionally filtered by status.
     */
    @Transactional(readOnly = true)
    public List<CourseAssociationRequestResponse> getRequestsForAdmin(User adminUser, String statusFilter) {
        validateAdmin(adminUser);

        List<CourseAssociationRequest> requests;
        if (statusFilter != null && !statusFilter.trim().isEmpty()) {
            AssociationRequestStatus status = AssociationRequestStatus.fromString(statusFilter);
            requests = requestRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            requests = requestRepository.findAllWithDetailsOrderByCreatedAtDesc();
        }

        return requests.stream()
                .map(CourseAssociationRequestResponse::fromEntity)
                .toList();
    }

    /**
     * FR-I58 — Admin approves an association request, creates CourseInstructor association, and notifies instructor.
     */
    @Transactional
    public CourseAssociationRequestResponse approveRequest(
            User adminUser,
            Integer requestId,
            CourseAssociationReviewRequest reviewRequest) {
        validateAdmin(adminUser);

        CourseAssociationRequest req = findRequestOrThrow(requestId);
        if (req.getStatus() != AssociationRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Association request has already been " + req.getStatus().name().toLowerCase());
        }

        Course course = req.getCourse();
        User instructor = req.getInstructor();

        // Reuse existing course_instructors mapping; skip insert if already associated
        boolean alreadyOwner = course.getInstructor() != null
                && instructor.getId().equals(course.getInstructor().getId());
        if (!alreadyOwner && !courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), instructor.getId())) {
            CourseInstructorRole assignedRole = (req.getRequestedRole() != null && !req.getRequestedRole().isBlank())
                    ? CourseInstructorRole.fromString(req.getRequestedRole())
                    : CourseInstructorRole.CO_INSTRUCTOR;
            CourseInstructor association = new CourseInstructor(course, instructor, assignedRole);
            courseInstructorRepository.save(association);
        }

        req.setStatus(AssociationRequestStatus.APPROVED);
        req.setReviewedBy(adminUser);
        req.setReviewedAt(LocalDateTime.now());
        req.setAdminNotes(reviewRequest != null ? reviewRequest.resolveNotes() : null);

        CourseAssociationRequest saved = requestRepository.save(req);

        notifyInstructor(instructor, course, AssociationRequestStatus.APPROVED, saved.getAdminNotes());
        return CourseAssociationRequestResponse.fromEntity(saved);
    }

    /**
     * FR-I58 — Admin rejects an association request and notifies instructor.
     */
    @Transactional
    public CourseAssociationRequestResponse rejectRequest(
            User adminUser,
            Integer requestId,
            CourseAssociationReviewRequest reviewRequest) {
        validateAdmin(adminUser);

        CourseAssociationRequest req = findRequestOrThrow(requestId);
        if (req.getStatus() != AssociationRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Association request has already been " + req.getStatus().name().toLowerCase());
        }

        req.setStatus(AssociationRequestStatus.REJECTED);
        req.setReviewedBy(adminUser);
        req.setReviewedAt(LocalDateTime.now());
        req.setAdminNotes(reviewRequest != null ? reviewRequest.resolveNotes() : null);

        CourseAssociationRequest saved = requestRepository.save(req);

        notifyInstructor(req.getInstructor(), req.getCourse(), AssociationRequestStatus.REJECTED, saved.getAdminNotes());
        return CourseAssociationRequestResponse.fromEntity(saved);
    }

    /**
     * FR-I59 — Admin retrieves all instructors associated with a course, including roles and basic profile info.
     */
    @Transactional(readOnly = true)
    public List<CourseInstructorResponse> getCourseInstructorsForAdmin(User adminUser, Integer courseId) {
        validateAdmin(adminUser);
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        return CourseInstructorResponse.resolveInstructorsForCourse(
                course, courseInstructorRepository, instructorProfileRepository);
    }

    private void notifyInstructor(User instructor, Course course, AssociationRequestStatus decision, String notes) {
        if (emailService == null || instructor == null || instructor.getEmail() == null) {
            return;
        }
        String courseTitle = course != null ? course.getTitle() : "Course";
        emailService.sendCourseAssociationDecisionEmail(
                instructor.getEmail(),
                instructor.getName(),
                courseTitle,
                decision,
                notes
        );
    }

    private CourseAssociationRequest findRequestOrThrow(Integer requestId) {
        if (requestId == null) {
            throw new IllegalArgumentException("Association request ID cannot be null");
        }
        return requestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Course association request not found with id: " + requestId));
    }

    private CourseAssociationRequest findRequestOwnedByInstructor(Integer requestId, User currentUser) {
        CourseAssociationRequest req = findRequestOrThrow(requestId);
        if (req.getInstructor() == null || !currentUser.getId().equals(req.getInstructor().getId())) {
            throw new AccessDeniedException("Cannot access or manage another instructor's association request");
        }
        return req;
    }

    private void validateActiveInstructor(User currentUser) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (!"instructor".equalsIgnoreCase(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied: only instructors can manage course association requests");
        }
        if (!"active".equalsIgnoreCase(currentUser.getAccountStatus())) {
            throw new AccessDeniedException("Access denied: Instructor account is awaiting admin approval");
        }
    }

    private void validateAdmin(User adminUser) {
        if (adminUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (!"admin".equalsIgnoreCase(adminUser.getRole())) {
            throw new AccessDeniedException("Access denied: only admins can approve or reject association requests");
        }
    }
}

