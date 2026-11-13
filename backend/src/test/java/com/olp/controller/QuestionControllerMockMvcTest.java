package com.olp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.dto.QuestionRequest;
import com.olp.entity.Course;
import com.olp.entity.CourseStatus;
import com.olp.entity.Question;
import com.olp.entity.User;
import com.olp.exception.GlobalExceptionHandler;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.QuestionRepository;
import com.olp.repository.UserRepository;
import com.olp.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration-style MockMvc test wiring QuestionController + real QuestionService + GlobalExceptionHandler
 * to verify full HTTP request validation, question-type normalization, error statuses, and JSON payloads.
 */
@ExtendWith(MockitoExtension.class)
class QuestionControllerMockMvcTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private User instructor;
    private User student;
    private Authentication instructorAuth;
    private Authentication studentAuth;

    @BeforeEach
    void setUp() {
        QuestionService questionService = new QuestionService(
                questionRepository, courseRepository, courseInstructorRepository, userRepository);
        QuestionController controller = new QuestionController(questionService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        instructor = new User("Dr. Knuth", "knuth@olp.com", "hash", "instructor", "active");
        instructor.setId(1);
        instructorAuth = new UsernamePasswordAuthenticationToken(instructor, null);

        student = new User("Sam", "sam@olp.com", "hash", "student", "active");
        student.setId(5);
        studentAuth = new UsernamePasswordAuthenticationToken(student, null);
    }

    @Test
    void testCreateAllFiveQuestionTypes_ViaHttpJsonContract() throws Exception {
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(10);
            return q;
        });

        // 1. MULTIPLE_CHOICE
        QuestionRequest mcq = new QuestionRequest();
        mcq.setQuestionText("Capital of France?");
        mcq.setQuestionType("MULTIPLE_CHOICE");
        mcq.setOptions(List.of("Berlin", "Paris", "Rome"));
        mcq.setCorrectAnswer("Paris");

        mockMvc.perform(post("/api/instructor/questions")
                        .principal(instructorAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mcq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questionType").value("MULTIPLE_CHOICE"))
                .andExpect(jsonPath("$.correctAnswer").value("Paris"))
                .andExpect(jsonPath("$.requiresManualGrading").value(false));

        // 2. MULTIPLE_SELECT
        QuestionRequest ms = new QuestionRequest();
        ms.setQuestionText("Select prime numbers.");
        ms.setQuestionType("MULTIPLE_SELECT");
        ms.setOptions(List.of("2", "3", "4", "5"));
        ms.setCorrectAnswers(List.of("2", "3", "5"));

        mockMvc.perform(post("/api/instructor/questions")
                        .principal(instructorAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ms)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questionType").value("MULTIPLE_SELECT"))
                .andExpect(jsonPath("$.correctAnswers.length()").value(3));

        // 3. TRUE_FALSE
        QuestionRequest tf = new QuestionRequest();
        tf.setQuestionText("Java supports single class inheritance.");
        tf.setQuestionType("TRUE_FALSE");
        tf.setCorrectAnswer("true");

        mockMvc.perform(post("/api/instructor/questions")
                        .principal(instructorAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tf)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questionType").value("TRUE_FALSE"))
                .andExpect(jsonPath("$.correctAnswer").value("TRUE"));

        // 4. NUMERICAL
        QuestionRequest num = new QuestionRequest();
        num.setQuestionText("Gravitational acceleration (m/s^2)?");
        num.setQuestionType("NUMERICAL");
        num.setCorrectAnswer("9.81");
        num.setNumericalTolerance(new BigDecimal("0.05"));

        mockMvc.perform(post("/api/instructor/questions")
                        .principal(instructorAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(num)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questionType").value("NUMERICAL"))
                .andExpect(jsonPath("$.numericalTolerance").value(0.05));

        // 5. DESCRIPTIVE
        QuestionRequest desc = new QuestionRequest();
        desc.setQuestionText("Discuss CAP theorem trade-offs.");
        desc.setQuestionType("DESCRIPTIVE");
        desc.setMaxMarks(new BigDecimal("20.00"));
        desc.setGradingRubric("10 marks definitions, 10 marks real-world system trade-offs.");

        mockMvc.perform(post("/api/instructor/questions")
                        .principal(instructorAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(desc)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questionType").value("DESCRIPTIVE"))
                .andExpect(jsonPath("$.maxMarks").value(20.00))
                .andExpect(jsonPath("$.requiresManualGrading").value(true));
    }

    @Test
    void testValidationAndAuthorizationErrors_Return400And403And404() throws Exception {
        // Missing @NotBlank questionText -> 400
        mockMvc.perform(post("/api/instructor/questions")
                        .principal(instructorAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("questionType", "TRUE_FALSE"))))
                .andExpect(status().isBadRequest());

        // Student principal -> 403 Forbidden
        QuestionRequest validTf = new QuestionRequest();
        validTf.setQuestionText("Valid?");
        validTf.setQuestionType("TRUE_FALSE");
        validTf.setCorrectAnswer("TRUE");

        mockMvc.perform(post("/api/instructor/questions")
                        .principal(studentAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTf)))
                .andExpect(status().isForbidden());

        // Not found -> 404
        when(questionRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/instructor/questions/999").principal(instructorAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    void testManualGradingRoutingEndpoint_Success() throws Exception {
        Course course = new Course(instructor, "Distributed Systems", "DS", CourseStatus.PUBLISHED);
        course.setId(10);
        Question desc = new Question(
                course, instructor, "Explain Raft consensus.", com.olp.entity.QuestionType.DESCRIPTIVE,
                null, null, new BigDecimal("10.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, "Leader election + log replication", true);
        desc.setId(55);

        when(questionRepository.findByIdWithDetails(55)).thenReturn(Optional.of(desc));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(questionRepository.save(any(Question.class))).thenReturn(desc);

        mockMvc.perform(post("/api/instructor/questions/55/manual-grading")
                        .principal(instructorAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("gradingRubric", "Updated rubric"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresManualGrading").value(true))
                .andExpect(jsonPath("$.gradingRubric").value("Updated rubric"));
    }
}

