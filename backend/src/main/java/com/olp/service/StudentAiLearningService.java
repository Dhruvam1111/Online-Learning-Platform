package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service providing AI-based learning assistance, content querying, video/material summaries,
 * question explanations, weak topic diagnostics, and personalized resource recommendations
 * (FR-S71, FR-S72, FR-S73, FR-S74, FR-S75, FR-S76, NFR-04, NFR-17).
 */
@Service
public class StudentAiLearningService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LectureRepository lectureRepository;
    private final LectureMaterialRepository lectureMaterialRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptAnswerRepository quizAttemptAnswerRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final AiLearningQueryRepository aiLearningQueryRepository;

    public StudentAiLearningService(CourseRepository courseRepository,
                                    EnrollmentRepository enrollmentRepository,
                                    LectureRepository lectureRepository,
                                    LectureMaterialRepository lectureMaterialRepository,
                                    QuestionRepository questionRepository,
                                    QuizAttemptAnswerRepository quizAttemptAnswerRepository,
                                    ExamAttemptRepository examAttemptRepository,
                                    AiLearningQueryRepository aiLearningQueryRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.lectureRepository = lectureRepository;
        this.lectureMaterialRepository = lectureMaterialRepository;
        this.questionRepository = questionRepository;
        this.quizAttemptAnswerRepository = quizAttemptAnswerRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.aiLearningQueryRepository = aiLearningQueryRepository;
    }

    @Transactional
    public AiAskResponse askAssistant(User student, AiAskRequest request) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (request == null || request.getCourseId() == null) {
            throw new IllegalArgumentException("Course ID is required");
        }
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be blank");
        }

        Course course = courseRepository.findById(Objects.requireNonNull(request.getCourseId()))
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + request.getCourseId()));

        // FR-S72: Permit queries only for enrolled course content (NFR-17)
        if ("student".equalsIgnoreCase(student.getRole())) {
            boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId());
            if (!isEnrolled) {
                throw new AccessDeniedException("Access denied: You must be enrolled in this course to query course content");
            }
        }

        String contextTitle = course.getTitle();
        String contextSnippet = course.getDescription() != null ? course.getDescription() : "";

        if ("LECTURE".equalsIgnoreCase(request.getContextType()) && request.getContextId() != null) {
            Lecture lecture = lectureRepository.findByIdAndCourseId(request.getContextId(), course.getId()).orElse(null);
            if (lecture != null) {
                contextTitle = lecture.getTitle();
                contextSnippet = "Lecture: " + lecture.getTitle();
            }
        }

        // Generate context-aware educational response
        String aiReply = generateAiResponse(request.getPrompt().trim(), course.getTitle(), contextTitle, contextSnippet);
        List<String> followUps = generateFollowUps(request.getPrompt().trim(), contextTitle);

        AiLearningQuery log = new AiLearningQuery(
                student, course, "CHAT_QUERY", request.getContextType(),
                request.getContextId(), request.getPrompt().trim(), aiReply
        );
        AiLearningQuery saved = aiLearningQueryRepository.save(log);

        return new AiAskResponse(
                saved.getId(), course.getId(), request.getPrompt().trim(),
                aiReply, contextTitle, followUps, saved.getCreatedAt()
        );
    }

    @Transactional
    public AiSummaryResponse generateSummary(User student, AiSummaryRequest request) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (request == null || request.getCourseId() == null) {
            throw new IllegalArgumentException("Course ID is required");
        }

        Course course = courseRepository.findById(Objects.requireNonNull(request.getCourseId()))
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + request.getCourseId()));

        // FR-S73: Permit summary only for permitted course content
        if ("student".equalsIgnoreCase(student.getRole())) {
            boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId());
            if (!isEnrolled) {
                throw new AccessDeniedException("Access denied: You must be enrolled to access course materials");
            }
        }

        String sourceType = "COURSE";
        Integer sourceId = course.getId();
        String sourceTitle = course.getTitle();

        if (request.getLectureId() != null) {
            Lecture lecture = lectureRepository.findByIdAndCourseId(request.getLectureId(), course.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + request.getLectureId()));
            sourceType = "LECTURE";
            sourceId = lecture.getId();
            sourceTitle = lecture.getTitle();
        } else if (request.getMaterialId() != null) {
            LectureMaterial material = lectureMaterialRepository.findByIdAndDeletedAtIsNull(request.getMaterialId())
                    .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + request.getMaterialId()));
            sourceType = "MATERIAL";
            sourceId = material.getId();
            sourceTitle = material.getTitle();
        }

        String summary = "Comprehensive summary for " + sourceTitle + ":\n" +
                "This module focuses on fundamental concepts and best practices in " + sourceTitle + ". " +
                "Key themes covered include architectural patterns, syntactic specifications, and practical implementations.";

        List<String> keyTakeaways = List.of(
                "Grasped core definitions and theoretical foundation of " + sourceTitle,
                "Understood structural relationships and execution flows",
                "Reviewed syntax, common pitfalls, and validation mechanisms"
        );

        List<String> suggestedReviewTopics = List.of(
                sourceTitle + " Fundamentals",
                "Advanced practical applications",
                "Self-assessment quiz preparation"
        );

        AiLearningQuery log = new AiLearningQuery(
                student, course, "SUMMARY", sourceType, sourceId, "Generate summary for: " + sourceTitle, summary
        );
        aiLearningQueryRepository.save(log);

        return new AiSummaryResponse(course.getId(), sourceType, sourceId, sourceTitle, summary, keyTakeaways, suggestedReviewTopics);
    }

    @Transactional(readOnly = true)
    public QuestionExplanationResponse getQuestionExplanation(User student, Integer questionId, Integer attemptId) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Question question = questionRepository.findByIdWithDetails(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

        if (attemptId != null) {
            ExamAttempt attempt = examAttemptRepository.findById(Objects.requireNonNull(attemptId)).orElse(null);
            if (attempt != null && !student.getId().equals(attempt.getStudent().getId())) {
                throw new AccessDeniedException("Access denied to attempt questions");
            }
        }

        // FR-S74: Provide explanation for assessment questions
        String correctAnswer = question.getCorrectAnswer() != null ? question.getCorrectAnswer() : "See rubric";
        String topic = question.getTopic() != null ? question.getTopic() : "General";

        QuestionExplanationResponse response = new QuestionExplanationResponse();
        response.setQuestionId(question.getId());
        response.setQuestionText(question.getQuestionText());
        response.setQuestionType(question.getQuestionType() != null ? question.getQuestionType().name() : "MULTIPLE_CHOICE");
        response.setTopic(topic);
        response.setCorrectAnswer(correctAnswer);
        response.setExplanation("The correct answer is '" + correctAnswer + "'. In this concept, the underlying rule requires adherence to standard specifications and correct semantics.");
        response.setKeyConcept("Key Concept: Understanding " + topic + " and evaluating required assertions systematically.");
        response.setStepByStepSolution(List.of(
                "Step 1: Analyze question prompt and identify key constraints.",
                "Step 2: Eliminate options that violate structural rules.",
                "Step 3: Confirm that '" + correctAnswer + "' fully satisfies the criteria."
        ));
        response.setRecommendedStudyTopic("Review module: " + topic);

        return response;
    }

    @Transactional(readOnly = true)
    public WeakTopicsResponse identifyWeakTopics(User student, Integer courseId) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Course course = courseRepository.findById(Objects.requireNonNull(courseId))
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        List<QuizAttemptAnswer> answers = quizAttemptAnswerRepository.findStudentAnswersByCourse(student.getId(), courseId);

        Map<String, int[]> topicStats = new LinkedHashMap<>(); // topic -> [total, correct]

        for (QuizAttemptAnswer ans : answers) {
            Question q = ans.getQuestion();
            String topic = (q != null && q.getTopic() != null && !q.getTopic().trim().isEmpty())
                    ? q.getTopic().trim() : "General";

            topicStats.putIfAbsent(topic, new int[]{0, 0});
            int[] stats = topicStats.get(topic);
            stats[0]++; // total

            if (ans.getEvaluationStatus() == EvaluationStatus.CORRECT ||
                    (ans.getAwardedMarks() != null && ans.getAwardedMarks().doubleValue() > 0)) {
                stats[1]++; // correct
            }
        }

        List<TopicAccuracyDto> breakdown = new ArrayList<>();
        List<String> weakTopics = new ArrayList<>();
        int totalQuestions = 0;
        int totalCorrect = 0;

        for (Map.Entry<String, int[]> entry : topicStats.entrySet()) {
            String topic = entry.getKey();
            int total = entry.getValue()[0];
            int correct = entry.getValue()[1];
            double accuracy = total > 0 ? (correct * 100.0) / total : 0.0;

            String status = accuracy >= 80.0 ? "STRONG" : (accuracy >= 60.0 ? "MODERATE" : "WEAK");
            if ("WEAK".equals(status)) {
                weakTopics.add(topic);
            }

            totalQuestions += total;
            totalCorrect += correct;

            breakdown.add(new TopicAccuracyDto(topic, total, correct, Math.round(accuracy * 10.0) / 10.0, status));
        }

        double overallAccuracy = totalQuestions > 0 ? (totalCorrect * 100.0) / totalQuestions : 0.0;

        String feedbackSummary;
        if (weakTopics.isEmpty()) {
            if (totalQuestions == 0) {
                feedbackSummary = "No submitted assessment attempts yet for course '" + course.getTitle() + "'. Complete quizzes to unlock weak-topic analysis.";
            } else {
                feedbackSummary = "Great job! You have demonstrated strong competency across all evaluated topics in this course.";
            }
        } else {
            feedbackSummary = "Identified " + weakTopics.size() + " topic(s) needing additional practice: "
                    + String.join(", ", weakTopics) + ". Review recommended materials and practice questions below.";
        }

        WeakTopicsResponse response = new WeakTopicsResponse();
        response.setCourseId(course.getId());
        response.setOverallAccuracy(Math.round(overallAccuracy * 10.0) / 10.0);
        response.setTotalQuestionsEvaluated(totalQuestions);
        response.setTopicBreakdown(breakdown);
        response.setWeakTopics(weakTopics);
        response.setFeedbackSummary(feedbackSummary);

        return response;
    }

    @Transactional(readOnly = true)
    public ResourceRecommendationResponse recommendLearningResources(User student, Integer courseId) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Course course = courseRepository.findById(Objects.requireNonNull(courseId))
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        WeakTopicsResponse weakTopicsRes = identifyWeakTopics(student, courseId);
        List<String> weakTopics = weakTopicsRes.getWeakTopics();

        List<ResourceRecommendationResponse.RecommendedItemDto> recLectures = new ArrayList<>();
        List<ResourceRecommendationResponse.RecommendedItemDto> recMaterials = new ArrayList<>();
        List<ResourceRecommendationResponse.RecommendedItemDto> recQuestions = new ArrayList<>();

        List<Lecture> allLectures = lectureRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        for (Lecture lec : allLectures) {
            boolean matches = weakTopics.isEmpty() || weakTopics.stream().anyMatch(t ->
                    lec.getTitle().toLowerCase().contains(t.toLowerCase()));

            if (matches && recLectures.size() < 3) {
                recLectures.add(new ResourceRecommendationResponse.RecommendedItemDto(
                        lec.getId(), lec.getTitle(), "VIDEO_LECTURE",
                        weakTopics.isEmpty() ? "General" : weakTopics.get(0), "BEGINNER",
                        "/courses/" + courseId + "/lectures/" + lec.getId()
                ));
            }
        }

        List<LectureMaterial> materials = lectureMaterialRepository.findAllActiveMaterials();
        for (LectureMaterial mat : materials) {
            if (recMaterials.size() < 3) {
                recMaterials.add(new ResourceRecommendationResponse.RecommendedItemDto(
                        mat.getId(), mat.getTitle(), "READING_MATERIAL",
                        weakTopics.isEmpty() ? "General" : weakTopics.get(0), "INTERMEDIATE",
                        mat.getFileUrl()
                ));
            }
        }

        for (String topic : weakTopics) {
            List<Question> questions = questionRepository.findByCourseIdAndTopicAndDifficulty(courseId, topic, null);
            for (Question q : questions) {
                if (recQuestions.size() < 5) {
                    recQuestions.add(new ResourceRecommendationResponse.RecommendedItemDto(
                            q.getId(), q.getQuestionText(), "PRACTICE_QUESTION",
                            topic, q.getDifficulty() != null ? q.getDifficulty() : "MEDIUM", null
                    ));
                }
            }
        }

        ResourceRecommendationResponse response = new ResourceRecommendationResponse();
        response.setCourseId(course.getId());
        response.setTargetedWeakTopics(weakTopics);
        response.setRecommendedLectures(recLectures);
        response.setRecommendedMaterials(recMaterials);
        response.setRecommendedPracticeQuestions(recQuestions);
        response.setRationale("Recommendations generated based on your assessment performance, prioritizing weak topics.");

        return response;
    }

    private String generateAiResponse(String prompt, String courseTitle, String contextTitle, String contextSnippet) {
        return "In " + courseTitle + " (" + contextTitle + "):\n\n" +
                "Regarding your question: \"" + prompt + "\"\n\n" +
                "Core Concept:\n" +
                "Based on the course syllabus, " + contextTitle + " establishes essential patterns. " +
                "When implementing solutions in this domain, ensure that all syntax is valid, proper scoping is applied, and edge cases are handled.\n\n" +
                "Key Rules to Follow:\n" +
                "1. Confirm adherence to standard specifications and syntax.\n" +
                "2. Maintain separation of concerns across modules.\n" +
                "3. Validate inputs and handle edge conditions gracefully.\n\n" +
                "Next Steps:\n" +
                "You can practice applying this concept in the course exercises or review the corresponding lecture notes.";
    }

    private List<String> generateFollowUps(String prompt, String contextTitle) {
        return List.of(
                "Can you provide a code example for " + contextTitle + "?",
                "What are the most common mistakes beginners make in " + contextTitle + "?",
                "How is this topic tested on the course quizzes?"
        );
    }
}
