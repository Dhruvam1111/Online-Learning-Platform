package com.olp.service;

import com.olp.entity.*;
import com.olp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class LectureContextPyramidService {

    private static final Logger log = LoggerFactory.getLogger(LectureContextPyramidService.class);

    private final LessonRepository lessonRepository;
    private final LectureNotesDocumentRepository notesDocumentRepository;
    private final CoursePrerequisiteRepository prerequisiteRepository;

    public LectureContextPyramidService(LessonRepository lessonRepository,
                                         LectureNotesDocumentRepository notesDocumentRepository,
                                         CoursePrerequisiteRepository prerequisiteRepository) {
        this.lessonRepository = lessonRepository;
        this.notesDocumentRepository = notesDocumentRepository;
        this.prerequisiteRepository = prerequisiteRepository;
    }

    public Map<String, Object> assembleContextPyramid(Integer lessonId,
                                                      Integer notesDocumentId,
                                                      String optionalTranscript,
                                                      String customFocus,
                                                      Integer questionCount,
                                                      Boolean includeFlashcards) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found with id: " + lessonId));

        CourseSection section = lesson.getSection();
        CourseModule module = section != null ? section.getModule() : null;
        Course course = module != null ? module.getCourse() : null;

        if (course == null) {
            throw new IllegalStateException("Lesson hierarchy is incomplete for lesson id: " + lessonId);
        }

        // --- Tier 1: Global Course & Curriculum Context ---
        Map<String, Object> courseContext = new LinkedHashMap<>();
        courseContext.put("course_id", course.getId());
        courseContext.put("title", course.getTitle() != null ? course.getTitle() : "Untitled Course");
        courseContext.put("department_subject", course.getCategory() != null ? course.getCategory() : "Computer Science");
        courseContext.put("difficulty_level", "Undergraduate / Graduate");
        courseContext.put("description", course.getDescription() != null ? course.getDescription() : "");

        List<String> clos = course.getLearningOutcomesList();
        courseContext.put("learning_objectives", (clos != null && !clos.isEmpty()) ? clos : List.of("Master core course concepts and practical applications"));

        List<CoursePrerequisite> prereqs = prerequisiteRepository.findByCourseIdOrderByIdAsc(course.getId());
        List<String> prereqTitles = prereqs.stream()
                .map(CoursePrerequisite::getTitle)
                .filter(Objects::nonNull)
                .toList();
        courseContext.put("prerequisites", prereqTitles);

        // Syllabus Outline
        List<Lesson> allLessonsInCourse = lessonRepository.findByCourseId(course.getId());
        List<String> syllabus = allLessonsInCourse.stream()
                .map(l -> l.getTitle() != null ? l.getTitle() : "Lesson " + l.getId())
                .toList();
        courseContext.put("syllabus_outline", syllabus);

        // --- Tier 2: Curriculum Trajectory & Lesson Series Placement ---
        int lessonIndexInCourse = 1;
        List<String> precedingContext = new ArrayList<>();
        List<String> succeedingBoundary = new ArrayList<>();

        for (int i = 0; i < allLessonsInCourse.size(); i++) {
            Lesson l = allLessonsInCourse.get(i);
            if (l.getId().equals(lesson.getId())) {
                lessonIndexInCourse = i + 1;
                // Preceding lessons (up to 2)
                for (int p = Math.max(0, i - 2); p < i; p++) {
                    precedingContext.add(allLessonsInCourse.get(p).getTitle());
                }
                // Succeeding lessons (up to 2)
                for (int s = i + 1; s < Math.min(allLessonsInCourse.size(), i + 3); s++) {
                    succeedingBoundary.add(allLessonsInCourse.get(s).getTitle());
                }
                break;
            }
        }

        Map<String, Object> trajectory = new LinkedHashMap<>();
        trajectory.put("current_module", module.getTitle() != null ? module.getTitle() : "Module " + module.getId());
        trajectory.put("current_section", section.getTitle() != null ? section.getTitle() : "Section " + section.getId());
        trajectory.put("lesson_id", lesson.getId());
        trajectory.put("lesson_title", lesson.getTitle() != null ? lesson.getTitle() : "Lesson " + lesson.getId());
        trajectory.put("lesson_index", lessonIndexInCourse);
        trajectory.put("total_lessons_in_course", allLessonsInCourse.size());
        trajectory.put("estimated_duration_minutes", lesson.getDurationMinutes() != null ? lesson.getDurationMinutes() : 45);
        trajectory.put("preceding_context", precedingContext);
        trajectory.put("succeeding_boundary", succeedingBoundary);

        // --- Tier 3: Spoken Lecture Transcript ---
        String transcript = (optionalTranscript != null && !optionalTranscript.isBlank())
                ? optionalTranscript
                : (lesson.getContent() != null ? lesson.getContent() : "");

        // --- Tier 4: Extracted Lecture Notes Document ---
        String extractedNotes = "";
        if (notesDocumentId != null) {
            Optional<LectureNotesDocument> docOpt = notesDocumentRepository.findById(notesDocumentId);
            if (docOpt.isPresent() && docOpt.get().getExtractedText() != null) {
                extractedNotes = docOpt.get().getExtractedText();
            }
        } else {
            // Check if any notes document is attached to this lesson
            List<LectureNotesDocument> docs = notesDocumentRepository.findByLessonIdOrderByCreatedAtDesc(lessonId);
            if (!docs.isEmpty() && docs.get(0).getExtractedText() != null) {
                extractedNotes = docs.get(0).getExtractedText();
            }
        }

        // --- Full Pyramid Payload ---
        Map<String, Object> pyramid = new LinkedHashMap<>();
        pyramid.put("course", courseContext);
        pyramid.put("trajectory", trajectory);
        pyramid.put("transcript_text", transcript);
        pyramid.put("extracted_notes_text", extractedNotes);
        pyramid.put("custom_focus_instructions", customFocus);
        pyramid.put("question_count", questionCount != null ? questionCount : 5);
        pyramid.put("include_flashcards", includeFlashcards != null ? includeFlashcards : true);

        return pyramid;
    }
}
