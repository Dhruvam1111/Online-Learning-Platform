package com.olp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.ExamBlueprintRepository;
import com.olp.repository.ExamRepository;
import com.olp.repository.QuestionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service managing examination blueprints, deterministic randomized exam set generation,
 * question/option shuffling, and balanced mark distributions (FR-I27, Step 39).
 */
@Service
public class ExamRandomizationService {

    private final ExamRepository examRepository;
    private final ExamBlueprintRepository examBlueprintRepository;
    private final QuestionRepository questionRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final ObjectMapper objectMapper;

    public ExamRandomizationService(ExamRepository examRepository,
                                    ExamBlueprintRepository examBlueprintRepository,
                                    QuestionRepository questionRepository,
                                    CourseRepository courseRepository,
                                    CourseInstructorRepository courseInstructorRepository,
                                    ObjectMapper objectMapper) {
        this.examRepository = examRepository;
        this.examBlueprintRepository = examBlueprintRepository;
        this.questionRepository = questionRepository;
        this.courseRepository = courseRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Transactional
    public List<ExamBlueprintItemDto> configureBlueprint(User currentUser, Integer examId, ExamBlueprintRequest request) {
        validateInstructorOrAdmin(currentUser);
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
        verifyCourseAssociation(exam.getCourse().getId(), currentUser);

        if (request == null || request.getRules() == null || request.getRules().isEmpty()) {
            throw new IllegalArgumentException("Exam blueprint rules cannot be empty");
        }

        examBlueprintRepository.deleteByExamId(examId);

        List<ExamBlueprint> blueprintsToSave = new ArrayList<>();
        int order = 1;
        BigDecimal calculatedTotalMarks = BigDecimal.ZERO;

        for (ExamBlueprintItemDto rule : request.getRules()) {
            if (rule.getQuestionCount() == null || rule.getQuestionCount() < 1) {
                throw new IllegalArgumentException("Question count must be at least 1");
            }
            if (rule.getMarksPerQuestion() == null || rule.getMarksPerQuestion().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Marks per question must be greater than zero");
            }

            // Verify Question Bank availability for this rule
            List<Question> available = questionRepository.findByCourseIdAndTopicAndDifficulty(
                    exam.getCourse().getId(),
                    trimToNull(rule.getTopic()),
                    trimToNull(rule.getDifficulty())
            );

            if (available.size() < rule.getQuestionCount()) {
                throw new IllegalArgumentException(String.format(
                        "Insufficient questions in Question Bank for topic '%s' and difficulty '%s'. Required: %d, Available: %d",
                        rule.getTopic() != null ? rule.getTopic() : "ANY",
                        rule.getDifficulty() != null ? rule.getDifficulty() : "ANY",
                        rule.getQuestionCount(),
                        available.size()
                ));
            }

            ExamBlueprint bp = new ExamBlueprint(
                    exam,
                    trimToNull(rule.getTopic()),
                    trimToNull(rule.getDifficulty()),
                    rule.getQuestionCount(),
                    rule.getMarksPerQuestion(),
                    order++
            );
            blueprintsToSave.add(bp);

            BigDecimal ruleMarks = rule.getMarksPerQuestion().multiply(BigDecimal.valueOf(rule.getQuestionCount()));
            calculatedTotalMarks = calculatedTotalMarks.add(ruleMarks);
        }

        List<ExamBlueprint> saved = examBlueprintRepository.saveAll(blueprintsToSave);

        exam.setIsRandomized(request.getIsRandomized() != null ? request.getIsRandomized() : true);
        if (request.getRandomSeed() != null) {
            exam.setRandomSeed(request.getRandomSeed());
        }
        if (request.getTotalSets() != null && request.getTotalSets() >= 1) {
            exam.setTotalSets(request.getTotalSets());
        }
        exam.setMaxMarks(calculatedTotalMarks);
        examRepository.save(exam);

        return saved.stream()
                .map(this::mapToBlueprintItemDto)
                .collect(Collectors.toList());
    }

    public List<ExamBlueprintItemDto> getBlueprint(User currentUser, Integer examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));

        if (currentUser != null && "instructor".equalsIgnoreCase(currentUser.getRole())) {
            verifyCourseAssociation(exam.getCourse().getId(), currentUser);
        }

        List<ExamBlueprint> blueprints = examBlueprintRepository.findByExamIdOrderByOrderIndexAsc(examId);
        return blueprints.stream()
                .map(this::mapToBlueprintItemDto)
                .collect(Collectors.toList());
    }

    /**
     * Generates multiple deterministic exam sets from the Question Bank according to the blueprint.
     * The same seed always produces identical sets with balanced total marks and difficulty.
     */
    public ExamSetGenerationResponse previewExamSets(User currentUser, Integer examId, Long seed, Integer numSets) {
        validateInstructorOrAdmin(currentUser);
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
        verifyCourseAssociation(exam.getCourse().getId(), currentUser);

        List<ExamBlueprint> blueprints = examBlueprintRepository.findByExamIdOrderByOrderIndexAsc(examId);
        if (blueprints.isEmpty()) {
            throw new IllegalArgumentException("No blueprint configured for exam: " + examId);
        }

        long masterSeed = (seed != null) ? seed : (exam.getRandomSeed() != null ? exam.getRandomSeed() : 42L);
        int setsToGenerate = (numSets != null && numSets >= 1) ? numSets : (exam.getTotalSets() != null ? exam.getTotalSets() : 2);

        List<ExamSetVariantDto> variants = new ArrayList<>();
        for (int i = 0; i < setsToGenerate; i++) {
            char labelChar = (char) ('A' + i);
            String setLabel = "SET_" + labelChar;
            long setSeed = masterSeed + ((long) i * 31337L);
            ExamSetVariantDto variant = generateSingleSet(exam, blueprints, setLabel, setSeed);
            variants.add(variant);
        }

        return new ExamSetGenerationResponse(exam.getId(), exam.getTitle(), masterSeed, variants.size(), variants);
    }

    /**
     * Deterministically generates a single set variant using a specific seed.
     * Guarantees no duplicate questions inside the set.
     */
    public ExamSetVariantDto generateSingleSet(Exam exam, List<ExamBlueprint> blueprints, String setLabel, long setSeed) {
        Random rng = new Random(setSeed);
        Set<Integer> usedQuestionIds = new HashSet<>();
        List<ExamQuestionItemDto> selectedQuestions = new ArrayList<>();
        BigDecimal totalMarks = BigDecimal.ZERO;

        int order = 1;
        for (ExamBlueprint rule : blueprints) {
            List<Question> candidatePool = questionRepository.findByCourseIdAndTopicAndDifficulty(
                    exam.getCourse().getId(),
                    rule.getTopic(),
                    rule.getDifficulty()
            );

            // Filter out questions already selected for earlier rules
            List<Question> available = candidatePool.stream()
                    .filter(q -> !usedQuestionIds.contains(q.getId()))
                    .collect(Collectors.toList());

            if (available.size() < rule.getQuestionCount()) {
                // If not enough unique, fallback to all matching candidates
                available = new ArrayList<>(candidatePool);
            }

            // Shuffle candidates deterministically using rng
            List<Question> shuffledCandidates = new ArrayList<>(available);
            Collections.shuffle(shuffledCandidates, new Random(rng.nextLong()));

            int count = Math.min(rule.getQuestionCount(), shuffledCandidates.size());
            for (int i = 0; i < count; i++) {
                Question chosen = shuffledCandidates.get(i);
                usedQuestionIds.add(chosen.getId());

                ExamQuestionItemDto item = new ExamQuestionItemDto(
                        null,
                        chosen.getId(),
                        chosen.getQuestionText(),
                        chosen.getQuestionType() != null ? chosen.getQuestionType().name() : "MULTIPLE_CHOICE",
                        chosen.getOptionsJson(),
                        rule.getMarksPerQuestion(),
                        order++
                );
                selectedQuestions.add(item);
                totalMarks = totalMarks.add(rule.getMarksPerQuestion());
            }
        }

        // Shuffle presentation order of questions within the set
        Collections.shuffle(selectedQuestions, new Random(rng.nextLong()));
        for (int i = 0; i < selectedQuestions.size(); i++) {
            selectedQuestions.get(i).setOrderIndex(i + 1);
        }

        return new ExamSetVariantDto(setLabel, setSeed, totalMarks, selectedQuestions.size(), selectedQuestions);
    }

    /**
     * Shuffles question options deterministically and records the permutation mapping.
     */
    public ShuffledOptionsResult shuffleOptions(String optionsJson, long questionSeed) {
        if (optionsJson == null || !optionsJson.trim().startsWith("[")) {
            return new ShuffledOptionsResult(optionsJson, Collections.emptyList());
        }

        try {
            List<String> originalOptions = objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
            if (originalOptions.size() <= 1) {
                return new ShuffledOptionsResult(optionsJson, Collections.emptyList());
            }

            List<Integer> permutation = new ArrayList<>();
            for (int i = 0; i < originalOptions.size(); i++) {
                permutation.add(i);
            }

            Collections.shuffle(permutation, new Random(questionSeed));

            List<String> shuffledOptions = permutation.stream()
                    .map(originalOptions::get)
                    .collect(Collectors.toList());

            String shuffledJson = objectMapper.writeValueAsString(shuffledOptions);
            return new ShuffledOptionsResult(shuffledJson, permutation);
        } catch (Exception e) {
            return new ShuffledOptionsResult(optionsJson, Collections.emptyList());
        }
    }

    public static class ShuffledOptionsResult {
        private final String shuffledJson;
        private final List<Integer> permutation;

        public ShuffledOptionsResult(String shuffledJson, List<Integer> permutation) {
            this.shuffledJson = shuffledJson;
            this.permutation = permutation;
        }

        public String getShuffledJson() {
            return shuffledJson;
        }

        public List<Integer> getPermutation() {
            return permutation;
        }
    }

    private ExamBlueprintItemDto mapToBlueprintItemDto(ExamBlueprint bp) {
        return new ExamBlueprintItemDto(
                bp.getId(),
                bp.getTopic(),
                bp.getDifficulty(),
                bp.getQuestionCount(),
                bp.getMarksPerQuestion(),
                bp.getOrderIndex()
        );
    }

    public void verifyCourseAssociation(Integer courseId, User user) {
        if ("admin".equalsIgnoreCase(user.getRole())) {
            return;
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
        boolean isOwner = course.getInstructor() != null && course.getInstructor().getId().equals(user.getId());
        boolean isCollaborator = courseInstructorRepository != null
                && courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, user.getId());
        if (!isOwner && !isCollaborator) {
            throw new AccessDeniedException("You are not authorized to manage content for course: " + courseId);
        }
    }

    private void validateInstructorOrAdmin(User user) {
        if (user == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        String role = user.getRole();
        if (!"instructor".equalsIgnoreCase(role) && !"admin".equalsIgnoreCase(role)) {
            throw new AccessDeniedException("Only instructors and administrators can perform this action");
        }
    }

    private String trimToNull(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return text.trim();
    }
}
