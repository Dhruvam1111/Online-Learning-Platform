package com.olp.service;

import com.olp.dto.CourseDraftRequest;
import com.olp.dto.CoursePrerequisiteRequest;
import com.olp.dto.CourseResponse;
import com.olp.dto.CreateCourseRequest;
import com.olp.dto.UpdateCourseRequest;
import com.olp.dto.UpdateCourseStatusRequest;
import com.olp.entity.Course;
import com.olp.entity.CourseInstructor;
import com.olp.entity.CoursePrerequisite;
import com.olp.entity.CourseStatus;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CoursePrerequisiteRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        assertEquals("owner", instCaptor.getValue().getRole());
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
}
