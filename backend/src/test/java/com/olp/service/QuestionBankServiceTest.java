package com.olp.service;

import com.olp.dto.QuestionBankFilterRequest;
import com.olp.dto.QuestionRequest;
import com.olp.dto.QuestionResponse;
import com.olp.entity.*;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FR-I24: Question Bank and Question Versioning.
 */
@ExtendWith(MockitoExtension.class)
class QuestionBankServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private AutoGradingService autoGradingService = new AutoGradingService();

    @InjectMocks
    private QuestionService questionService;

    private User instructor;
    private User otherInstructor;
    private Course courseA;
    private Course courseB;

    @BeforeEach
    void setUp() {
        instructor = new User("Dr. Alpha", "alpha@olp.com", "hash", "instructor", "active");
        instructor.setId(10);

        otherInstructor = new User("Dr. Beta", "beta@olp.com", "hash", "instructor", "active");
        otherInstructor.setId(20);

        courseA = new Course(instructor, "Data Structures", "Trees, Graphs", CourseStatus.PUBLISHED);
        courseA.setId(100);

        courseB = new Course(instructor, "Operating Systems", "Concurrency, Virtual Memory", CourseStatus.PUBLISHED);
        courseB.setId(200);
    }

    private Question buildBaseQuestion(Integer id, Course course, String text, String topic, String tags, Integer version, boolean isLatest) {
        Question q = new Question();
        q.setId(id);
        q.setCourse(course);
        q.setCreatedBy(instructor);
        q.setQuestionText(text);
        q.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        q.setOptionsJson("[\"Option A\",\"Option B\",\"Option C\"]");
        q.setCorrectAnswer("Option A");
        q.setDefaultMarks(new BigDecimal("2.00"));
        q.setTopic(topic);
        q.setDifficulty("MEDIUM");
        q.setOrigin("MANUAL");
        q.setTags(tags);
        q.setIsBankQuestion(true);
        q.setVersion(version != null ? version : 1);
        q.setIsLatest(isLatest);
        return q;
    }

    @Test
    void testCreateQuestion_WithBankMetadata_Version1AndLatest() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(courseA));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(501);
            return q;
        });

        QuestionRequest request = new QuestionRequest();
        request.setCourseId(100);
        request.setQuestionText("What is a balanced binary search tree?");
        request.setQuestionType("MULTIPLE_CHOICE");
        request.setOptions(List.of("AVL Tree", "Array", "Linked List"));
        request.setCorrectAnswer("AVL Tree");
        request.setDefaultMarks(new BigDecimal("2.00"));
        request.setTopic("Trees");
        request.setDifficulty("MEDIUM");
        request.setOrigin("MANUAL");
        request.setTags("tree,balanced,avl");
        request.setIsBankQuestion(true);

        QuestionResponse response = questionService.createQuestion(instructor, request);

        assertNotNull(response);
        assertEquals(501, response.getId());
        assertEquals("Trees", response.getTopic());
        assertEquals("tree,balanced,avl", response.getTags());
        assertEquals("MANUAL", response.getOrigin());
        assertEquals(1, response.getVersion());
        assertTrue(Boolean.TRUE.equals(response.getIsLatest()));
        assertTrue(Boolean.TRUE.equals(response.getIsBankQuestion()));
    }

    @Test
    void testSearchQuestionBank_FilteringByTopicAndTags() {
        Question q1 = buildBaseQuestion(1, courseA, "Binary Search Time Complexity", "Search Algorithms", "binary-search,algorithms,complexity", 1, true);
        Question q2 = buildBaseQuestion(2, courseA, "AVL Tree Rotation", "Trees", "trees,avl,rotation", 1, true);
        Question q3 = buildBaseQuestion(3, courseA, "Dijkstra Shortest Path", "Graphs", "graphs,dijkstra,greedy", 1, true);

        when(questionRepository.findBankQuestionsAccessibleByInstructorId(instructor.getId()))
                .thenReturn(List.of(q1, q2, q3));

        QuestionBankFilterRequest filter = new QuestionBankFilterRequest();
        filter.setTag("avl");
        filter.setIsLatest(true);

        List<QuestionResponse> results = questionService.searchQuestionBank(instructor, filter);

        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getId());
        assertEquals("Trees", results.get(0).getTopic());
    }

    @Test
    void testSearchQuestionBank_KeywordSearchOverTextAndExplanation() {
        Question q1 = buildBaseQuestion(1, courseA, "What is a Red-Black Tree?", "Trees", "tree,balance", 1, true);
        q1.setExplanation("A red-black tree is a self-balancing binary search tree.");
        Question q2 = buildBaseQuestion(2, courseA, "What is QuickSort?", "Sorting", "sort,divide-conquer", 1, true);
        q2.setExplanation("Quicksort uses partitioning.");

        when(questionRepository.findBankQuestionsAccessibleByInstructorId(instructor.getId()))
                .thenReturn(List.of(q1, q2));

        QuestionBankFilterRequest filter = new QuestionBankFilterRequest();
        filter.setSearch("self-balancing");

        List<QuestionResponse> results = questionService.searchQuestionBank(instructor, filter);

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getId());
    }

    @Test
    void testUpdateQuestion_WhenNotInPublishedQuiz_UpdatesInPlace() {
        Question existing = buildBaseQuestion(10, courseA, "Old question text", "Trees", "tree", 1, true);

        when(questionRepository.findByIdWithDetails(10)).thenReturn(Optional.of(existing));
        when(courseRepository.findById(100)).thenReturn(Optional.of(courseA));
        when(quizQuestionRepository.isQuestionUsedInPublishedQuiz(10)).thenReturn(false);
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        QuestionRequest updateReq = new QuestionRequest();
        updateReq.setQuestionText("Updated question in-place");
        updateReq.setQuestionType("MULTIPLE_CHOICE");
        updateReq.setOptions(List.of("A", "B"));
        updateReq.setCorrectAnswer("A");
        updateReq.setTopic("Trees");
        updateReq.setTags("tree,updated");

        QuestionResponse response = questionService.updateQuestion(instructor, 10, updateReq);

        assertEquals("Updated question in-place", response.getQuestionText());
        assertEquals(1, response.getVersion());
        assertTrue(Boolean.TRUE.equals(response.getIsLatest()));
        verify(questionRepository, times(1)).save(existing);
    }

    @Test
    void testUpdateQuestion_WhenInPublishedQuiz_CreatesNewVersionAndPreservesOriginal() {
        Question existing = buildBaseQuestion(10, courseA, "Original question in published quiz", "Trees", "tree", 1, true);

        when(questionRepository.findByIdWithDetails(10)).thenReturn(Optional.of(existing));
        when(courseRepository.findById(100)).thenReturn(Optional.of(courseA));
        when(quizQuestionRepository.isQuestionUsedInPublishedQuiz(10)).thenReturn(true);
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            if (q.getId() == null) {
                q.setId(20);
            }
            return q;
        });

        QuestionRequest updateReq = new QuestionRequest();
        updateReq.setQuestionText("Revised question text for future quizzes");
        updateReq.setQuestionType("MULTIPLE_CHOICE");
        updateReq.setOptions(List.of("A", "B"));
        updateReq.setCorrectAnswer("A");
        updateReq.setTopic("Trees");
        updateReq.setTags("tree,v2");

        QuestionResponse response = questionService.updateQuestion(instructor, 10, updateReq);

        // Verify original question was frozen
        assertFalse(existing.isLatestVersion());

        // Verify response represents the new version
        assertEquals(20, response.getId());
        assertEquals(2, response.getVersion());
        assertEquals(10, response.getParentQuestionId());
        assertTrue(Boolean.TRUE.equals(response.getIsLatest()));
        assertEquals("Revised question text for future quizzes", response.getQuestionText());

        // Verify save was called for both old question (to freeze it) and new version
        verify(questionRepository, times(2)).save(any(Question.class));
    }

    @Test
    void testGetQuestionVersions_ReturnsFullLineage() {
        Question root = buildBaseQuestion(10, courseA, "Version 1", "Trees", "tree", 1, false);
        Question v2 = buildBaseQuestion(20, courseA, "Version 2", "Trees", "tree", 2, true);
        v2.setParentQuestion(root);

        when(questionRepository.findByIdWithDetails(20)).thenReturn(Optional.of(v2));
        when(courseRepository.findById(100)).thenReturn(Optional.of(courseA));
        when(questionRepository.findVersionLineage(10)).thenReturn(List.of(root, v2));

        List<QuestionResponse> versions = questionService.getQuestionVersions(instructor, 20);

        assertEquals(2, versions.size());
        assertEquals(10, versions.get(0).getId());
        assertEquals(1, versions.get(0).getVersion());
        assertEquals(20, versions.get(1).getId());
        assertEquals(2, versions.get(1).getVersion());
    }

    @Test
    void testCopyQuestion_CreatesIndependentCloneInTargetCourse() {
        Question source = buildBaseQuestion(10, courseA, "Source question to copy", "Trees", "tree,copy", 1, true);

        when(questionRepository.findByIdWithDetails(10)).thenReturn(Optional.of(source));
        when(courseRepository.findById(100)).thenReturn(Optional.of(courseA));
        when(courseRepository.findById(200)).thenReturn(Optional.of(courseB));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(888);
            return q;
        });

        QuestionResponse copied = questionService.copyQuestion(instructor, 10, 200);

        assertNotNull(copied);
        assertEquals(888, copied.getId());
        assertEquals(200, copied.getCourseId());
        assertEquals("BANK_COPY", copied.getOrigin());
        assertEquals(1, copied.getVersion());
        assertTrue(Boolean.TRUE.equals(copied.getIsLatest()));
        assertEquals("Source question to copy", copied.getQuestionText());
    }

    @Test
    void testAccessDenied_WhenStrangerTriesToUpdateQuestion() {
        Question question = buildBaseQuestion(10, courseA, "Protected question", "Trees", "tree", 1, true);

        when(questionRepository.findByIdWithDetails(10)).thenReturn(Optional.of(question));
        when(courseRepository.findById(100)).thenReturn(Optional.of(courseA));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, otherInstructor.getId()))
                .thenReturn(false);

        QuestionRequest req = new QuestionRequest();
        req.setQuestionText("Hacked text");

        assertThrows(AccessDeniedException.class, () ->
                questionService.updateQuestion(otherInstructor, 10, req)
        );
    }
}
