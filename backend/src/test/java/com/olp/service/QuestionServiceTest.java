package com.olp.service;

import com.olp.dto.ApiResponse;
import com.olp.dto.QuestionEvaluationRequest;
import com.olp.dto.QuestionEvaluationResult;
import com.olp.dto.QuestionRequest;
import com.olp.dto.QuestionResponse;
import com.olp.entity.*;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.QuestionRepository;
import com.olp.repository.QuizQuestionRepository;
import com.olp.repository.UserRepository;
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
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @org.mockito.Spy
    private AutoGradingService autoGradingService = new AutoGradingService();

    @InjectMocks
    private QuestionService questionService;

    private User owner;
    private User coInstructor;
    private User stranger;
    private User pendingInstructor;
    private User student;
    private User admin;
    private Course course;

    @BeforeEach
    void setUp() {
        owner = new User("Dr. Owner", "owner@olp.com", "hash", "instructor", "active");
        owner.setId(1);

        coInstructor = new User("Dr. Co", "co@olp.com", "hash", "instructor", "active");
        coInstructor.setId(2);

        stranger = new User("Dr. Stranger", "stranger@olp.com", "hash", "instructor", "active");
        stranger.setId(3);

        pendingInstructor = new User("Dr. Pending", "pending@olp.com", "hash", "instructor", "pending");
        pendingInstructor.setId(4);

        student = new User("Sam Student", "student@olp.com", "hash", "student", "active");
        student.setId(5);

        admin = new User("Admin", "admin@olp.com", "hash", "admin", "active");
        admin.setId(99);

        course = new Course(owner, "Algorithms", "Core algorithms", CourseStatus.PUBLISHED);
        course.setId(10);
    }

    private void stubSaveReturnsWithId(int id) {
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            if (q.getId() == null) {
                q.setId(id);
            }
            return q;
        });
    }

    // =========================================================================
    // FIVE QUESTION TYPES & VALIDATION
    // =========================================================================

    @Test
    void testCreateQuestion_MultipleChoice_SuccessAndValidation() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        stubSaveReturnsWithId(101);

        QuestionRequest req = new QuestionRequest();
        req.setCourseId(10);
        req.setQuestionText("What is the time complexity of binary search?");
        req.setQuestionType("MULTIPLE_CHOICE");
        req.setOptions(List.of("O(1)", "O(log n)", "O(n)", "O(n^2)"));
        req.setCorrectAnswer("o(log n)");
        req.setDefaultMarks(new BigDecimal("2.00"));
        req.setIsBankQuestion(true);

        QuestionResponse res = questionService.createQuestion(owner, req);

        assertEquals(101, res.getId());
        assertEquals("MULTIPLE_CHOICE", res.getQuestionType());
        assertEquals("O(log n)", res.getCorrectAnswer());
        assertEquals(4, res.getOptions().size());
        assertFalse(res.getRequiresManualGrading());

        // Reject multiple answers for MULTIPLE_CHOICE
        QuestionRequest multiAns = new QuestionRequest();
        multiAns.setQuestionText("Pick one");
        multiAns.setQuestionType("MULTIPLE_CHOICE");
        multiAns.setOptions(List.of("A", "B", "C"));
        multiAns.setCorrectAnswers(List.of("A", "B"));
        assertThrows(IllegalArgumentException.class, () -> questionService.createQuestion(owner, multiAns));

        // Reject option mismatch
        QuestionRequest badOpt = new QuestionRequest();
        badOpt.setQuestionText("Pick one");
        badOpt.setQuestionType("MULTIPLE_CHOICE");
        badOpt.setOptions(List.of("A", "B"));
        badOpt.setCorrectAnswer("Z");
        assertThrows(IllegalArgumentException.class, () -> questionService.createQuestion(owner, badOpt));
    }

    @Test
    void testCreateQuestion_MultipleSelect_SuccessAndValidation() {
        stubSaveReturnsWithId(102);

        QuestionRequest req = new QuestionRequest();
        req.setQuestionText("Which sorting algorithms have O(n log n) average complexity?");
        req.setQuestionType("MULTIPLE_SELECT");
        req.setOptions(List.of("Merge Sort", "Quick Sort", "Bubble Sort", "Heap Sort"));
        req.setCorrectAnswers(List.of("Merge Sort", "Quick Sort", "Heap Sort"));
        req.setDefaultMarks(new BigDecimal("3.00"));

        QuestionResponse res = questionService.createQuestion(owner, req);

        assertEquals(102, res.getId());
        assertNull(res.getCourseId()); // independent of course/quiz
        assertEquals("MULTIPLE_SELECT", res.getQuestionType());
        assertEquals(List.of("Merge Sort", "Quick Sort", "Heap Sort"), res.getCorrectAnswers());
        assertFalse(res.getRequiresManualGrading());

        // Reject empty correct answers
        QuestionRequest noAns = new QuestionRequest();
        noAns.setQuestionText("Select");
        noAns.setQuestionType("MULTIPLE_SELECT");
        noAns.setOptions(List.of("A", "B"));
        assertThrows(IllegalArgumentException.class, () -> questionService.createQuestion(owner, noAns));
    }

    @Test
    void testCreateQuestion_TrueFalse_SuccessAndValidation() {
        stubSaveReturnsWithId(103);

        QuestionRequest req = new QuestionRequest();
        req.setQuestionText("QuickSort is stable in its standard in-place implementation.");
        req.setQuestionType("TRUE_FALSE");
        req.setCorrectAnswer("false");
        req.setDefaultMarks(BigDecimal.ONE);

        QuestionResponse res = questionService.createQuestion(owner, req);

        assertEquals("TRUE_FALSE", res.getQuestionType());
        assertEquals("FALSE", res.getCorrectAnswer());
        assertEquals(List.of("TRUE", "FALSE"), res.getOptions());
        assertFalse(res.getRequiresManualGrading());

        // Reject non-boolean answer
        QuestionRequest invalid = new QuestionRequest();
        invalid.setQuestionText("Q?");
        invalid.setQuestionType("TRUE_FALSE");
        invalid.setCorrectAnswer("MAYBE");
        assertThrows(IllegalArgumentException.class, () -> questionService.createQuestion(owner, invalid));
    }

    @Test
    void testCreateQuestion_Numerical_SuccessAndValidation() {
        stubSaveReturnsWithId(104);

        QuestionRequest req = new QuestionRequest();
        req.setQuestionText("Compute the approximate value of Pi to 2 decimal places.");
        req.setQuestionType("NUMERICAL");
        req.setCorrectAnswer("3.14");
        req.setNumericalTolerance(new BigDecimal("0.02"));
        req.setDefaultMarks(new BigDecimal("4.00"));

        QuestionResponse res = questionService.createQuestion(owner, req);

        assertEquals("NUMERICAL", res.getQuestionType());
        assertEquals("3.14", res.getCorrectAnswer());
        assertEquals(0, new BigDecimal("0.02").compareTo(res.getNumericalTolerance()));
        assertFalse(res.getRequiresManualGrading());

        // Reject non-numeric answer
        QuestionRequest nonNum = new QuestionRequest();
        nonNum.setQuestionText("Value?");
        nonNum.setQuestionType("NUMERICAL");
        nonNum.setCorrectAnswer("abc");
        assertThrows(IllegalArgumentException.class, () -> questionService.createQuestion(owner, nonNum));

        // Reject negative tolerance
        QuestionRequest negTol = new QuestionRequest();
        negTol.setQuestionText("Value?");
        negTol.setQuestionType("NUMERICAL");
        negTol.setCorrectAnswer("42");
        negTol.setNumericalTolerance(new BigDecimal("-0.5"));
        assertThrows(IllegalArgumentException.class, () -> questionService.createQuestion(owner, negTol));
    }

    @Test
    void testCreateQuestion_Descriptive_SuccessValidationAndManualGrading() {
        stubSaveReturnsWithId(105);

        QuestionRequest req = new QuestionRequest();
        req.setQuestionText("Design a balanced BST deletion algorithm and analyze complexity.");
        req.setQuestionType("DESCRIPTIVE");
        req.setMaxMarks(new BigDecimal("15.00"));
        req.setGradingRubric("5 pts cases analysis, 5 pts pointer updates, 5 pts O(h) proof.");

        QuestionResponse res = questionService.createQuestion(owner, req);

        assertEquals("DESCRIPTIVE", res.getQuestionType());
        assertEquals(0, new BigDecimal("15.00").compareTo(res.getMaxMarks()));
        assertEquals("5 pts cases analysis, 5 pts pointer updates, 5 pts O(h) proof.", res.getGradingRubric());
        assertTrue(res.getRequiresManualGrading());

        // Reject missing rubric
        QuestionRequest noRubric = new QuestionRequest();
        noRubric.setQuestionText("Explain.");
        noRubric.setQuestionType("DESCRIPTIVE");
        noRubric.setMaxMarks(new BigDecimal("10.00"));
        assertThrows(IllegalArgumentException.class, () -> questionService.createQuestion(owner, noRubric));

        // Reject missing or zero max marks
        QuestionRequest zeroMarks = new QuestionRequest();
        zeroMarks.setQuestionText("Explain.");
        zeroMarks.setQuestionType("DESCRIPTIVE");
        zeroMarks.setMaxMarks(BigDecimal.ZERO);
        zeroMarks.setGradingRubric("Some rubric");
        assertThrows(IllegalArgumentException.class, () -> questionService.createQuestion(owner, zeroMarks));
    }

    // =========================================================================
    // CRUD, MANUAL GRADING ROUTING & AUTHORIZATION
    // =========================================================================

    @Test
    void testUpdateAndDeleteQuestion_IndependentAndCourseAuthorized() {
        Question standalone = new Question(
                null, owner, "Old?", QuestionType.TRUE_FALSE,
                "[\"TRUE\",\"FALSE\"]", "TRUE", BigDecimal.ONE, BigDecimal.ZERO);
        standalone.setId(200);

        when(questionRepository.findByIdWithDetails(200)).thenReturn(Optional.of(standalone));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        QuestionRequest updateReq = new QuestionRequest();
        updateReq.setQuestionText("Updated numerical question?");
        updateReq.setQuestionType("NUMERICAL");
        updateReq.setCorrectAnswer("100");
        updateReq.setNumericalTolerance(new BigDecimal("0.50"));
        updateReq.setDefaultMarks(new BigDecimal("5.00"));

        QuestionResponse updated = questionService.updateQuestion(owner, 200, updateReq);
        assertEquals("NUMERICAL", updated.getQuestionType());
        assertEquals("100", updated.getCorrectAnswer());

        // Stranger denied on standalone question
        assertThrows(AccessDeniedException.class, () -> questionService.updateQuestion(stranger, 200, updateReq));

        // Admin allowed to delete
        ApiResponse delRes = questionService.deleteQuestion(admin, 200);
        assertTrue(delRes.isSuccess());
        verify(questionRepository).delete(standalone);
    }

    @Test
    void testCourseQuestion_CoInstructorAllowed_StudentAndPendingRejected() {
        Question courseQ = new Question(
                course, owner, "Q?", QuestionType.TRUE_FALSE,
                "[\"TRUE\",\"FALSE\"]", "TRUE", BigDecimal.ONE, BigDecimal.ZERO);
        courseQ.setId(300);

        when(questionRepository.findByIdWithDetails(300)).thenReturn(Optional.of(courseQ));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 2)).thenReturn(true);

        QuestionResponse res = questionService.getQuestionById(coInstructor, 300);
        assertEquals(300, res.getId());

        assertThrows(AccessDeniedException.class, () -> questionService.getQuestionById(student, 300));
        assertThrows(AccessDeniedException.class, () -> questionService.getQuestionById(pendingInstructor, 300));

        when(questionRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> questionService.getQuestionById(owner, 999));
    }

    @Test
    void testSendForManualGrading_DescriptiveAllowed_ObjectiveRejected() {
        Question descQ = new Question(
                course, owner, "Essay?", QuestionType.DESCRIPTIVE,
                null, null, new BigDecimal("20.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, "Initial rubric", true);
        descQ.setId(401);

        Question mcqQ = new Question(
                course, owner, "MCQ?", QuestionType.MULTIPLE_CHOICE,
                "[\"A\",\"B\"]", "A", BigDecimal.ONE, BigDecimal.ZERO);
        mcqQ.setId(402);

        when(questionRepository.findByIdWithDetails(401)).thenReturn(Optional.of(descQ));
        when(questionRepository.findByIdWithDetails(402)).thenReturn(Optional.of(mcqQ));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(questionRepository.save(descQ)).thenReturn(descQ);

        QuestionResponse routed = questionService.sendForManualGrading(owner, 401, "Detailed grading rubric");
        assertTrue(routed.getRequiresManualGrading());
        assertEquals("Detailed grading rubric", routed.getGradingRubric());

        assertThrows(IllegalArgumentException.class,
                () -> questionService.sendForManualGrading(owner, 402, "Cannot manually grade MCQ"));

        when(questionRepository.findManualGradingQuestionsByInstructorId(1)).thenReturn(List.of(descQ));
        List<QuestionResponse> manualList = questionService.getManualGradingQuestions(owner, null);
        assertEquals(1, manualList.size());
        assertEquals(401, manualList.get(0).getId());
    }

    @Test
    void testEvaluateQuestion_InstructorEvaluatesOptionDirectly() {
        Question mcq = new Question(
                course, owner, "What is 2+2?", QuestionType.MULTIPLE_CHOICE,
                "[\"3\",\"4\"]", "4", new BigDecimal("5.00"), BigDecimal.ZERO);
        mcq.setId(500);

        when(questionRepository.findByIdWithDetails(500)).thenReturn(Optional.of(mcq));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));

        QuestionEvaluationResult res = questionService.evaluateQuestion(
                owner, 500, new QuestionEvaluationRequest("4"));

        assertNotNull(res);
        assertEquals(EvaluationStatus.CORRECT, res.getStatus());
        assertEquals(new BigDecimal("5.00"), res.getAwardedMarks());
    }
}

