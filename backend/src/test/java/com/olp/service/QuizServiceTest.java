package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseModuleRepository courseModuleRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private QuizService quizService;

    private User owner;
    private User coInstructor;
    private User stranger;
    private User pendingInstructor;
    private User student;
    private Course course;
    private Course otherCourse;
    private CourseModule module;
    private CourseModule otherModule;
    private CourseSection section;
    private Lesson quizLesson;
    private Lesson videoLesson;

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

        course = new Course(owner, "Java Backend Engineering", "Deep dive", CourseStatus.DRAFT);
        course.setId(10);

        otherCourse = new Course(stranger, "Cooking 101", "Recipes", CourseStatus.PUBLISHED);
        otherCourse.setId(20);

        module = new CourseModule(course, "Module 1", "Basics", 1);
        module.setId(100);

        otherModule = new CourseModule(otherCourse, "Other Module", "Other", 1);
        otherModule.setId(200);

        section = new CourseSection(module, "Section 1.1", "Intro", 1);
        section.setId(1000);

        quizLesson = new Lesson(section, "Module 1 Checkpoint Quiz", LessonType.QUIZ, 1);
        quizLesson.setId(500);

        videoLesson = new Lesson(section, "Intro Video", LessonType.VIDEO, 2);
        videoLesson.setId(501);
    }

    // =========================================================================
    // CREATE QUIZ
    // =========================================================================

    @Test
    void testCreateQuiz_DefaultsToDraft_LinkedToCourse_Success() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> {
            Quiz q = inv.getArgument(0);
            q.setId(77);
            return q;
        });

        QuizRequest req = new QuizRequest();
        req.setCourseId(10);
        req.setTitle("Course Readiness Quiz");
        req.setDescription("Initial baseline");

        QuizResponse res = quizService.createQuiz(owner, req);

        assertEquals(77, res.getId());
        assertEquals(10, res.getCourseId());
        assertEquals("DRAFT", res.getStatus());
        assertEquals("Course Readiness Quiz", res.getTitle());
        assertEquals(0, res.getQuestionCount());
        assertEquals(BigDecimal.ZERO, res.getTotalMarks());
    }

    @Test
    void testCreateQuiz_CoInstructor_LinkedToModuleAndLesson_WithInlineQuestions_Success() {
        when(courseModuleRepository.findById(100)).thenReturn(Optional.of(module));
        when(lessonRepository.findById(500)).thenReturn(Optional.of(quizLesson));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 2)).thenReturn(true);
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> {
            Quiz q = inv.getArgument(0);
            if (q.getId() == null) {
                q.setId(88);
            }
            return q;
        });
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(901);
            return q;
        });
        when(quizQuestionRepository.existsByQuizIdAndQuestionId(88, 901)).thenReturn(false);
        when(quizQuestionRepository.findMaxOrderIndexByQuizId(88)).thenReturn(0);
        when(quizQuestionRepository.save(any(QuizQuestion.class))).thenAnswer(inv -> {
            QuizQuestion qq = inv.getArgument(0);
            qq.setId(301);
            return qq;
        });

        QuizQuestionRequest qqReq = new QuizQuestionRequest();
        qqReq.setQuestionText("What is JVM?");
        qqReq.setQuestionType("MULTIPLE_CHOICE");
        qqReq.setDefaultMarks(new BigDecimal("5.00"));

        QuizRequest req = new QuizRequest();
        req.setModuleId(100);
        req.setLessonId(500);
        req.setTitle("JVM Quiz");
        req.setStatus("PUBLISHED");
        req.setQuestions(List.of(qqReq));

        QuizResponse res = quizService.createQuiz(coInstructor, req);

        assertEquals(88, res.getId());
        assertEquals(10, res.getCourseId());
        assertEquals(100, res.getModuleId());
        assertEquals(500, res.getLessonId());
        assertEquals("PUBLISHED", res.getStatus());
        assertEquals(1, res.getQuestionCount());
        assertEquals(0, new BigDecimal("5.00").compareTo(res.getTotalMarks()));
    }

    @Test
    void testCreateQuiz_MissingCourseModuleAndLesson_ThrowsIllegalArgument() {
        QuizRequest req = new QuizRequest();
        req.setTitle("Orphan Quiz");

        assertThrows(IllegalArgumentException.class, () -> quizService.createQuiz(owner, req));
    }

    @Test
    void testCreateQuiz_NonQuizLessonType_ThrowsIllegalArgument() {
        when(lessonRepository.findById(501)).thenReturn(Optional.of(videoLesson));

        QuizRequest req = new QuizRequest();
        req.setCourseId(10);
        req.setLessonId(501);
        req.setTitle("Invalid Lesson Type Quiz");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> quizService.createQuiz(owner, req));
        assertTrue(ex.getMessage().contains("QUIZ"));
    }

    @Test
    void testCreateQuiz_ModuleBelongsToDifferentCourse_ThrowsIllegalArgument() {
        when(courseModuleRepository.findById(200)).thenReturn(Optional.of(otherModule));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));

        QuizRequest req = new QuizRequest();
        req.setCourseId(10);
        req.setModuleId(200);
        req.setTitle("Cross Course Quiz");

        assertThrows(IllegalArgumentException.class, () -> quizService.createQuiz(owner, req));
    }

    @Test
    void testCreateQuiz_LessonBelongsToDifferentModule_ThrowsIllegalArgument() {
        CourseModule secondModule = new CourseModule(course, "Module 2", "Second", 2);
        secondModule.setId(101);
        when(courseModuleRepository.findById(101)).thenReturn(Optional.of(secondModule));
        when(lessonRepository.findById(500)).thenReturn(Optional.of(quizLesson));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));

        QuizRequest req = new QuizRequest();
        req.setCourseId(10);
        req.setModuleId(101);
        req.setLessonId(500);
        req.setTitle("Mismatched Module Lesson Quiz");

        assertThrows(IllegalArgumentException.class, () -> quizService.createQuiz(owner, req));
    }

    @Test
    void testCreateQuiz_AuthorizationChecks() {
        QuizRequest req = new QuizRequest();
        req.setCourseId(10);
        req.setTitle("Unauthorized Quiz");

        assertThrows(AccessDeniedException.class, () -> quizService.createQuiz(student, req));
        assertThrows(AccessDeniedException.class, () -> quizService.createQuiz(pendingInstructor, req));

        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 3)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> quizService.createQuiz(stranger, req));
    }

    @Test
    void testCreateQuiz_InvalidStatusAndPassingMarks_ThrowsIllegalArgument() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));

        QuizRequest badStatus = new QuizRequest();
        badStatus.setCourseId(10);
        badStatus.setTitle("Bad Status Quiz");
        badStatus.setStatus("ARCHIVED");
        assertThrows(IllegalArgumentException.class, () -> quizService.createQuiz(owner, badStatus));

        QuizRequest badPassing = new QuizRequest();
        badPassing.setCourseId(10);
        badPassing.setTitle("Bad Marks Quiz");
        badPassing.setTotalMarks(new BigDecimal("10"));
        badPassing.setPassingMarks(new BigDecimal("15"));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> {
            Quiz q = inv.getArgument(0);
            q.setId(99);
            return q;
        });
        assertThrows(IllegalArgumentException.class, () -> quizService.createQuiz(owner, badPassing));
    }

    // =========================================================================
    // RETRIEVE, UPDATE, DELETE
    // =========================================================================

    @Test
    void testGetQuizzes_ByCourseAndStatus_Success() {
        Quiz draftQuiz = new Quiz(course, module, quizLesson, owner, "Quiz 1", "D1", null, QuizStatus.DRAFT);
        draftQuiz.setId(1);

        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(quizRepository.findByCourseIdAndStatusOrderByCreatedAtDesc(10, QuizStatus.DRAFT))
                .thenReturn(List.of(draftQuiz));
        when(quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(1)).thenReturn(List.of());

        List<QuizResponse> results = quizService.getQuizzesByCourse(owner, 10, "DRAFT");

        assertEquals(1, results.size());
        assertEquals("Quiz 1", results.get(0).getTitle());
        assertEquals("DRAFT", results.get(0).getStatus());
    }

    @Test
    void testGetMyQuizzes_AndGetByModule_Success() {
        Quiz published = new Quiz(course, module, null, owner, "Quiz P", "Desc", null, QuizStatus.PUBLISHED);
        published.setId(2);

        when(quizRepository.findByAssociatedInstructorId(1)).thenReturn(List.of(published));
        when(quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(2)).thenReturn(List.of());

        List<QuizResponse> myQuizzes = quizService.getMyQuizzes(owner, null);
        assertEquals(1, myQuizzes.size());

        when(courseModuleRepository.findById(100)).thenReturn(Optional.of(module));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(quizRepository.findByModuleIdOrderByCreatedAtDesc(100)).thenReturn(List.of(published));

        List<QuizResponse> moduleQuizzes = quizService.getQuizzesByModule(owner, 10, 100);
        assertEquals(1, moduleQuizzes.size());
    }

    @Test
    void testUpdateQuiz_AndPublish_Success() {
        Quiz existing = new Quiz(course, module, null, owner, "Old Title", "Old Desc", null, QuizStatus.DRAFT);
        existing.setId(15);

        when(quizRepository.findByIdWithDetails(15)).thenReturn(Optional.of(existing));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(lessonRepository.findById(500)).thenReturn(Optional.of(quizLesson));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> inv.getArgument(0));
        when(quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(15)).thenReturn(List.of());

        QuizRequest updateReq = new QuizRequest();
        updateReq.setTitle("Updated Quiz Title");
        updateReq.setStatus("PUBLISHED");
        updateReq.setLessonId(500);
        updateReq.setTimeLimitMinutes(20);

        QuizResponse updated = quizService.updateQuiz(owner, 15, updateReq);

        assertEquals("Updated Quiz Title", updated.getTitle());
        assertEquals("PUBLISHED", updated.getStatus());
        assertEquals(500, updated.getLessonId());
        assertEquals(20, updated.getTimeLimitMinutes());
    }

    @Test
    void testUpdateQuiz_ReassignDifferentCourse_ThrowsIllegalArgument() {
        Quiz existing = new Quiz(course, module, null, owner, "Title", null, null, QuizStatus.DRAFT);
        existing.setId(15);

        when(quizRepository.findByIdWithDetails(15)).thenReturn(Optional.of(existing));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));

        QuizRequest req = new QuizRequest();
        req.setCourseId(20);
        req.setTitle("Moved");

        assertThrows(IllegalArgumentException.class, () -> quizService.updateQuiz(owner, 15, req));
    }

    @Test
    void testDeleteQuiz_Success_AndNotFound() {
        Quiz existing = new Quiz(course, module, null, owner, "To Delete", null, null, QuizStatus.DRAFT);
        existing.setId(15);

        when(quizRepository.findByIdWithDetails(15)).thenReturn(Optional.of(existing));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));

        ApiResponse res = quizService.deleteQuiz(owner, 15);
        assertTrue(res.isSuccess());
        verify(quizRepository).delete(existing);

        when(quizRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> quizService.deleteQuiz(owner, 999));
    }

    // =========================================================================
    // QUIZ-QUESTION MAPPING & REORDERING
    // =========================================================================

    @Test
    void testAddQuestionToQuiz_ExistingBankQuestion_AndDuplicateBlocked() {
        Quiz quiz = new Quiz(course, module, null, owner, "Bank Quiz", null, null, QuizStatus.DRAFT);
        quiz.setId(30);

        Question bankQ = new Question(course, owner, "What is Spring IoC?", QuestionType.DESCRIPTIVE,
                null, null, new BigDecimal("4.00"), BigDecimal.ZERO);
        bankQ.setId(400);
        bankQ.setIsBankQuestion(true);

        when(quizRepository.findByIdWithDetails(30)).thenReturn(Optional.of(quiz));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(questionRepository.findById(400)).thenReturn(Optional.of(bankQ));
        when(quizQuestionRepository.existsByQuizIdAndQuestionId(30, 400)).thenReturn(false);
        when(quizQuestionRepository.findMaxOrderIndexByQuizId(30)).thenReturn(1);

        QuizQuestion savedLink = new QuizQuestion(quiz, bankQ, 2, new BigDecimal("6.00"), BigDecimal.ZERO, true);
        savedLink.setId(700);
        when(quizQuestionRepository.save(any(QuizQuestion.class))).thenReturn(savedLink);
        when(quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(30)).thenReturn(List.of(savedLink));
        when(quizRepository.save(any(Quiz.class))).thenReturn(quiz);

        QuizQuestionRequest req = new QuizQuestionRequest();
        req.setQuestionId(400);
        req.setMarks(new BigDecimal("6.00"));

        QuizQuestionResponse res = quizService.addQuestionToQuiz(owner, 30, req);
        assertEquals(700, res.getId());
        assertEquals(2, res.getOrderIndex());
        assertEquals(0, new BigDecimal("6.00").compareTo(res.getEffectiveMarks()));
        assertEquals(0, new BigDecimal("6.00").compareTo(quiz.getTotalMarks()));

        // Duplicate question blocked with IllegalStateException
        when(quizQuestionRepository.existsByQuizIdAndQuestionId(30, 400)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> quizService.addQuestionToQuiz(owner, 30, req));
    }

    @Test
    void testAddQuestionToQuiz_OtherCourseQuestion_ThrowsAccessDenied() {
        Quiz quiz = new Quiz(course, module, null, owner, "Quiz", null, null, QuizStatus.DRAFT);
        quiz.setId(30);

        Question foreignQ = new Question(otherCourse, stranger, "Secret recipe?", QuestionType.SHORT_ANSWER,
                null, null, BigDecimal.ONE, BigDecimal.ZERO);
        foreignQ.setId(499);

        when(quizRepository.findByIdWithDetails(30)).thenReturn(Optional.of(quiz));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(questionRepository.findById(499)).thenReturn(Optional.of(foreignQ));

        QuizQuestionRequest req = new QuizQuestionRequest();
        req.setQuestionId(499);

        assertThrows(AccessDeniedException.class, () -> quizService.addQuestionToQuiz(owner, 30, req));
    }

    @Test
    void testAddQuestionToQuiz_ReuseByCopy_ClonesQuestion() {
        Quiz quiz = new Quiz(course, module, null, owner, "Quiz", null, null, QuizStatus.DRAFT);
        quiz.setId(30);

        Question source = new Question(course, owner, "Original question", QuestionType.MULTIPLE_CHOICE,
                "[\"A\",\"B\"]", "A", new BigDecimal("2.00"), BigDecimal.ZERO);
        source.setId(400);

        when(quizRepository.findByIdWithDetails(30)).thenReturn(Optional.of(quiz));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(questionRepository.findById(400)).thenReturn(Optional.of(source));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question cloned = inv.getArgument(0);
            cloned.setId(777);
            return cloned;
        });
        when(quizQuestionRepository.save(any(QuizQuestion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(30)).thenReturn(List.of());
        when(quizRepository.save(quiz)).thenReturn(quiz);

        QuizQuestionRequest req = new QuizQuestionRequest();
        req.setQuestionId(400);
        req.setReuseMode("COPY");
        req.setMarks(new BigDecimal("2.00"));

        QuizQuestionResponse res = quizService.addQuestionToQuiz(owner, 30, req);
        assertNotNull(res);
        assertEquals(777, res.getQuestionId());
        verify(questionRepository, times(1)).save(any(Question.class));
    }

    @Test
    void testUpdateAndRemoveQuizQuestion_RenumbersAndRecalculatesTotalMarks() {
        Quiz quiz = new Quiz(course, module, null, owner, "Quiz", null, null, QuizStatus.DRAFT);
        quiz.setId(31);

        Question q1 = new Question(course, owner, "Q1", QuestionType.TRUE_FALSE, null, "TRUE", new BigDecimal("2.00"), BigDecimal.ZERO);
        q1.setId(1);
        Question q2 = new Question(course, owner, "Q2", QuestionType.NUMERICAL, null, "42", new BigDecimal("3.00"), BigDecimal.ZERO);
        q2.setId(2);

        QuizQuestion qq1 = new QuizQuestion(quiz, q1, 1, null, null, true);
        qq1.setId(11);
        QuizQuestion qq2 = new QuizQuestion(quiz, q2, 2, null, null, true);
        qq2.setId(12);

        when(quizRepository.findByIdWithDetails(31)).thenReturn(Optional.of(quiz));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(quizQuestionRepository.findByIdAndQuizId(12, 31)).thenReturn(Optional.of(qq2));
        when(quizQuestionRepository.save(qq2)).thenReturn(qq2);
        when(quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(31)).thenReturn(List.of(qq1, qq2));
        when(quizRepository.save(quiz)).thenReturn(quiz);

        QuizQuestionRequest updateReq = new QuizQuestionRequest();
        updateReq.setMarks(new BigDecimal("8.00"));

        QuizQuestionResponse updated = quizService.updateQuizQuestion(owner, 31, 12, updateReq);
        assertEquals(0, new BigDecimal("8.00").compareTo(updated.getEffectiveMarks()));
        assertEquals(0, new BigDecimal("10.00").compareTo(quiz.getTotalMarks()));

        // Remove qq1 -> qq2 renumbered to 1 and totalMarks = 8.00
        when(quizQuestionRepository.findByIdAndQuizId(11, 31)).thenReturn(Optional.of(qq1));
        ApiResponse delRes = quizService.removeQuestionFromQuiz(owner, 31, 11);

        assertTrue(delRes.isSuccess());
        assertEquals(1, qq2.getOrderIndex());
        assertEquals(0, new BigDecimal("8.00").compareTo(quiz.getTotalMarks()));
    }

    @Test
    void testReorderQuizQuestions_SuccessAndValidation() {
        Quiz quiz = new Quiz(course, module, null, owner, "Quiz", null, null, QuizStatus.DRAFT);
        quiz.setId(32);

        Question q1 = new Question(course, owner, "Q1", QuestionType.TRUE_FALSE, null, "TRUE", BigDecimal.ONE, BigDecimal.ZERO);
        q1.setId(1);
        Question q2 = new Question(course, owner, "Q2", QuestionType.TRUE_FALSE, null, "FALSE", BigDecimal.ONE, BigDecimal.ZERO);
        q2.setId(2);

        QuizQuestion qq1 = new QuizQuestion(quiz, q1, 1, null, null, true);
        qq1.setId(101);
        QuizQuestion qq2 = new QuizQuestion(quiz, q2, 2, null, null, true);
        qq2.setId(102);

        when(quizRepository.findByIdWithDetails(32)).thenReturn(Optional.of(quiz));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(32)).thenReturn(List.of(qq1, qq2));

        List<QuizQuestionResponse> reordered = quizService.reorderQuizQuestions(
                owner, 32, new ReorderStructureRequest(List.of(102, 101)));

        assertEquals(2, reordered.size());
        assertEquals(102, reordered.get(0).getId());
        assertEquals(1, reordered.get(0).getOrderIndex());
        assertEquals(101, reordered.get(1).getId());
        assertEquals(2, reordered.get(1).getOrderIndex());

        // Duplicate / mismatched rejection
        assertThrows(IllegalArgumentException.class, () ->
                quizService.reorderQuizQuestions(owner, 32, new ReorderStructureRequest(List.of(101, 101))));
        assertThrows(IllegalArgumentException.class, () ->
                quizService.reorderQuizQuestions(owner, 32, new ReorderStructureRequest(List.of(101, 999))));
    }

    // =========================================================================
    // QUIZ SETTINGS & DR-ED03 MARKS VALIDATION (FR-I20)
    // =========================================================================

    @Test
    void testConfigureQuizSettings_DrEd03ExactSum_Success() {
        Quiz quiz = new Quiz(course, module, null, owner, "Settings Quiz", null, null, QuizStatus.DRAFT);
        quiz.setId(40);

        Question q1 = new Question(course, owner, "Q1", QuestionType.MULTIPLE_CHOICE, null, "A", new BigDecimal("4.00"), BigDecimal.ZERO);
        q1.setId(1);
        Question q2 = new Question(course, owner, "Q2", QuestionType.TRUE_FALSE, null, "TRUE", new BigDecimal("2.00"), BigDecimal.ZERO);
        q2.setId(2);

        QuizQuestion qq1 = new QuizQuestion(quiz, q1, 1, null, null, true); // uses defaultMarks 4.00
        QuizQuestion qq2 = new QuizQuestion(quiz, q2, 2, new BigDecimal("6.00"), null, true); // override 6.00

        when(quizRepository.findByIdWithDetails(40)).thenReturn(Optional.of(quiz));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(40)).thenReturn(List.of(qq1, qq2));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime start = LocalDateTime.of(2026, 11, 15, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 20, 18, 0);

        QuizSettingsRequest req = new QuizSettingsRequest();
        req.setTimeLimitMinutes(45);
        req.setNumberOfQuestions(2);
        req.setTotalMarks(new BigDecimal("10.00"));
        req.setPassingMarks(new BigDecimal("5.00"));
        req.setMaxAttempts(3);
        req.setAvailableFrom(start);
        req.setAvailableUntil(end);

        QuizSettingsResponse res = quizService.configureQuizSettings(owner, 40, req);

        assertEquals(40, res.getQuizId());
        assertEquals(45, res.getTimeLimitMinutes());
        assertEquals(2, res.getNumberOfQuestions());
        assertEquals(2, res.getLinkedQuestionCount());
        assertEquals(3, res.getMaxAttempts());
        assertEquals(start, res.getAvailableFrom());
        assertEquals(end, res.getAvailableUntil());
        assertEquals(0, new BigDecimal("10.00").compareTo(res.getTotalMarks()));
        assertEquals(0, new BigDecimal("10.00").compareTo(res.getCalculatedQuestionMarksSum()));
        assertTrue(res.isMarksConsistent());
    }

    @Test
    void testConfigureQuizSettings_DrEd03MarksMismatch_ThrowsIllegalArgument() {
        Quiz quiz = new Quiz(course, module, null, owner, "Quiz", null, null, QuizStatus.DRAFT);
        quiz.setId(41);

        Question q1 = new Question(course, owner, "Q1", QuestionType.MULTIPLE_CHOICE, null, "A", new BigDecimal("5.00"), BigDecimal.ZERO);
        QuizQuestion qq1 = new QuizQuestion(quiz, q1, 1, null, null, true);

        when(quizRepository.findByIdWithDetails(41)).thenReturn(Optional.of(quiz));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(41)).thenReturn(List.of(qq1));

        QuizSettingsRequest req = new QuizSettingsRequest();
        req.setTotalMarks(new BigDecimal("10.00")); // mismatch vs 5.00
        req.setMaxAttempts(1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> quizService.configureQuizSettings(owner, 41, req));
        assertTrue(ex.getMessage().contains("DR-ED03"));
    }

    @Test
    void testConfigureQuizSettings_InvalidAttemptsDurationAndWindow_ThrowsIllegalArgument() {
        Quiz quiz = new Quiz(course, module, null, owner, "Quiz", null, null, QuizStatus.DRAFT);
        quiz.setId(42);

        when(quizRepository.findByIdWithDetails(42)).thenReturn(Optional.of(quiz));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));

        QuizSettingsRequest zeroAttempts = new QuizSettingsRequest();
        zeroAttempts.setMaxAttempts(0);
        assertThrows(IllegalArgumentException.class, () -> quizService.configureQuizSettings(owner, 42, zeroAttempts));

        QuizSettingsRequest zeroDuration = new QuizSettingsRequest();
        zeroDuration.setTimeLimitMinutes(0);
        assertThrows(IllegalArgumentException.class, () -> quizService.configureQuizSettings(owner, 42, zeroDuration));

        QuizSettingsRequest zeroQuestions = new QuizSettingsRequest();
        zeroQuestions.setNumberOfQuestions(0);
        assertThrows(IllegalArgumentException.class, () -> quizService.configureQuizSettings(owner, 42, zeroQuestions));

        QuizSettingsRequest invertedWindow = new QuizSettingsRequest();
        invertedWindow.setAvailableFrom(LocalDateTime.of(2026, 11, 20, 12, 0));
        invertedWindow.setAvailableUntil(LocalDateTime.of(2026, 11, 19, 12, 0));
        assertThrows(IllegalArgumentException.class, () -> quizService.configureQuizSettings(owner, 42, invertedWindow));
    }

    @Test
    void testGetQuizSettings_CoInstructorAllowed_StrangerDenied() {
        Quiz quiz = new Quiz(course, module, null, owner, "Quiz", null, null, QuizStatus.PUBLISHED);
        quiz.setId(43);
        quiz.setMaxAttempts(2);
        quiz.setTimeLimitMinutes(30);

        when(quizRepository.findByIdWithDetails(43)).thenReturn(Optional.of(quiz));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 2)).thenReturn(true);
        when(quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(43)).thenReturn(List.of());

        QuizSettingsResponse res = quizService.getQuizSettings(coInstructor, 43);
        assertEquals(43, res.getQuizId());
        assertEquals(2, res.getMaxAttempts());
        assertEquals(30, res.getTimeLimitMinutes());

        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 3)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> quizService.getQuizSettings(stranger, 43));
    }
}

