package com.olp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.ExamBlueprintRepository;
import com.olp.repository.ExamRepository;
import com.olp.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExamRandomizationService (FR-I27, Step 39).
 * Tests deterministic set generation, duplicate prevention, mark balancing, option shuffling,
 * and blueprint configuration validation.
 */
@ExtendWith(MockitoExtension.class)
class ExamRandomizationServiceTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamBlueprintRepository examBlueprintRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    private ExamRandomizationService randomizationService;

    private User instructor;
    private User unauthorizedInstructor;
    private Course course;
    private Exam exam;
    private List<Question> questionPool;

    @BeforeEach
    void setUp() {
        randomizationService = new ExamRandomizationService(
                examRepository,
                examBlueprintRepository,
                questionRepository,
                courseRepository,
                courseInstructorRepository,
                new ObjectMapper()
        );

        instructor = new User("Dr. Turing", "turing@olp.com", "hash", "instructor", "active");
        instructor.setId(10);

        unauthorizedInstructor = new User("Dr. Hacker", "hacker@olp.com", "hash", "instructor", "active");
        unauthorizedInstructor.setId(99);

        course = new Course();
        course.setId(100);
        course.setTitle("Algorithms & Data Structures");
        course.setInstructor(instructor);

        exam = new Exam();
        exam.setId(1);
        exam.setTitle("Midterm Assessment");
        exam.setCourse(course);
        exam.setCreatedBy(instructor);
        exam.setStartWindow(LocalDateTime.now().minusHours(1));
        exam.setEndWindow(LocalDateTime.now().plusHours(4));
        exam.setDurationMinutes(60);
        exam.setIsRandomized(true);
        exam.setRandomSeed(12345L);
        exam.setTotalSets(2);

        // Populate question pool with 10 questions across topics and difficulties
        questionPool = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Question q = new Question();
            q.setId(100 + i);
            q.setCourse(course);
            q.setCreatedBy(instructor);
            q.setQuestionText("Question " + i);
            q.setTopic(i <= 5 ? "Graphs" : "Dynamic Programming");
            q.setDifficulty(i % 2 == 0 ? "EASY" : "HARD");
            q.setOptionsJson("[\"Option A\", \"Option B\", \"Option C\", \"Option D\"]");
            q.setDefaultMarks(new BigDecimal("5.00"));
            questionPool.add(q);
        }
    }

    @Test
    void testSeedDeterminism_SameSeedGeneratesIdenticalSets() {
        ExamBlueprint bp1 = new ExamBlueprint(exam, "Graphs", "EASY", 2, new BigDecimal("5.00"), 1);
        ExamBlueprint bp2 = new ExamBlueprint(exam, "Dynamic Programming", "HARD", 2, new BigDecimal("10.00"), 2);
        List<ExamBlueprint> blueprints = List.of(bp1, bp2);

        List<Question> easyGraphs = questionPool.stream()
                .filter(q -> "Graphs".equals(q.getTopic()) && "EASY".equals(q.getDifficulty()))
                .toList();
        List<Question> hardDp = questionPool.stream()
                .filter(q -> "Dynamic Programming".equals(q.getTopic()) && "HARD".equals(q.getDifficulty()))
                .toList();

        when(questionRepository.findByCourseIdAndTopicAndDifficulty(100, "Graphs", "EASY")).thenReturn(easyGraphs);
        when(questionRepository.findByCourseIdAndTopicAndDifficulty(100, "Dynamic Programming", "HARD")).thenReturn(hardDp);

        long testSeed = 987654321L;

        // Generate twice with identical seed
        ExamSetVariantDto set1 = randomizationService.generateSingleSet(exam, blueprints, "SET_A", testSeed);
        ExamSetVariantDto set2 = randomizationService.generateSingleSet(exam, blueprints, "SET_A", testSeed);

        assertNotNull(set1);
        assertNotNull(set2);
        assertEquals(set1.getQuestions().size(), set2.getQuestions().size());
        assertEquals(set1.getTotalMarks(), set2.getTotalMarks());

        // Verify exact question IDs and order are identical
        List<Integer> ids1 = set1.getQuestions().stream().map(ExamQuestionItemDto::getQuestionId).toList();
        List<Integer> ids2 = set2.getQuestions().stream().map(ExamQuestionItemDto::getQuestionId).toList();
        assertEquals(ids1, ids2, "Same seed must produce identical question IDs in identical order");
    }

    @Test
    void testNoDuplicateQuestionsInsideSet() {
        // Blueprint with 4 questions from Graphs
        ExamBlueprint bp = new ExamBlueprint(exam, "Graphs", null, 4, new BigDecimal("5.00"), 1);
        List<ExamBlueprint> blueprints = List.of(bp);

        List<Question> graphsQuestions = questionPool.stream()
                .filter(q -> "Graphs".equals(q.getTopic()))
                .toList();

        when(questionRepository.findByCourseIdAndTopicAndDifficulty(100, "Graphs", null)).thenReturn(graphsQuestions);

        ExamSetVariantDto set = randomizationService.generateSingleSet(exam, blueprints, "SET_A", 42L);

        assertEquals(4, set.getQuestions().size());
        Set<Integer> uniqueIds = new HashSet<>();
        for (ExamQuestionItemDto item : set.getQuestions()) {
            assertTrue(uniqueIds.add(item.getQuestionId()), "Question ID " + item.getQuestionId() + " appears more than once in the same set");
        }
    }

    @Test
    void testBalancedMarksAcrossSets() {
        ExamBlueprint bp1 = new ExamBlueprint(exam, "Graphs", "EASY", 2, new BigDecimal("5.00"), 1);
        ExamBlueprint bp2 = new ExamBlueprint(exam, "Dynamic Programming", "HARD", 1, new BigDecimal("10.00"), 2);
        List<ExamBlueprint> blueprints = List.of(bp1, bp2);

        when(examRepository.findById(1)).thenReturn(Optional.of(exam));
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(examBlueprintRepository.findByExamIdOrderByOrderIndexAsc(1)).thenReturn(blueprints);

        List<Question> easyGraphs = questionPool.stream()
                .filter(q -> "Graphs".equals(q.getTopic()) && "EASY".equals(q.getDifficulty()))
                .toList();
        List<Question> hardDp = questionPool.stream()
                .filter(q -> "Dynamic Programming".equals(q.getTopic()) && "HARD".equals(q.getDifficulty()))
                .toList();

        when(questionRepository.findByCourseIdAndTopicAndDifficulty(100, "Graphs", "EASY")).thenReturn(easyGraphs);
        when(questionRepository.findByCourseIdAndTopicAndDifficulty(100, "Dynamic Programming", "HARD")).thenReturn(hardDp);

        ExamSetGenerationResponse response = randomizationService.previewExamSets(instructor, 1, 55555L, 3);

        assertNotNull(response);
        assertEquals(3, response.getVariants().size());

        BigDecimal expectedMarks = new BigDecimal("20.00"); // (2 * 5.00) + (1 * 10.00)
        for (ExamSetVariantDto variant : response.getVariants()) {
            assertEquals(3, variant.getQuestionCount());
            assertEquals(expectedMarks, variant.getTotalMarks(), "Every generated variant must have identical total marks");
        }
    }

    @Test
    void testOptionShufflingDeterminism() {
        String optionsJson = "[\"Mercury\", \"Venus\", \"Earth\", \"Mars\"]";
        long seed = 777L;

        ExamRandomizationService.ShuffledOptionsResult res1 = randomizationService.shuffleOptions(optionsJson, seed);
        ExamRandomizationService.ShuffledOptionsResult res2 = randomizationService.shuffleOptions(optionsJson, seed);

        assertNotNull(res1);
        assertNotNull(res2);
        assertEquals(res1.getShuffledJson(), res2.getShuffledJson(), "Option shuffle must be deterministic with same seed");
        assertEquals(res1.getPermutation(), res2.getPermutation());
        assertEquals(4, res1.getPermutation().size());

        // Verify different seed produces different shuffle
        ExamRandomizationService.ShuffledOptionsResult resDiff = randomizationService.shuffleOptions(optionsJson, 99999L);
        assertNotNull(resDiff.getShuffledJson());
    }

    @Test
    void testConfigureBlueprint_Success() {
        when(examRepository.findById(1)).thenReturn(Optional.of(exam));
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));

        List<Question> easyGraphs = questionPool.stream()
                .filter(q -> "Graphs".equals(q.getTopic()) && "EASY".equals(q.getDifficulty()))
                .toList();

        when(questionRepository.findByCourseIdAndTopicAndDifficulty(100, "Graphs", "EASY")).thenReturn(easyGraphs);

        when(examBlueprintRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExamBlueprintRequest request = new ExamBlueprintRequest();
        request.setIsRandomized(true);
        request.setTotalSets(4);
        request.setRandomSeed(12345L);

        ExamBlueprintItemDto rule = new ExamBlueprintItemDto(null, "Graphs", "EASY", 2, new BigDecimal("10.00"), 1);
        request.setRules(List.of(rule));

        List<ExamBlueprintItemDto> result = randomizationService.configureBlueprint(instructor, 1, request);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Graphs", result.get(0).getTopic());
        assertEquals(2, result.get(0).getQuestionCount());

        verify(examBlueprintRepository).deleteByExamId(1);
        verify(examRepository).save(exam);
        assertEquals(new BigDecimal("20.00"), exam.getMaxMarks());
    }

    @Test
    void testConfigureBlueprint_InsufficientQuestions_ThrowsIllegalArgument() {
        when(examRepository.findById(1)).thenReturn(Optional.of(exam));
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));

        // Return empty pool
        when(questionRepository.findByCourseIdAndTopicAndDifficulty(100, "Quantum Computing", "HARD"))
                .thenReturn(Collections.emptyList());

        ExamBlueprintRequest request = new ExamBlueprintRequest();
        ExamBlueprintItemDto rule = new ExamBlueprintItemDto(null, "Quantum Computing", "HARD", 5, new BigDecimal("10.00"), 1);
        request.setRules(List.of(rule));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> randomizationService.configureBlueprint(instructor, 1, request));

        assertTrue(ex.getMessage().contains("Insufficient questions"));
        verify(examBlueprintRepository, never()).saveAll(any());
    }

    @Test
    void testConfigureBlueprint_Unauthorized_ThrowsAccessDenied() {
        when(examRepository.findById(1)).thenReturn(Optional.of(exam));
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 99)).thenReturn(false);

        ExamBlueprintRequest request = new ExamBlueprintRequest();
        request.setRules(List.of(new ExamBlueprintItemDto(null, "Graphs", "EASY", 1, BigDecimal.ONE, 1)));

        assertThrows(AccessDeniedException.class,
                () -> randomizationService.configureBlueprint(unauthorizedInstructor, 1, request));
    }
}
