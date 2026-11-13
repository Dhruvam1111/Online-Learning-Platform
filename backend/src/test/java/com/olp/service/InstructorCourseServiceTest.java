package com.olp.service;

import com.olp.dto.CourseDraftRequest;
import com.olp.dto.CourseInstructorResponse;
import com.olp.dto.CoursePrerequisiteRequest;
import com.olp.dto.CourseResponse;
import com.olp.dto.CreateCourseRequest;
import com.olp.dto.PageResponse;
import com.olp.dto.UpdateCourseRequest;
import com.olp.dto.UpdateCourseStatusRequest;
import com.olp.entity.Course;
import com.olp.entity.CourseInstructor;
import com.olp.entity.CourseInstructorRole;
import com.olp.entity.CoursePrerequisite;
import com.olp.entity.CourseStatus;
import com.olp.entity.InstructorProfile;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CoursePrerequisiteRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.EnrollmentRepository;
import com.olp.repository.InstructorProfileRepository;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstructorCourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseInstructorRepository courseInstructorRepository;
    @Mock
    private CoursePrerequisiteRepository coursePrerequisiteRepository;
    @Mock
    private InstructorProfileRepository instructorProfileRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CoursePricingService coursePricingService;
    @Mock
    private CoursePreviewService coursePreviewService;

    @InjectMocks
    private InstructorCourseService service;

    private User instructor(Integer id) {
        User user = new User("Dr. Chen", "chen" + id + "@olp.com", "hash", "instructor");
        user.setId(id);
        return user;
    }

    private User student(Integer id) {
        User user = new User("Sam Student", "sam" + id + "@olp.com", "hash", "student");
        user.setId(id);
        return user;
    }

    private Course course(Integer id, User owner) {
        Course course = new Course(owner, "Original Title", "Original Description", CourseStatus.DRAFT);
        course.setId(id);
        return course;
    }

    private Course givenOwnedCourse(Integer courseId, User owner) {
        Course course = course(courseId, owner);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        return course;
    }

    private void stubSaveReturnsArgument() {
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ===== resolveUser =====

    @Test
    void testResolveUser_NullAuthentication_Throws() {
        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(null));
    }

    @Test
    void testResolveUser_NullPrincipal_Throws() {
        Authentication auth = mock(Authentication.class);

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                () -> service.resolveUser(auth));

        assertTrue(ex.getMessage().contains("not authenticated"));
    }

    @Test
    void testResolveUser_UserPrincipal_ReturnedWithoutRepositoryLookup() {
        User user = instructor(1);
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null);

        assertSame(user, service.resolveUser(auth));
        verifyNoInteractions(userRepository);
    }

    @Test
    void testResolveUser_EmailPrincipal_LooksUpByEmail() {
        User user = instructor(1);
        Authentication auth = new UsernamePasswordAuthenticationToken("chen1@olp.com", null);
        when(userRepository.findByEmail("chen1@olp.com")).thenReturn(Optional.of(user));

        assertSame(user, service.resolveUser(auth));
    }

    @Test
    void testResolveUser_EmailPrincipalNotFound_Throws() {
        Authentication auth = new UsernamePasswordAuthenticationToken("ghost@olp.com", null);
        when(userRepository.findByEmail("ghost@olp.com")).thenReturn(Optional.empty());

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                () -> service.resolveUser(auth));

        assertTrue(ex.getMessage().contains("User not found"));
    }

    // ===== getInstructorCourses =====

    @Test
    void testGetInstructorCourses_NullUser_Throws() {
        assertThrows(InvalidCredentialsException.class, () -> service.getInstructorCourses(null));
        verifyNoInteractions(courseRepository);
    }

    @Test
    void testGetInstructorCourses_NonInstructor_ThrowsAccessDenied() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> service.getInstructorCourses(student(2)));

        assertTrue(ex.getMessage().contains("only instructors"));
        verifyNoInteractions(courseRepository);
    }

    @Test
    void testGetInstructorCourses_ReturnsOwnCoursesMapped() {
        User owner = instructor(1);
        when(courseRepository.findByInstructorIdOrderByIdAsc(1))
                .thenReturn(List.of(course(10, owner), course(11, owner)));

        List<CourseResponse> result = service.getInstructorCourses(owner);

        assertEquals(2, result.size());
        assertEquals(10, result.get(0).getId());
        assertEquals(11, result.get(1).getId());
        assertEquals(1, result.get(0).getInstructorId());
        assertEquals("Original Title", result.get(0).getTitle());
    }

    @Test
    void testGetInstructorCourses_NoCourses_ReturnsEmptyList() {
        when(courseRepository.findByInstructorIdOrderByIdAsc(1)).thenReturn(List.of());

        assertTrue(service.getInstructorCourses(instructor(1)).isEmpty());
    }

    @Test
    void testGetInstructorCourses_RoleCheckIsCaseInsensitive() {
        User owner = instructor(1);
        owner.setRole("INSTRUCTOR");
        when(courseRepository.findByInstructorIdOrderByIdAsc(1)).thenReturn(List.of());

        assertDoesNotThrow(() -> service.getInstructorCourses(owner));
    }

    // ===== createCourse =====

    @Test
    void testCreateCourse_NullUser_Throws() {
        assertThrows(InvalidCredentialsException.class,
                () -> service.createCourse(null, new CreateCourseRequest("T", "D")));
    }

    @Test
    void testCreateCourse_NonInstructor_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class,
                () -> service.createCourse(student(2), new CreateCourseRequest("T", "D")));
    }

    @Test
    void testCreateCourse_BlankTitle_ThrowsAndDoesNotSave() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createCourse(instructor(1), new CreateCourseRequest("   ", "D")));

        assertTrue(ex.getMessage().contains("Course title cannot be blank"));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void testCreateCourse_NullTitle_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createCourse(instructor(1), new CreateCourseRequest(null, "D")));
    }

    @Test
    void testCreateCourse_Success_TrimsFieldsAndStartsAsDraft() {
        User owner = instructor(1);
        stubSaveReturnsArgument();

        CourseResponse response = service.createCourse(owner,
                new CreateCourseRequest("  Spring Boot  ", "  Learn it  "));

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository, times(1)).save(captor.capture());
        Course saved = captor.getValue();
        assertEquals("Spring Boot", saved.getTitle());
        assertEquals("Learn it", saved.getDescription());
        assertEquals(CourseStatus.DRAFT, saved.getStatus());
        assertSame(owner, saved.getInstructor());
        assertEquals("Spring Boot", response.getTitle());
        assertEquals(CourseStatus.DRAFT, response.getStatus());
        assertEquals(1, response.getInstructorId());
    }

    @Test
    void testCreateCourse_NullDescription_StoredAsNull() {
        stubSaveReturnsArgument();

        service.createCourse(instructor(1), new CreateCourseRequest("Title", null));

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertNull(captor.getValue().getDescription());
    }

    @Test
    void testCreateCourse_WithCategoryOutcomesAndPrerequisites_Success() {
        User owner = instructor(1);
        stubSaveReturnsArgument();

        Course reqCourse = course(5, owner);
        reqCourse.setTitle("Prerequisite Course Title");
        when(courseRepository.findById(5)).thenReturn(Optional.of(reqCourse));

        when(coursePrerequisiteRepository.save(any(CoursePrerequisite.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(courseInstructorRepository.save(any(CourseInstructor.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CoursePrerequisiteRequest p1 = new CoursePrerequisiteRequest("COURSE", 5, null, "Must complete course 5");
        CoursePrerequisiteRequest p2 = new CoursePrerequisiteRequest("SKILL", null, "Basic Java",
                "Familiarity with syntax");
        List<String> outcomes = List.of("Master Spring Boot", "Build microservices");

        CreateCourseRequest request = new CreateCourseRequest(
                "Advanced Spring Boot",
                "Deep dive into production engineering",
                "Software Engineering",
                List.of(p1, p2),
                outcomes);

        CourseResponse response = service.createCourse(owner, request);

        assertNotNull(response);
        assertEquals("Advanced Spring Boot", response.getTitle());
        assertEquals("Software Engineering", response.getCategory());
        assertEquals(CourseStatus.DRAFT, response.getStatus());
        assertEquals(2, response.getLearningOutcomes().size());
        assertEquals("Master Spring Boot", response.getLearningOutcomes().get(0));
        assertEquals(2, response.getPrerequisites().size());

        ArgumentCaptor<CourseInstructor> instCaptor = ArgumentCaptor.forClass(CourseInstructor.class);
        verify(courseInstructorRepository, times(1)).save(instCaptor.capture());
        assertEquals(CourseInstructorRole.OWNER, instCaptor.getValue().getRole());
        assertTrue(instCaptor.getValue().isOwner());
        assertSame(owner, instCaptor.getValue().getInstructor());

        verify(coursePrerequisiteRepository, times(2)).save(any(CoursePrerequisite.class));
    }

    @Test
    void testCreateCourse_MisleadingAccreditationClaim_Throws() {
        User owner = instructor(1);

        CreateCourseRequest request = new CreateCourseRequest(
                "Earn an Accredited Degree in 3 Months",
                "Valid description");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createCourse(owner, request));
        assertTrue(ex.getMessage().contains("misleading claims regarding accreditation"));
    }

    @Test
    void testCreateCourse_MisleadingDegreeEquivalenceInDescription_Throws() {
        User owner = instructor(1);

        CreateCourseRequest request = new CreateCourseRequest(
                "Full Stack Web Development",
                "This course offers university degree equivalence for tech jobs.");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createCourse(owner, request));
        assertTrue(ex.getMessage().contains("misleading claims regarding accreditation"));
    }

    @Test
    void testCreateCourse_BlankCategory_Throws() {
        User owner = instructor(1);

        CreateCourseRequest request = new CreateCourseRequest("Valid Title", "Description", "   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createCourse(owner, request));
        assertTrue(ex.getMessage().contains("Course category cannot be blank"));
    }

    @Test
    void testCreateCourse_BlankLearningOutcome_Throws() {
        User owner = instructor(1);

        CreateCourseRequest request = new CreateCourseRequest(
                "Valid Title", "Desc", "Category",
                null, List.of("Valid Outcome", "   "));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createCourse(owner, request));
        assertTrue(ex.getMessage().contains("Learning outcome cannot be blank"));
    }

    @Test
    void testCreateCourse_InvalidPrerequisiteType_Throws() {
        User owner = instructor(1);

        CoursePrerequisiteRequest badPrereq = new CoursePrerequisiteRequest("UNKNOWN", "Some requirement");
        CreateCourseRequest request = new CreateCourseRequest(
                "Valid Title", "Desc", "Category",
                List.of(badPrereq), null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createCourse(owner, request));
        assertTrue(ex.getMessage().contains("Invalid prerequisite type"));
    }

    @Test
    void testCreateCourse_MissingPrerequisiteCourse_ThrowsResourceNotFound() {
        User owner = instructor(1);
        when(courseRepository.findById(999)).thenReturn(Optional.empty());

        CoursePrerequisiteRequest p = new CoursePrerequisiteRequest("COURSE", 999, null, null);
        CreateCourseRequest request = new CreateCourseRequest(
                "Valid Title", "Desc", "Category",
                List.of(p), null);

        assertThrows(ResourceNotFoundException.class, () -> service.createCourse(owner, request));
    }

    @Test
    void testCreateCourse_DifferentInstructorIdInBody_ThrowsAccessDenied() {
        User owner = instructor(1);

        CreateCourseRequest request = new CreateCourseRequest("Valid Title", "Desc");
        request.setInstructorId(999); // foreign instructor ID

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> service.createCourse(owner, request));
        assertTrue(ex.getMessage().contains("Cannot create a course for another instructor"));
    }

    // ===== updateCourse =====

    @Test
    void testUpdateCourse_NullUser_Throws() {
        assertThrows(InvalidCredentialsException.class,
                () -> service.updateCourse(null, 10, new UpdateCourseRequest("T", "D")));
    }

    @Test
    void testUpdateCourse_NonInstructor_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class,
                () -> service.updateCourse(student(2), 10, new UpdateCourseRequest("T", "D")));
    }

    @Test
    void testUpdateCourse_BodyInstructorIdMismatch_ThrowsAccessDenied() {
        UpdateCourseRequest request = new UpdateCourseRequest("T", "D");
        request.setInstructorId(99);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> service.updateCourse(instructor(1), 10, request));

        assertTrue(ex.getMessage().contains("another instructor"));
        verifyNoInteractions(courseRepository);
    }

    @Test
    void testUpdateCourse_BodyCourseIdMismatch_ThrowsIllegalArgument() {
        UpdateCourseRequest request = new UpdateCourseRequest("T", "D");
        request.setId(77);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateCourse(instructor(1), 10, request));

        assertTrue(ex.getMessage().contains("do not match"));
        verifyNoInteractions(courseRepository);
    }

    @Test
    void testUpdateCourse_NullCourseId_Throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateCourse(instructor(1), null, new UpdateCourseRequest("T", "D")));

        assertTrue(ex.getMessage().contains("Course ID cannot be null"));
    }

    @Test
    void testUpdateCourse_CourseNotFound_Throws() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.updateCourse(instructor(1), 99, new UpdateCourseRequest("T", "D")));

        assertTrue(ex.getMessage().contains("Course not found with id: 99"));
    }

    @Test
    void testUpdateCourse_CourseOwnedByAnotherInstructor_ThrowsAccessDenied() {
        givenOwnedCourse(10, instructor(2));

        assertThrows(AccessDeniedException.class,
                () -> service.updateCourse(instructor(1), 10, new UpdateCourseRequest("T", "D")));

        verify(courseRepository, never()).save(any());
    }

    @Test
    void testUpdateCourse_CourseWithoutInstructor_ThrowsAccessDenied() {
        Course orphan = course(10, null);
        when(courseRepository.findById(10)).thenReturn(Optional.of(orphan));

        assertThrows(AccessDeniedException.class,
                () -> service.updateCourse(instructor(1), 10, new UpdateCourseRequest("T", "D")));
    }

    @Test
    void testUpdateCourse_BlankTitle_ThrowsAndDoesNotSave() {
        givenOwnedCourse(10, instructor(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateCourse(instructor(1), 10, new UpdateCourseRequest("  ", "D")));

        assertTrue(ex.getMessage().contains("Title cannot be blank"));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void testUpdateCourse_NullTitle_Throws() {
        givenOwnedCourse(10, instructor(1));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateCourse(instructor(1), 10, new UpdateCourseRequest(null, "D")));
    }

    @Test
    void testUpdateCourse_Success_TrimsTitleAndSetsDescriptionVerbatim() {
        Course existing = givenOwnedCourse(10, instructor(1));
        stubSaveReturnsArgument();

        CourseResponse response = service.updateCourse(instructor(1), 10,
                new UpdateCourseRequest("  New Title  ", "  New Desc  "));

        assertEquals("New Title", existing.getTitle());
        assertEquals("  New Desc  ", existing.getDescription());
        assertEquals("New Title", response.getTitle());
        verify(courseRepository, times(1)).save(existing);
    }

    @Test
    void testUpdateCourse_NullDescription_KeepsExistingDescription() {
        Course existing = givenOwnedCourse(10, instructor(1));
        stubSaveReturnsArgument();

        service.updateCourse(instructor(1), 10, new UpdateCourseRequest("New Title", null));

        assertEquals("Original Description", existing.getDescription());
    }

    @Test
    void testUpdateCourse_MatchingBodyIds_Allowed() {
        givenOwnedCourse(10, instructor(1));
        stubSaveReturnsArgument();
        UpdateCourseRequest request = new UpdateCourseRequest("New Title", "D");
        request.setId(10);
        request.setInstructorId(1);

        CourseResponse response = service.updateCourse(instructor(1), 10, request);

        assertEquals("New Title", response.getTitle());
    }

    // ===== updateCourseStatus =====

    @Test
    void testUpdateCourseStatus_NullUser_Throws() {
        assertThrows(InvalidCredentialsException.class,
                () -> service.updateCourseStatus(null, 10, new UpdateCourseStatusRequest("PUBLISHED")));
    }

    @Test
    void testUpdateCourseStatus_NonInstructor_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class,
                () -> service.updateCourseStatus(student(2), 10, new UpdateCourseStatusRequest("PUBLISHED")));
    }

    @Test
    void testUpdateCourseStatus_CourseNotFound_Throws() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateCourseStatus(instructor(1), 99, new UpdateCourseStatusRequest("PUBLISHED")));
    }

    @Test
    void testUpdateCourseStatus_NotOwner_ThrowsAccessDenied() {
        givenOwnedCourse(10, instructor(2));

        assertThrows(AccessDeniedException.class,
                () -> service.updateCourseStatus(instructor(1), 10, new UpdateCourseStatusRequest("PUBLISHED")));
    }

    @Test
    void testUpdateCourseStatus_NullRequest_Throws() {
        givenOwnedCourse(10, instructor(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateCourseStatus(instructor(1), 10, null));

        assertTrue(ex.getMessage().contains("Course status cannot be blank"));
    }

    @Test
    void testUpdateCourseStatus_NullStatus_Throws() {
        givenOwnedCourse(10, instructor(1));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateCourseStatus(instructor(1), 10, new UpdateCourseStatusRequest(null)));
    }

    @Test
    void testUpdateCourseStatus_BlankStatus_Throws() {
        givenOwnedCourse(10, instructor(1));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateCourseStatus(instructor(1), 10, new UpdateCourseStatusRequest("   ")));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void testUpdateCourseStatus_UnknownStatus_ThrowsAndDoesNotSave() {
        givenOwnedCourse(10, instructor(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateCourseStatus(instructor(1), 10, new UpdateCourseStatusRequest("unknown_status")));

        assertTrue(ex.getMessage().contains("Invalid course status"));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void testUpdateCourseStatus_Archived_Supported() {
        assertStatusChange("archived", CourseStatus.ARCHIVED);
    }

    private void assertStatusChange(String input, CourseStatus expected) {
        Course existing = givenOwnedCourse(10, instructor(1));
        stubSaveReturnsArgument();

        CourseResponse response = service.updateCourseStatus(instructor(1), 10, new UpdateCourseStatusRequest(input));

        assertEquals(expected, existing.getStatus());
        assertEquals(expected, response.getStatus());
        verify(courseRepository, times(1)).save(existing);
    }

    @Test
    void testUpdateCourseStatus_LowercasePublished_IsNormalised() {
        assertStatusChange("published", CourseStatus.PUBLISHED);
    }

    @Test
    void testUpdateCourseStatus_HyphenatedPendingReview_IsNormalised() {
        assertStatusChange("pending-review", CourseStatus.PENDING_REVIEW);
    }

    @Test
    void testUpdateCourseStatus_SurroundingWhitespace_IsTrimmed() {
        assertStatusChange("  DRAFT  ", CourseStatus.DRAFT);
    }

    @Test
    void testUpdateCourseStatus_InstructorCanSetRejected_NoTransitionRules() {
        assertStatusChange("REJECTED", CourseStatus.REJECTED);
    }

    @Test
    void testUpdateCourseStatus_PublishedBackToDraft_Allowed() {
        Course existing = givenOwnedCourse(10, instructor(1));
        existing.setStatus(CourseStatus.PUBLISHED);
        stubSaveReturnsArgument();

        CourseResponse response = service.updateCourseStatus(instructor(1), 10, new UpdateCourseStatusRequest("draft"));

        assertEquals(CourseStatus.DRAFT, response.getStatus());
    }

    // ===== FR-I07: Save Course as Draft & Statuses =====

    @Test
    void testCourseEntity_SupportsAllRequiredStatusesWithDraftDefault() {
        Course defaultCourse = new Course();
        assertEquals(CourseStatus.DRAFT, defaultCourse.getStatus());

        assertEquals(CourseStatus.DRAFT, CourseStatus.fromString("DRAFT"));
        assertEquals(CourseStatus.PENDING_REVIEW, CourseStatus.fromString("PENDING_REVIEW"));
        assertEquals(CourseStatus.PUBLISHED, CourseStatus.fromString("PUBLISHED"));
        assertEquals(CourseStatus.ARCHIVED, CourseStatus.fromString("ARCHIVED"));
    }

    @Test
    void testSaveDraft_NewCourseWithMinimalEmptyPayload_CreatesUntitledDraft() {
        User owner = instructor(1);
        stubSaveReturnsArgument();

        CourseResponse response = service.saveDraft(owner, new CourseDraftRequest());

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        Course saved = captor.getValue();
        assertEquals("Untitled Draft", saved.getTitle());
        assertNull(saved.getDescription());
        assertEquals("General", saved.getCategory());
        assertEquals(CourseStatus.DRAFT, saved.getStatus());
        assertEquals("Untitled Draft", response.getTitle());
        assertEquals(CourseStatus.DRAFT, response.getStatus());
        verify(courseInstructorRepository, times(1)).save(any(CourseInstructor.class));
    }

    @Test
    void testSaveDraft_NewCourseWithPartialData_SkipsCompletenessValidation() {
        User owner = instructor(1);
        stubSaveReturnsArgument();

        CourseDraftRequest request = new CourseDraftRequest(null, "Work in progress description");
        request.setLearningOutcomes(List.of("Understand draft flow", "   "));

        CourseResponse response = service.saveDraft(owner, request);

        assertEquals("Untitled Draft", response.getTitle());
        assertEquals("Work in progress description", response.getDescription());
        assertEquals(CourseStatus.DRAFT, response.getStatus());
        assertEquals(List.of("Understand draft flow"), response.getLearningOutcomes());
    }

    @Test
    void testSaveDraft_ExistingDraft_PartialUpdatePreservesOmittedFields() {
        User owner = instructor(1);
        Course existing = givenOwnedCourse(10, owner);
        existing.setCategory("Data Science");
        existing.setLearningOutcomesList(List.of("Existing outcome"));
        stubSaveReturnsArgument();

        CourseDraftRequest request = new CourseDraftRequest();
        request.setDescription("Updated partial description");

        CourseResponse response = service.saveDraft(owner, 10, request);

        assertEquals("Original Title", response.getTitle());
        assertEquals("Data Science", response.getCategory());
        assertEquals(List.of("Existing outcome"), response.getLearningOutcomes());
        assertEquals("Updated partial description", response.getDescription());
        assertEquals(CourseStatus.DRAFT, response.getStatus());
    }

    @Test
    void testSaveDraft_ExistingDraft_CoInstructorAllowed() {
        givenOwnedCourse(10, instructor(1));
        User coInstructor = instructor(2);
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 2)).thenReturn(true);
        stubSaveReturnsArgument();

        CourseDraftRequest request = new CourseDraftRequest("Co-edited Draft", null);

        CourseResponse response = service.saveDraft(coInstructor, 10, request);

        assertEquals("Co-edited Draft", response.getTitle());
        assertEquals("Original Description", response.getDescription());
        assertEquals(CourseStatus.DRAFT, response.getStatus());
    }

    @Test
    void testSaveDraft_PublishedCourse_RejectedAndPreservesStatusAndData() {
        User owner = instructor(1);
        Course published = givenOwnedCourse(10, owner);
        published.setStatus(CourseStatus.PUBLISHED);

        CourseDraftRequest request = new CourseDraftRequest("Changed Title", "Changed Desc");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.saveDraft(owner, 10, request));

        assertTrue(ex.getMessage().contains("PUBLISHED"));
        assertEquals(CourseStatus.PUBLISHED, published.getStatus());
        assertEquals("Original Title", published.getTitle());
        assertEquals("Original Description", published.getDescription());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void testSaveDraft_PendingReviewCourse_RejectedAndPreservesStatusAndData() {
        User owner = instructor(1);
        Course submitted = givenOwnedCourse(10, owner);
        submitted.setStatus(CourseStatus.PENDING_REVIEW);

        CourseDraftRequest request = new CourseDraftRequest("Tampered Title", null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.saveDraft(owner, 10, request));

        assertTrue(ex.getMessage().contains("PENDING_REVIEW"));
        assertEquals(CourseStatus.PENDING_REVIEW, submitted.getStatus());
        assertEquals("Original Title", submitted.getTitle());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void testSaveDraft_ArchivedCourse_RejectedAndPreservesStatus() {
        User owner = instructor(1);
        Course archived = givenOwnedCourse(10, owner);
        archived.setStatus(CourseStatus.ARCHIVED);

        assertThrows(IllegalStateException.class,
                () -> service.saveDraft(owner, 10, new CourseDraftRequest("Title", null)));
        assertEquals(CourseStatus.ARCHIVED, archived.getStatus());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void testSaveDraft_RejectedCourse_AllowsRevisionBackToDraft() {
        User owner = instructor(1);
        Course rejected = givenOwnedCourse(10, owner);
        rejected.setStatus(CourseStatus.REJECTED);
        stubSaveReturnsArgument();

        CourseResponse response = service.saveDraft(owner, 10, new CourseDraftRequest("Revised Draft", null));

        assertEquals(CourseStatus.DRAFT, response.getStatus());
        assertEquals("Revised Draft", response.getTitle());
    }

    @Test
    void testSaveDraft_StudentOrPendingInstructor_ThrowsAccessDenied() {
        User pending = instructor(3);
        pending.setAccountStatus("pending");

        assertThrows(AccessDeniedException.class, () -> service.saveDraft(student(2), new CourseDraftRequest()));
        assertThrows(AccessDeniedException.class, () -> service.saveDraft(pending, new CourseDraftRequest()));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void testSaveDraft_OtherInstructorCannotModifyDraft() {
        givenOwnedCourse(10, instructor(1));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 2)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.saveDraft(instructor(2), 10, new CourseDraftRequest("Hijack", null)));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void testSaveDraft_ForeignInstructorIdInBody_ThrowsAccessDenied() {
        CourseDraftRequest request = new CourseDraftRequest("Title", "Desc");
        request.setInstructorId(99);

        assertThrows(AccessDeniedException.class, () -> service.saveDraft(instructor(1), request));
    }

    @Test
    void testSaveDraft_InvalidTitleLengthAndMisleadingClaim_Rejected() {
        User owner = instructor(1);

        CourseDraftRequest tooLong = new CourseDraftRequest("A".repeat(201), null);
        assertThrows(IllegalArgumentException.class, () -> service.saveDraft(owner, tooLong));

        CourseDraftRequest misleading = new CourseDraftRequest("Draft", "Official government accreditation included");
        assertThrows(IllegalArgumentException.class, () -> service.saveDraft(owner, misleading));
    }

    @Test
    void testSaveDraft_SelfPrerequisite_ThrowsIllegalArgument() {
        User owner = instructor(1);
        givenOwnedCourse(10, owner);

        CourseDraftRequest request = new CourseDraftRequest();
        request.setPrerequisites(List.of(new CoursePrerequisiteRequest("COURSE", 10, null, null)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.saveDraft(owner, 10, request));
        assertTrue(ex.getMessage().contains("prerequisite of itself"));
    }

    @Test
    void testGetInstructorDrafts_ReturnsOnlyDraftCoursesIncludingCoInstructor() {
        User owner = instructor(1);
        Course draft1 = course(10, owner);
        Course published = course(11, owner);
        published.setStatus(CourseStatus.PUBLISHED);
        Course coDraft = course(12, instructor(9));

        when(courseRepository.findByInstructorIdOrderByIdAsc(1)).thenReturn(List.of(draft1, published));
        when(courseInstructorRepository.findByInstructorId(1))
                .thenReturn(List.of(new CourseInstructor(coDraft, owner, "co-instructor")));

        List<CourseResponse> drafts = service.getInstructorDrafts(owner);

        assertEquals(2, drafts.size());
        assertEquals(10, drafts.get(0).getId());
        assertEquals(12, drafts.get(1).getId());
    }

    @Test
    void testGetInstructorCourseById_AllowsOwnerAndDeniesStranger() {
        User owner = instructor(1);
        givenOwnedCourse(10, owner);
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 2)).thenReturn(false);

        CourseResponse found = service.getInstructorCourseById(owner, 10);
        assertEquals(10, found.getId());
        assertEquals(CourseStatus.DRAFT, found.getStatus());

        assertThrows(AccessDeniedException.class, () -> service.getInstructorCourseById(instructor(2), 10));
    }

    // ===== FR-I59: Multiple Instructors per Course =====

    @Test
    void testCourseInstructorRole_SupportsOwnerAndCoInstructorFormats() {
        assertEquals(CourseInstructorRole.OWNER, CourseInstructorRole.fromString("OWNER"));
        assertEquals(CourseInstructorRole.OWNER, CourseInstructorRole.fromString("owner"));
        assertEquals(CourseInstructorRole.CO_INSTRUCTOR, CourseInstructorRole.fromString("CO_INSTRUCTOR"));
        assertEquals(CourseInstructorRole.CO_INSTRUCTOR, CourseInstructorRole.fromString("co-instructor"));
        assertEquals("owner", CourseInstructorRole.OWNER.toDatabaseValue());
        assertEquals("co-instructor", CourseInstructorRole.CO_INSTRUCTOR.toDatabaseValue());
        assertThrows(IllegalArgumentException.class, () -> CourseInstructorRole.fromString("STUDENT"));
        assertThrows(IllegalArgumentException.class, () -> CourseInstructorRole.fromString("  "));
    }

    @Test
    void testUpdateCourse_CoInstructorAllowed_StrangerDenied() {
        User owner = instructor(1);
        User coInstructor = instructor(2);
        User stranger = instructor(3);
        givenOwnedCourse(10, owner);
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 2)).thenReturn(true);
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 3)).thenReturn(false);
        stubSaveReturnsArgument();

        CourseResponse updated = service.updateCourse(
                coInstructor, 10, new UpdateCourseRequest("Co-Updated Title", "Co-Updated Description"));

        assertEquals("Co-Updated Title", updated.getTitle());
        assertThrows(AccessDeniedException.class,
                () -> service.updateCourse(stranger, 10, new UpdateCourseRequest("Hack", "Desc")));
    }

    @Test
    void testArchiveCourse_OwnerAllowed_CoInstructorDenied() {
        User owner = instructor(1);
        User coInstructor = instructor(2);
        Course c = givenOwnedCourse(10, owner);
        when(courseInstructorRepository.existsByCourseIdAndInstructorIdAndRole(10, 2, CourseInstructorRole.OWNER))
                .thenReturn(false);
        stubSaveReturnsArgument();

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> service.archiveCourse(coInstructor, 10));
        assertTrue(ex.getMessage().contains("OWNER"));
        assertEquals(CourseStatus.DRAFT, c.getStatus());

        CourseResponse archived = service.archiveCourse(owner, 10);
        assertEquals(CourseStatus.ARCHIVED, archived.getStatus());
    }

    @Test
    void testUpdateCourseStatus_ArchiveAndUnarchiveByCoInstructorDenied() {
        User owner = instructor(1);
        User coInstructor = instructor(2);
        Course c = givenOwnedCourse(10, owner);
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 2)).thenReturn(true);
        when(courseInstructorRepository.existsByCourseIdAndInstructorIdAndRole(10, 2, CourseInstructorRole.OWNER))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.updateCourseStatus(coInstructor, 10, new UpdateCourseStatusRequest("ARCHIVED")));

        c.setStatus(CourseStatus.ARCHIVED);
        assertThrows(AccessDeniedException.class,
                () -> service.updateCourseStatus(coInstructor, 10, new UpdateCourseStatusRequest("DRAFT")));
        assertEquals(CourseStatus.ARCHIVED, c.getStatus());
    }

    @Test
    void testDeleteCourse_OwnerAllowed_CoInstructorDenied() {
        User owner = instructor(1);
        User coInstructor = instructor(2);
        Course c = givenOwnedCourse(10, owner);
        when(courseInstructorRepository.existsByCourseIdAndInstructorIdAndRole(10, 2, CourseInstructorRole.OWNER))
                .thenReturn(false);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> service.deleteCourse(coInstructor, 10));
        assertTrue(ex.getMessage().contains("OWNER"));
        verify(courseRepository, never()).delete(any());

        service.deleteCourse(owner, 10);
        verify(courseRepository, times(1)).delete(c);
    }

    @Test
    void testGetCourseInstructors_ReturnsOwnerAndCoInstructorsWithProfilesAndDeduplicates() {
        User owner = instructor(1);
        User co1 = instructor(2);
        User co2 = instructor(3);
        User stranger = instructor(4);
        Course c = givenOwnedCourse(10, owner);

        CourseInstructor mOwner = new CourseInstructor(c, owner, CourseInstructorRole.OWNER);
        CourseInstructor mCo1 = new CourseInstructor(c, co1, CourseInstructorRole.CO_INSTRUCTOR);
        CourseInstructor mCo1Dup = new CourseInstructor(c, co1, CourseInstructorRole.CO_INSTRUCTOR);
        CourseInstructor mCo2 = new CourseInstructor(c, co2, CourseInstructorRole.CO_INSTRUCTOR);

        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 2)).thenReturn(true);
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 4)).thenReturn(false);
        when(courseInstructorRepository.findByCourseIdWithInstructor(10))
                .thenReturn(List.of(mCo1, mOwner, mCo1Dup, mCo2));

        InstructorProfile p1 = new InstructorProfile(owner, "Ph.D. CS", "Algorithms", "Lead prof");
        InstructorProfile p2 = new InstructorProfile(co1, "M.Sc. SE", "Databases", "Lab lead");
        when(instructorProfileRepository.findByInstructorIdIn(anyList())).thenReturn(List.of(p1, p2));

        List<CourseInstructorResponse> list = service.getCourseInstructors(co1, 10);

        assertEquals(3, list.size());
        assertEquals(CourseInstructorRole.OWNER, list.get(0).getRole());
        assertEquals(1, list.get(0).getInstructorId());
        assertEquals("Ph.D. CS", list.get(0).getQualifications());

        assertEquals(CourseInstructorRole.CO_INSTRUCTOR, list.get(1).getRole());
        assertEquals(2, list.get(1).getInstructorId());
        assertEquals("Databases", list.get(1).getExpertiseAreas());

        assertEquals(CourseInstructorRole.CO_INSTRUCTOR, list.get(2).getRole());
        assertEquals(3, list.get(2).getInstructorId());

        assertThrows(AccessDeniedException.class, () -> service.getCourseInstructors(stranger, 10));
        assertThrows(AccessDeniedException.class, () -> service.getCourseInstructors(student(5), 10));
    }

    @Test
    void testRemoveCourseInstructor_OwnerCanRemoveCoInstructor_CoInstructorAndSelfOwnerDenied() {
        User owner = instructor(1);
        User co1 = instructor(2);
        Course c = givenOwnedCourse(10, owner);
        CourseInstructor coMapping = new CourseInstructor(c, co1, CourseInstructorRole.CO_INSTRUCTOR);

        when(courseInstructorRepository.existsByCourseIdAndInstructorIdAndRole(10, 2, CourseInstructorRole.OWNER))
                .thenReturn(false);
        when(courseInstructorRepository.findByCourseIdAndInstructorId(10, 2))
                .thenReturn(Optional.of(coMapping));

        assertThrows(AccessDeniedException.class, () -> service.removeCourseInstructor(co1, 10, 2));
        assertThrows(IllegalStateException.class, () -> service.removeCourseInstructor(owner, 10, 1));

        service.removeCourseInstructor(owner, 10, 2);
        verify(courseInstructorRepository).delete(coMapping);
    }

    // ===== FR-I60: My Courses List (Pagination, Status Filtering, Batch Enrollment Counts) =====

    @Test
    void testGetMyCourses_Unfiltered_ReturnsPagedOwnerAndCoInstructorCoursesWithBatchEnrollmentCounts() {
        User owner = instructor(1);
        User otherOwner = instructor(2);

        Course ownedPublished = new Course(owner, "Java Fundamentals", "Learn core Java", "Programming", CourseStatus.PUBLISHED);
        ownedPublished.setId(10);
        Course coInstructorDraft = new Course(otherOwner, "System Design", "Scalable systems", "Architecture", CourseStatus.DRAFT);
        coInstructorDraft.setId(11);

        Page<Course> page = new PageImpl<>(
                List.of(ownedPublished, coInstructorDraft),
                PageRequest.of(0, 10),
                2);
        when(courseRepository.findAssociatedCoursesByInstructor(eq(1), any(Pageable.class))).thenReturn(page);
        when(enrollmentRepository.countEnrollmentsByCourseIds(List.of(10, 11)))
                .thenReturn(List.<Object[]>of(new Object[] { 10, 42L }));

        PageResponse<CourseResponse> result = service.getMyCourses(owner, null, 0, 10);

        assertEquals(2, result.getContent().size());
        assertEquals(0, result.getPageNumber());
        assertEquals(10, result.getPageSize());
        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLast());

        CourseResponse first = result.getContent().get(0);
        assertEquals(10, first.getId());
        assertEquals("Java Fundamentals", first.getTitle());
        assertEquals("Learn core Java", first.getDescription());
        assertEquals("Programming", first.getCategory());
        assertEquals(CourseStatus.PUBLISHED, first.getStatus());
        assertEquals(42L, first.getEnrollmentCount());

        CourseResponse second = result.getContent().get(1);
        assertEquals(11, second.getId());
        assertEquals("Architecture", second.getCategory());
        assertEquals(CourseStatus.DRAFT, second.getStatus());
        assertEquals(0L, second.getEnrollmentCount());

        // Single batch query used — no N+1 per-course count queries
        verify(enrollmentRepository, times(1)).countEnrollmentsByCourseIds(List.of(10, 11));
        verify(enrollmentRepository, never()).countByCourseId(any());
    }

    @Test
    void testGetMyCourses_StatusFilter_SupportsDraftPendingReviewPublishedArchived() {
        User owner = instructor(1);
        List<CourseStatus> supportedStatuses = List.of(
                CourseStatus.DRAFT,
                CourseStatus.PENDING_REVIEW,
                CourseStatus.PUBLISHED,
                CourseStatus.ARCHIVED);

        for (CourseStatus expectedStatus : supportedStatuses) {
            Course c = new Course(owner, "Course " + expectedStatus.name(), "Desc", "Tech", expectedStatus);
            c.setId(100);
            when(courseRepository.findAssociatedCoursesByInstructorAndStatus(eq(1), eq(expectedStatus), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(c), PageRequest.of(0, 5), 1));
            when(enrollmentRepository.countByCourseId(100)).thenReturn(7L);

            PageResponse<CourseResponse> result = service.getMyCourses(owner, expectedStatus.name().toLowerCase(), 0, 5);

            assertEquals(1, result.getContent().size());
            assertEquals(expectedStatus, result.getContent().get(0).getStatus());
            assertEquals(7L, result.getContent().get(0).getEnrollmentCount());
        }
    }

    @Test
    void testGetMyCourses_WithSearchAndStatusFilter_DelegatesProperly() {
        User owner = instructor(1);
        Course c = new Course(owner, "Spring Boot 3", "Microservices", "Backend", CourseStatus.PUBLISHED);
        c.setId(20);
        when(courseRepository.searchAssociatedCoursesByInstructorAndStatus(
                eq(1), eq(CourseStatus.PUBLISHED), eq("Spring"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(c), PageRequest.of(1, 1), 3));
        when(enrollmentRepository.countByCourseId(20)).thenReturn(15L);

        PageResponse<CourseResponse> page = service.getMyCourses(owner, "PUBLISHED", "  Spring  ", 1, 1);

        assertEquals(1, page.getContent().size());
        assertEquals(1, page.getPageNumber());
        assertEquals(1, page.getPageSize());
        assertEquals(3, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
        assertFalse(page.isLast());
        assertEquals(15L, page.getContent().get(0).getEnrollmentCount());
    }

    @Test
    void testGetMyCourses_EmptyResult_SkipsBatchQueries() {
        User owner = instructor(1);
        when(courseRepository.findAssociatedCoursesByInstructor(eq(1), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 10)));

        PageResponse<CourseResponse> result = service.getMyCourses(owner, null, 0, 10);

        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void testGetMyCourses_InvalidPaginationOrStatus_ThrowsIllegalArgument() {
        User owner = instructor(1);

        assertThrows(IllegalArgumentException.class, () -> service.getMyCourses(owner, null, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> service.getMyCourses(owner, null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> service.getMyCourses(owner, null, 0, 101));
        assertThrows(IllegalArgumentException.class, () -> service.getMyCourses(owner, "UNKNOWN_STATUS", 0, 10));
        verifyNoInteractions(courseRepository);
    }

    @Test
    void testGetMyCourses_StudentPendingAndUnauthenticatedDenied() {
        User pending = instructor(3);
        pending.setAccountStatus("pending");

        assertThrows(InvalidCredentialsException.class, () -> service.getMyCourses(null, null, 0, 10));
        assertThrows(AccessDeniedException.class, () -> service.getMyCourses(student(2), null, 0, 10));
        assertThrows(AccessDeniedException.class, () -> service.getMyCourses(pending, null, 0, 10));
        verifyNoInteractions(courseRepository, enrollmentRepository);
    }

    @Test
    void testGetCoursePricing_DelegatesToPricingService() {
        User owner = instructor(1);
        Course course = course(10, owner);
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));

        com.olp.dto.CoursePricingResponse response = new com.olp.dto.CoursePricingResponse();
        response.setCourseId(10);
        response.setBasePrice(new java.math.BigDecimal("999.00"));
        when(coursePricingService.calculatePricing(course)).thenReturn(response);

        com.olp.dto.CoursePricingResponse result = service.getCoursePricing(owner, 10);

        assertNotNull(result);
        assertEquals(new java.math.BigDecimal("999.00"), result.getBasePrice());
    }

    @Test
    void testUpdateCoursePricing_DelegatesToPricingService() {
        User owner = instructor(1);
        com.olp.dto.CoursePricingRequest request = new com.olp.dto.CoursePricingRequest(
                new java.math.BigDecimal("499.00"), null, null, null, null, 50
        );
        com.olp.dto.CoursePricingResponse response = new com.olp.dto.CoursePricingResponse();
        response.setCourseId(10);
        response.setBasePrice(new java.math.BigDecimal("499.00"));

        when(coursePricingService.updateCoursePricing(owner, 10, request)).thenReturn(response);

        com.olp.dto.CoursePricingResponse result = service.updateCoursePricing(owner, 10, request);

        assertNotNull(result);
        assertEquals(new java.math.BigDecimal("499.00"), result.getBasePrice());
    }

    @Test
    void testPreviewCourseAsStudent_DelegatesToPreviewService() {
        User owner = instructor(1);
        com.olp.dto.CoursePreviewResponse previewResponse = new com.olp.dto.CoursePreviewResponse();
        previewResponse.setId(10);
        previewResponse.setTitle("Quantum Physics");
        previewResponse.setPreview(true);
        previewResponse.setReadOnly(true);

        when(coursePreviewService.previewCourseAsStudent(owner, 10)).thenReturn(previewResponse);

        com.olp.dto.CoursePreviewResponse result = service.previewCourseAsStudent(owner, 10);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertTrue(result.isPreview());
        assertTrue(result.isReadOnly());
        verify(coursePreviewService).previewCourseAsStudent(owner, 10);
    }
}
