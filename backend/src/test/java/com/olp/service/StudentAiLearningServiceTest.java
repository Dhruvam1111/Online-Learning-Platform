package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentAiLearningServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private LectureMaterialRepository lectureMaterialRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuizAttemptAnswerRepository quizAttemptAnswerRepository;

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private AiLearningQueryRepository aiLearningQueryRepository;

    @InjectMocks
    private StudentAiLearningService aiLearningService;

    private User student;
    private Course course;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(10);
        student.setRole("student");

        course = new Course();
        course.setId(50);
        course.setTitle("Machine Learning Basics");
        course.setDescription("Introduction to ML algorithms and neural networks.");
    }

    @Test
    void testAskAssistant_EnrolledStudent_Success() {
        when(courseRepository.findById(50)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudentIdAndCourseId(10, 50)).thenReturn(true);
        when(aiLearningQueryRepository.save(any(AiLearningQuery.class))).thenAnswer(i -> {
            AiLearningQuery q = i.getArgument(0);
            q.setId(1);
            return q;
        });

        AiAskRequest request = new AiAskRequest(50, "What is gradient descent?", "GENERAL", null);
        AiAskResponse response = aiLearningService.askAssistant(student, request);

        assertNotNull(response);
        assertEquals(50, response.getCourseId());
        assertEquals("What is gradient descent?", response.getPrompt());
        assertNotNull(response.getResponse());
        verify(aiLearningQueryRepository).save(any(AiLearningQuery.class));
    }

    @Test
    void testAskAssistant_NotEnrolled_ThrowsAccessDenied() {
        when(courseRepository.findById(50)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudentIdAndCourseId(10, 50)).thenReturn(false);

        AiAskRequest request = new AiAskRequest(50, "What is overfitting?", "GENERAL", null);

        assertThrows(AccessDeniedException.class, () ->
                aiLearningService.askAssistant(student, request)
        );
    }

    @Test
    void testGenerateSummary_Success() {
        when(courseRepository.findById(50)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudentIdAndCourseId(10, 50)).thenReturn(true);
        when(aiLearningQueryRepository.save(any(AiLearningQuery.class))).thenAnswer(i -> i.getArgument(0));

        AiSummaryRequest req = new AiSummaryRequest(50, null, null);
        AiSummaryResponse res = aiLearningService.generateSummary(student, req);

        assertNotNull(res);
        assertEquals(50, res.getCourseId());
        assertNotNull(res.getSummary());
        assertFalse(res.getKeyTakeaways().isEmpty());
    }

    @Test
    void testGetQuestionExplanation_Success() {
        Question q = new Question();
        q.setId(100);
        q.setQuestionText("What does SGD stand for?");
        q.setCorrectAnswer("Stochastic Gradient Descent");
        q.setTopic("Optimization");

        when(questionRepository.findByIdWithDetails(100)).thenReturn(Optional.of(q));

        QuestionExplanationResponse res = aiLearningService.getQuestionExplanation(student, 100, null);

        assertNotNull(res);
        assertEquals(100, res.getQuestionId());
        assertEquals("Stochastic Gradient Descent", res.getCorrectAnswer());
        assertEquals("Optimization", res.getTopic());
    }

    @Test
    void testIdentifyWeakTopics_Success() {
        when(courseRepository.findById(50)).thenReturn(Optional.of(course));

        Question q = new Question();
        q.setTopic("Backpropagation");

        QuizAttemptAnswer wrongAns = new QuizAttemptAnswer();
        wrongAns.setQuestion(q);
        wrongAns.setEvaluationStatus(EvaluationStatus.INCORRECT);
        wrongAns.setAwardedMarks(BigDecimal.ZERO);

        when(quizAttemptAnswerRepository.findStudentAnswersByCourse(10, 50)).thenReturn(List.of(wrongAns));

        WeakTopicsResponse res = aiLearningService.identifyWeakTopics(student, 50);

        assertNotNull(res);
        assertEquals(50, res.getCourseId());
        assertTrue(res.getWeakTopics().contains("Backpropagation"));
    }

    @Test
    void testRecommendLearningResources_Success() {
        when(courseRepository.findById(50)).thenReturn(Optional.of(course));
        when(quizAttemptAnswerRepository.findStudentAnswersByCourse(10, 50)).thenReturn(List.of());

        Lecture lecture = new Lecture();
        lecture.setId(501);
        lecture.setTitle("Introduction to Cost Functions");
        lecture.setOrderIndex(1);

        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(50)).thenReturn(List.of(lecture));

        ResourceRecommendationResponse res = aiLearningService.recommendLearningResources(student, 50);

        assertNotNull(res);
        assertEquals(50, res.getCourseId());
        assertFalse(res.getRecommendedLectures().isEmpty());
    }
}
