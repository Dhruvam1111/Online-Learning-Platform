package com.olp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.dto.CertificateResponse;
import com.olp.dto.CompletionEvaluationResult;
import com.olp.dto.StudentExamDtos.AttemptState;
import com.olp.dto.StudentExamDtos.ExamListItem;
import com.olp.dto.StudentExamDtos.SavedAnswer;
import com.olp.entity.User;
import com.olp.exception.GlobalExceptionHandler;
import com.olp.exception.ResourceNotFoundException;
import com.olp.service.CourseCompletionService;
import com.olp.service.StudentExamLifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CertificateAndExamControllerMockMvcTest {

    @Mock
    private CourseCompletionService completionService;

    @Mock
    private StudentExamLifecycleService examService;

    private MockMvc mockMvc;
    private Authentication studentAuth;
    private Authentication nonUserAuth;
    private User student;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new CertificateController(completionService),
                        new StudentCertificateController(completionService),
                        new StudentExamLifecycleController(examService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        student = new User("Riya", "riya@olp.test", "hash", "student");
        student.setId(42);
        studentAuth = new UsernamePasswordAuthenticationToken(student, null);
        nonUserAuth = new UsernamePasswordAuthenticationToken("anonymous", null);
    }

    @Test
    void certificateController_supportsAllThreeVerificationRoutesAnd404OnMissingCode() throws Exception {
        CertificateResponse response = new CertificateResponse();
        response.setId(7);
        response.setCertificateCode("CERT-2026-XYZ");
        response.setCourseTitle("System Design");
        response.setStudentName("Riya");

        when(completionService.verifyCertificate("CERT-2026-XYZ")).thenReturn(response);
        when(completionService.verifyCertificate("NOPE-999"))
                .thenThrow(new ResourceNotFoundException("Certificate not found"));

        mockMvc.perform(get("/api/certificates/verify/CERT-2026-XYZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificateCode").value("CERT-2026-XYZ"))
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.valid").value(true));

        mockMvc.perform(get("/api/certificates/CERT-2026-XYZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseTitle").value("System Design"));

        mockMvc.perform(get("/api/certificates/verify").param("code", "CERT-2026-XYZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentName").value("Riya"));

        mockMvc.perform(get("/api/certificates/verify/NOPE-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void studentCertificateController_handlesAuthClaimStatusAndOwnershipError() throws Exception {
        mockMvc.perform(get("/api/student/certificates").principal(nonUserAuth))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        CompletionEvaluationResult eval = new CompletionEvaluationResult();
        eval.setCourseId(10);
        eval.setEligible(true);
        eval.setLessonCompletionPercentage(100.0);
        when(completionService.getStudentCompletionStatus(student, 10)).thenReturn(eval);

        mockMvc.perform(get("/api/student/courses/10/completion-status").principal(studentAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.lessonCompletionPercentage").value(100.0));

        CertificateResponse issued = new CertificateResponse();
        issued.setId(99);
        issued.setCertificateCode("CERT-NEW-99");
        when(completionService.issueCertificate(42, 10)).thenReturn(issued);

        mockMvc.perform(post("/api/student/courses/10/certificate/claim").principal(studentAuth))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.certificateCode").value("CERT-NEW-99"));

        when(completionService.getStudentCertificate(student, 55))
                .thenThrow(new AccessDeniedException("Cannot view another student's certificate"));

        mockMvc.perform(get("/api/student/certificates/55").principal(studentAuth))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void studentExamLifecycleController_validatesAuthDtoValidationAndConflictStatuses() throws Exception {
        mockMvc.perform(get("/api/student/exams"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        // @NotNull validation failure on questionId / answerText
        mockMvc.perform(put("/api/student/exams/attempts/12/answers")
                        .principal(studentAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(examService);

        SavedAnswer saved = new SavedAnswer(3, "Polymorphism explanation", LocalDateTime.now());
        when(examService.save(eq(student), eq(12), any())).thenReturn(saved);

        mockMvc.perform(put("/api/student/exams/attempts/12/answers")
                        .principal(studentAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":3,\"answerText\":\"Polymorphism explanation\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value(3));

        when(examService.list(student, "OPEN")).thenReturn(List.of(
                new ExamListItem(1, 10, "Midterm", "OPEN", "No cheating",
                        LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(55),
                        60, new BigDecimal("100"), new BigDecimal("50"), true, true)));

        mockMvc.perform(get("/api/student/exams").param("state", "OPEN").principal(studentAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Midterm"));

        when(examService.submit(student, 12)).thenThrow(new IllegalStateException("Attempt already submitted"));
        mockMvc.perform(post("/api/student/exams/attempts/12/submit").principal(studentAuth))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        AttemptState completed = new AttemptState(12, 1, "SUBMITTED",
                LocalDateTime.now(), LocalDateTime.now(), 0L, List.of());
        when(examService.attempt(student, 12)).thenReturn(completed);
        mockMvc.perform(get("/api/student/exams/attempts/12").principal(studentAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }
}
