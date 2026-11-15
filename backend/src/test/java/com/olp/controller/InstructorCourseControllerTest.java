package com.olp.controller;

import com.olp.dto.ApiResponse;
import com.olp.dto.CourseAuditResponse;
import com.olp.dto.CourseCompletionCriteriaRequest;
import com.olp.dto.CourseCompletionCriteriaResponse;
import com.olp.dto.CourseDraftRequest;
import com.olp.dto.CourseInstructorResponse;
import com.olp.dto.CoursePreviewResponse;
import com.olp.dto.CoursePricingRequest;
import com.olp.dto.CoursePricingResponse;
import com.olp.dto.CourseResponse;
import com.olp.dto.CourseReviewStatusResponse;
import com.olp.dto.CourseSubmissionRequest;
import com.olp.dto.CourseSubmissionResponse;
import com.olp.dto.CreateCourseRequest;
import com.olp.dto.DeleteCourseRequest;
import com.olp.dto.PageResponse;
import com.olp.dto.UpdateCourseRequest;
import com.olp.dto.UpdateCourseStatusRequest;
import com.olp.entity.CourseInstructorRole;
import com.olp.entity.CourseStatus;
import com.olp.entity.User;
import com.olp.exception.GlobalExceptionHandler;
import com.olp.exception.ResourceNotFoundException;
import com.olp.service.CourseCompletionService;
import com.olp.service.InstructorCourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InstructorCourseControllerTest {

    @Mock
    private InstructorCourseService instructorCourseService;
    @Mock
    private CourseCompletionService courseCompletionService;

    @InjectMocks
    private InstructorCourseController controller;

    private User currentUser;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        currentUser = new User("Dr. Chen", "chen@olp.com", "hash", "instructor");
        currentUser.setId(1);
        authentication = new UsernamePasswordAuthenticationToken(currentUser, null);
    }

    private void givenResolvedUser() {
        when(instructorCourseService.resolveUser(authentication)).thenReturn(currentUser);
    }

    private CourseResponse courseResponse(Integer id, String title) {
        return new CourseResponse(id, 1, title, "Description", CourseStatus.DRAFT, LocalDateTime.now());
    }

    @Test
    void testController_RequiresInstructorRole() {
        PreAuthorize preAuthorize = InstructorCourseController.class.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasRole('instructor')", preAuthorize.value());
    }

    @Test
    void testCreateCourse_Returns201WithCreatedCourse() {
        givenResolvedUser();
        CreateCourseRequest request = new CreateCourseRequest("Spring Boot", "Learn it");
        CourseResponse created = courseResponse(10, "Spring Boot");
        when(instructorCourseService.createCourse(currentUser, request)).thenReturn(created);

        ResponseEntity<CourseResponse> response = controller.createCourse(authentication, request);

        assertEquals(201, response.getStatusCode().value());
        assertSame(created, response.getBody());
        verify(instructorCourseService, times(1)).createCourse(currentUser, request);
    }

    @Test
    void testCreateCourse_FullPayloadWithPrerequisites_Returns201() {
        givenResolvedUser();
        CreateCourseRequest request = new CreateCourseRequest(
                "Full Stack Web", "Description", "Web Development",
                List.of(new com.olp.dto.CoursePrerequisiteRequest("SKILL", "Basic HTML")),
                List.of("Build sites")
        );
        CourseResponse created = new CourseResponse(
                10, 1, "Full Stack Web", "Description", CourseStatus.DRAFT, LocalDateTime.now(),
                "Web Development", List.of("Build sites"), List.of()
        );
        when(instructorCourseService.createCourse(currentUser, request)).thenReturn(created);

        ResponseEntity<CourseResponse> response = controller.createCourse(authentication, request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("Web Development", Objects.requireNonNull(response.getBody()).getCategory());
        assertEquals(CourseStatus.DRAFT, Objects.requireNonNull(response.getBody()).getStatus());
        verify(instructorCourseService, times(1)).createCourse(currentUser, request);
    }

    @Test
    void testCreateCourse_ServiceFailurePropagates() {
        givenResolvedUser();
        CreateCourseRequest request = new CreateCourseRequest("  ", null);
        when(instructorCourseService.createCourse(currentUser, request))
                .thenThrow(new IllegalArgumentException("Course title cannot be blank"));

        assertThrows(IllegalArgumentException.class, () -> controller.createCourse(authentication, request));
    }

    @Test
    void testGetCourses_NoQueryParams_Returns200WithList() {
        givenResolvedUser();
        List<CourseResponse> courses = List.of(courseResponse(10, "A"), courseResponse(11, "B"));
        when(instructorCourseService.getInstructorCourses(currentUser)).thenReturn(courses);

        ResponseEntity<List<CourseResponse>> response = controller.getCourses(authentication, null, null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(2, Objects.requireNonNull(response.getBody()).size());
        assertSame(courses, response.getBody());
    }

    @Test
    void testGetCourses_QueryParamsMatchingCurrentUser_Allowed() {
        givenResolvedUser();
        when(instructorCourseService.getInstructorCourses(currentUser)).thenReturn(List.of());

        ResponseEntity<List<CourseResponse>> response = controller.getCourses(authentication, 1, 1);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(Objects.requireNonNull(response.getBody()).isEmpty());
    }

    @Test
    void testGetCourses_InstructorIdOfAnotherUser_ThrowsAccessDenied() {
        givenResolvedUser();

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> controller.getCourses(authentication, 99, null));

        assertTrue(ex.getMessage().contains("Cannot access another instructor's courses"));
        verify(instructorCourseService, never()).getInstructorCourses(any());
    }

    @Test
    void testGetCourses_UserIdOfAnotherUser_ThrowsAccessDenied() {
        givenResolvedUser();

        assertThrows(AccessDeniedException.class, () -> controller.getCourses(authentication, null, 99));
        verify(instructorCourseService, never()).getInstructorCourses(any());
    }

    @Test
    void testGetCourses_MatchingInstructorIdButOtherUserId_ThrowsAccessDenied() {
        givenResolvedUser();

        assertThrows(AccessDeniedException.class, () -> controller.getCourses(authentication, 1, 99));
        verify(instructorCourseService, never()).getInstructorCourses(any());
    }

    @Test
    void testUpdateCourse_Returns200WithUpdatedCourse() {
        givenResolvedUser();
        UpdateCourseRequest request = new UpdateCourseRequest("New Title", "New Desc");
        CourseResponse updated = courseResponse(10, "New Title");
        when(instructorCourseService.updateCourse(currentUser, 10, request)).thenReturn(updated);

        ResponseEntity<CourseResponse> response = controller.updateCourse(authentication, 10, request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(updated, response.getBody());
        verify(instructorCourseService, times(1)).updateCourse(currentUser, 10, request);
    }

    @Test
    void testUpdateCourse_NotFoundPropagates() {
        givenResolvedUser();
        UpdateCourseRequest request = new UpdateCourseRequest("T", "D");
        when(instructorCourseService.updateCourse(currentUser, 99, request))
                .thenThrow(new ResourceNotFoundException("Course not found with id: 99"));

        assertThrows(ResourceNotFoundException.class, () -> controller.updateCourse(authentication, 99, request));
    }

    @Test
    void testUpdateCourseStatus_Returns200WithNewStatus() {
        givenResolvedUser();
        UpdateCourseStatusRequest request = new UpdateCourseStatusRequest("PUBLISHED");
        CourseResponse updated = new CourseResponse(10, 1, "Title", "Desc", CourseStatus.PUBLISHED, LocalDateTime.now());
        when(instructorCourseService.updateCourseStatus(currentUser, 10, request)).thenReturn(updated);

        ResponseEntity<CourseResponse> response = controller.updateCourseStatus(authentication, 10, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(CourseStatus.PUBLISHED, Objects.requireNonNull(response.getBody()).getStatus());
        verify(instructorCourseService, times(1)).updateCourseStatus(currentUser, 10, request);
    }

    @Test
    void testSaveDraft_NewDraft_Returns201() {
        givenResolvedUser();
        CourseDraftRequest request = new CourseDraftRequest(null, "WIP description");
        CourseResponse draft = courseResponse(15, "Untitled Draft");
        when(instructorCourseService.saveDraft(currentUser, request)).thenReturn(draft);

        ResponseEntity<CourseResponse> response = controller.saveDraft(authentication, request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(CourseStatus.DRAFT, Objects.requireNonNull(response.getBody()).getStatus());
        verify(instructorCourseService).saveDraft(currentUser, request);
    }

    @Test
    void testSaveDraft_WithExistingIdInBody_Returns200() {
        givenResolvedUser();
        CourseDraftRequest request = new CourseDraftRequest("Updated Draft Title", null);
        request.setId(15);
        CourseResponse draft = courseResponse(15, "Updated Draft Title");
        when(instructorCourseService.saveDraft(currentUser, request)).thenReturn(draft);

        ResponseEntity<CourseResponse> response = controller.saveDraft(authentication, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Updated Draft Title", Objects.requireNonNull(response.getBody()).getTitle());
    }

    @Test
    void testUpdateDraft_Returns200() {
        givenResolvedUser();
        CourseDraftRequest request = new CourseDraftRequest(null, "Partial description");
        CourseResponse updated = courseResponse(10, "Original Title");
        when(instructorCourseService.saveDraft(currentUser, 10, request)).thenReturn(updated);

        ResponseEntity<CourseResponse> response = controller.updateDraft(authentication, 10, request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(updated, response.getBody());
        verify(instructorCourseService).saveDraft(currentUser, 10, request);
    }

    @Test
    void testGetDrafts_Returns200WithDraftList() {
        givenResolvedUser();
        List<CourseResponse> drafts = List.of(courseResponse(10, "Draft 1"));
        when(instructorCourseService.getInstructorDrafts(currentUser)).thenReturn(drafts);

        ResponseEntity<List<CourseResponse>> response = controller.getDrafts(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
        verify(instructorCourseService).getInstructorDrafts(currentUser);
    }

    @Test
    void testGetCourseById_Returns200WithCourse() {
        givenResolvedUser();
        CourseResponse course = courseResponse(10, "Draft Course");
        when(instructorCourseService.getInstructorCourseById(currentUser, 10)).thenReturn(course);

        ResponseEntity<CourseResponse> response = controller.getCourseById(authentication, 10);

        assertEquals(200, response.getStatusCode().value());
        assertSame(course, response.getBody());
        verify(instructorCourseService).getInstructorCourseById(currentUser, 10);
    }

    @Test
    void testGetCourseInstructors_Returns200AndAllowsAdminOrInstructor() throws NoSuchMethodException {
        givenResolvedUser();
        CourseInstructorResponse ownerDto = new CourseInstructorResponse(
                1, "Dr. Chen", "chen@olp.com", CourseInstructorRole.OWNER,
                "Ph.D.", "AI", "Bio", null, LocalDateTime.now());
        when(instructorCourseService.getCourseInstructors(currentUser, 10)).thenReturn(List.of(ownerDto));

        ResponseEntity<List<CourseInstructorResponse>> response = controller.getCourseInstructors(authentication, 10);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
        assertEquals(CourseInstructorRole.OWNER, Objects.requireNonNull(response.getBody()).get(0).getRole());

        var method = InstructorCourseController.class.getMethod("getCourseInstructors", Authentication.class, Integer.class);
        PreAuthorize methodAuth = method.getAnnotation(PreAuthorize.class);
        assertNotNull(methodAuth);
        assertEquals("hasRole('instructor') or hasRole('admin')", methodAuth.value());
    }

    @Test
    void testRemoveCourseInstructor_Returns200() {
        givenResolvedUser();

        ResponseEntity<ApiResponse> response = controller.removeCourseInstructor(authentication, 10, 2);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
        verify(instructorCourseService).removeCourseInstructor(currentUser, 10, 2);
    }

    @Test
    void testArchiveCourse_Returns200() {
        givenResolvedUser();
        CourseResponse archived = new CourseResponse(10, 1, "Title", "Desc", CourseStatus.ARCHIVED, LocalDateTime.now());
        when(instructorCourseService.archiveCourse(currentUser, 10)).thenReturn(archived);

        ResponseEntity<CourseResponse> response = controller.archiveCourse(authentication, 10);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(CourseStatus.ARCHIVED, Objects.requireNonNull(response.getBody()).getStatus());
        verify(instructorCourseService).archiveCourse(currentUser, 10);
    }

    @Test
    void testDeleteCourse_Returns200() {
        givenResolvedUser();

        ResponseEntity<ApiResponse> response = controller.deleteCourse(authentication, 10);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
        verify(instructorCourseService).deleteCourse(currentUser, 10);
    }

    @Test
    void testDeleteCourse_WithConfirmParam_PassesConfirmationToService() {
        givenResolvedUser();

        ResponseEntity<ApiResponse> response = controller.deleteCourse(authentication, 10, true, null, null);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
        verify(instructorCourseService).deleteCourse(currentUser, 10, true, null);
    }

    @Test
    void testDeleteCourse_WithRequestBody_PassesConfirmationAndReasonToService() {
        givenResolvedUser();
        DeleteCourseRequest req = new DeleteCourseRequest(true, "Retiring course");

        ResponseEntity<ApiResponse> response = controller.deleteCourse(authentication, 10, null, null, req);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
        verify(instructorCourseService).deleteCourse(currentUser, 10, true, "Retiring course");
    }

    @Test
    void testGetMyCourses_PagedAndFiltered_Returns200() {
        givenResolvedUser();
        CourseResponse c = courseResponse(10, "Published Course");
        c.setStatus(CourseStatus.PUBLISHED);
        c.setEnrollmentCount(12L);
        PageResponse<CourseResponse> paged = new PageResponse<>(List.of(c), 0, 5, 1, 1, true);
        when(instructorCourseService.getMyCourses(currentUser, "PUBLISHED", "Pub", 0, 5)).thenReturn(paged);

        ResponseEntity<PageResponse<CourseResponse>> response =
                controller.getMyCourses(authentication, "PUBLISHED", "Pub", 0, 5, null, null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, Objects.requireNonNull(response.getBody()).getContent().size());
        assertEquals(12L, Objects.requireNonNull(response.getBody()).getContent().get(0).getEnrollmentCount());
        verify(instructorCourseService).getMyCourses(currentUser, "PUBLISHED", "Pub", 0, 5);
    }

    @Test
    void testGetMyCourses_OtherInstructorId_ThrowsAccessDenied() {
        givenResolvedUser();

        assertThrows(AccessDeniedException.class,
                () -> controller.getMyCourses(authentication, null, null, 0, 10, 99, null));
        verify(instructorCourseService, never()).getMyCourses(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void testMyCoursesRoutes_MockMvcDisambiguatesListVsPagedVsStatusFilters() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        when(instructorCourseService.resolveUser(any(Authentication.class))).thenReturn(currentUser);

        CourseResponse course = courseResponse(10, "Spring API");
        course.setStatus(CourseStatus.PUBLISHED);
        course.setEnrollmentCount(25L);
        PageResponse<CourseResponse> paged = new PageResponse<>(List.of(course), 0, 5, 1, 1, true);

        when(instructorCourseService.getMyCourses(currentUser, "PUBLISHED", null, 0, 5)).thenReturn(paged);
        when(instructorCourseService.getMyCourses(currentUser, "DRAFT", null, 0, 10))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0, true));

        mockMvc.perform(get("/api/instructor/courses/my-courses")
                        .param("status", "PUBLISHED")
                        .param("page", "0")
                        .param("size", "5")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].enrollmentCount").value(25))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/instructors/courses")
                        .param("page", "0")
                        .param("size", "5")
                        .param("status", "PUBLISHED")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Spring API"));

        mockMvc.perform(get("/api/instructor/courses")
                        .param("status", "DRAFT")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void testGetCoursePricing_Returns200WithPricingBreakdown() {
        givenResolvedUser();
        CoursePricingResponse pricing = new CoursePricingResponse();
        pricing.setCourseId(10);
        pricing.setCourseTitle("Spring API");
        pricing.setBasePrice(new BigDecimal("999.00"));
        pricing.setFinalPayableAmount(new BigDecimal("1178.82"));

        when(instructorCourseService.getCoursePricing(currentUser, 10)).thenReturn(pricing);

        ResponseEntity<CoursePricingResponse> response = controller.getCoursePricing(authentication, 10);

        assertEquals(200, response.getStatusCode().value());
        assertSame(pricing, response.getBody());
        verify(instructorCourseService).getCoursePricing(currentUser, 10);
    }

    @Test
    void testUpdateCoursePricing_Returns200WithPricingBreakdown() {
        givenResolvedUser();
        CoursePricingRequest request = new CoursePricingRequest(
                new BigDecimal("500.00"), null, null, null, null, 100
        );
        CoursePricingResponse pricing = new CoursePricingResponse();
        pricing.setCourseId(10);
        pricing.setBasePrice(new BigDecimal("500.00"));
        pricing.setEnrollmentLimit(100);

        when(instructorCourseService.updateCoursePricing(currentUser, 10, request)).thenReturn(pricing);

        ResponseEntity<CoursePricingResponse> response = controller.updateCoursePricing(authentication, 10, request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(pricing, response.getBody());
        verify(instructorCourseService).updateCoursePricing(currentUser, 10, request);
    }

    @Test
    void testPreviewCourse_Returns200WithPreviewDetails() {
        givenResolvedUser();
        CoursePreviewResponse preview = new CoursePreviewResponse();
        preview.setId(10);
        preview.setTitle("Spring Boot Deep Dive");
        preview.setStatus("DRAFT");
        preview.setPreview(true);
        preview.setReadOnly(true);
        preview.setCanEnroll(false);

        when(instructorCourseService.previewCourseAsStudent(currentUser, 10)).thenReturn(preview);

        ResponseEntity<CoursePreviewResponse> response = controller.previewCourse(authentication, 10);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertSame(preview, response.getBody());
        assertTrue(Objects.requireNonNull(response.getBody()).isPreview());
        assertTrue(Objects.requireNonNull(response.getBody()).isReadOnly());
        assertFalse(Objects.requireNonNull(response.getBody()).isCanEnroll());
        verify(instructorCourseService).previewCourseAsStudent(currentUser, 10);
    }

    @Test
    void testSubmitCourse_Returns200WithSubmissionResponse() {
        givenResolvedUser();
        CourseSubmissionRequest request = new CourseSubmissionRequest("Ready for review");
        CourseSubmissionResponse submission = new CourseSubmissionResponse();
        submission.setId(1);
        submission.setCourseId(10);
        submission.setStatus("PENDING_REVIEW");
        submission.setComments("Ready for review");

        when(instructorCourseService.submitCourseForReview(eq(currentUser), eq(10), any(CourseSubmissionRequest.class)))
                .thenReturn(submission);

        ResponseEntity<CourseSubmissionResponse> response = controller.submitCourse(authentication, 10, request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertSame(submission, response.getBody());
        assertEquals("PENDING_REVIEW", Objects.requireNonNull(response.getBody()).getStatus());
        verify(instructorCourseService).submitCourseForReview(eq(currentUser), eq(10), any(CourseSubmissionRequest.class));
    }

    @Test
    void testGetCourseSubmissions_Returns200WithSubmissionList() {
        givenResolvedUser();
        CourseSubmissionResponse submission = new CourseSubmissionResponse();
        submission.setId(1);
        submission.setCourseId(10);

        when(instructorCourseService.getCourseSubmissions(currentUser, 10))
                .thenReturn(List.of(submission));

        ResponseEntity<List<CourseSubmissionResponse>> response = controller.getCourseSubmissions(authentication, 10);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
        assertSame(submission, Objects.requireNonNull(response.getBody()).get(0));
        verify(instructorCourseService).getCourseSubmissions(currentUser, 10);
    }

    @Test
    void testGetReviewStatus_Returns200WithReviewStatusResponse() {
        givenResolvedUser();
        CourseReviewStatusResponse statusResp = new CourseReviewStatusResponse();
        statusResp.setCourseId(10);
        statusResp.setStatus("CHANGES_REQUESTED");
        statusResp.setReviewStatus("CHANGES_REQUESTED");
        statusResp.setCanResubmit(true);
        statusResp.setAdminComments("Please adjust exercise order.");

        when(instructorCourseService.getCourseReviewStatus(currentUser, 10))
                .thenReturn(statusResp);

        ResponseEntity<CourseReviewStatusResponse> response = controller.getReviewStatus(authentication, 10);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("CHANGES_REQUESTED", Objects.requireNonNull(response.getBody()).getReviewStatus());
        assertTrue(Objects.requireNonNull(response.getBody()).isCanResubmit());
        assertEquals("Please adjust exercise order.", Objects.requireNonNull(response.getBody()).getAdminComments());
        verify(instructorCourseService).getCourseReviewStatus(currentUser, 10);
    }

    @Test
    void testResubmitCourse_Returns200WithSubmissionResponse() {
        givenResolvedUser();
        CourseSubmissionRequest request = new CourseSubmissionRequest("Fixed requested changes.");
        CourseSubmissionResponse submission = new CourseSubmissionResponse();
        submission.setId(2);
        submission.setCourseId(10);
        submission.setStatus("PENDING_REVIEW");
        submission.setComments("Fixed requested changes.");

        when(instructorCourseService.resubmitCourse(eq(currentUser), eq(10), any(CourseSubmissionRequest.class)))
                .thenReturn(submission);

        ResponseEntity<CourseSubmissionResponse> response = controller.resubmitCourse(authentication, 10, request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertSame(submission, response.getBody());
        assertEquals("PENDING_REVIEW", Objects.requireNonNull(response.getBody()).getStatus());
        verify(instructorCourseService).resubmitCourse(eq(currentUser), eq(10), any(CourseSubmissionRequest.class));
    }

    @Test
    void testUnpublishCourse_Returns200WithUnpublishedStatus() {
        givenResolvedUser();
        CourseResponse unpublished = new CourseResponse();
        unpublished.setId(10);
        unpublished.setStatus(CourseStatus.UNPUBLISHED);

        when(instructorCourseService.unpublishCourse(currentUser, 10)).thenReturn(unpublished);

        ResponseEntity<CourseResponse> response = controller.unpublishCourse(authentication, 10);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(CourseStatus.UNPUBLISHED, Objects.requireNonNull(response.getBody()).getStatus());
        verify(instructorCourseService).unpublishCourse(currentUser, 10);
    }

    @Test
    void testGetCourseAudits_Returns200WithAuditList() {
        givenResolvedUser();
        CourseAuditResponse audit = new CourseAuditResponse();
        audit.setId(1);
        audit.setCourseId(10);
        audit.setActionType("COURSE_UNPUBLISH");

        when(instructorCourseService.getCourseAudits(currentUser, 10)).thenReturn(List.of(audit));

        ResponseEntity<List<CourseAuditResponse>> response = controller.getCourseAudits(authentication, 10);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
        assertEquals("COURSE_UNPUBLISH", Objects.requireNonNull(response.getBody()).get(0).getActionType());
        verify(instructorCourseService).getCourseAudits(currentUser, 10);
    }

    @Test
    void testGetCompletionCriteria_Returns200() {
        givenResolvedUser();
        CourseCompletionCriteriaResponse mockResp = new CourseCompletionCriteriaResponse();
        mockResp.setCourseId(10);
        when(courseCompletionService.getCompletionCriteria(10, currentUser)).thenReturn(mockResp);

        ResponseEntity<CourseCompletionCriteriaResponse> response = controller.getCompletionCriteria(authentication, 10);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(10, Objects.requireNonNull(response.getBody()).getCourseId());
        verify(courseCompletionService).getCompletionCriteria(10, currentUser);
    }

    @Test
    void testConfigureCompletionCriteria_Returns200() {
        givenResolvedUser();
        CourseCompletionCriteriaRequest req = new CourseCompletionCriteriaRequest();
        req.setMinLessonCompletionPercentage(BigDecimal.valueOf(80.0));
        CourseCompletionCriteriaResponse mockResp = new CourseCompletionCriteriaResponse();
        mockResp.setCourseId(10);
        mockResp.setMinLessonCompletionPercentage(BigDecimal.valueOf(80.0));
        when(courseCompletionService.configureCompletionCriteria(currentUser, 10, req)).thenReturn(mockResp);

        ResponseEntity<CourseCompletionCriteriaResponse> response = controller.configureCompletionCriteria(authentication, 10, req);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(BigDecimal.valueOf(80.0), Objects.requireNonNull(response.getBody()).getMinLessonCompletionPercentage());
        verify(courseCompletionService).configureCompletionCriteria(currentUser, 10, req);
    }
}
