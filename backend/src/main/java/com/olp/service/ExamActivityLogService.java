package com.olp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service managing examination activity logs, proctoring events, tab-switch and focus-loss tracking,
 * screen-sharing telemetry, and automated rule violation notifications
 * (FR-S52, FR-S53, FR-S54, FR-S55, FR-S56, FR-S57, FR-S58, FR-S59, FR-S60, FR-S61, DR-05, DR-06, DR-07).
 */
@Service
public class ExamActivityLogService {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamMonitoringLogRepository monitoringLogRepository;
    private final ExamMonitoringConfigRepository monitoringConfigRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExamActivityLogService(ExamAttemptRepository examAttemptRepository,
                                  ExamMonitoringLogRepository monitoringLogRepository,
                                  ExamMonitoringConfigRepository monitoringConfigRepository,
                                  ExamQuestionRepository examQuestionRepository,
                                  CourseInstructorRepository courseInstructorRepository) {
        this.examAttemptRepository = examAttemptRepository;
        this.monitoringLogRepository = monitoringLogRepository;
        this.monitoringConfigRepository = monitoringConfigRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.courseInstructorRepository = courseInstructorRepository;
    }

    @Transactional
    public ExamMonitoringEventResponse recordMonitoringEvent(User student, Integer attemptId, ExamMonitoringEventRequest request) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (request == null || request.getEventType() == null) {
            throw new IllegalArgumentException("Monitoring event details are required");
        }

        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam attempt not found: " + attemptId));

        // FR-S61: Associate monitoring events with corresponding student attempt
        if (!student.getId().equals(attempt.getStudent().getId())) {
            throw new AccessDeniedException("You do not have permission to submit events for this exam attempt");
        }

        String eventType = request.getEventType().toUpperCase().trim();
        LocalDateTime eventTime = request.getClientTimestamp() != null ? request.getClientTimestamp() : LocalDateTime.now();
        int durationSec = request.getDurationSeconds() != null ? Math.max(0, request.getDurationSeconds()) : 0;

        Exam exam = attempt.getExam();
        ExamMonitoringConfig config = monitoringConfigRepository.findByExamId(exam.getId()).orElse(null);

        int tabLimit = (config != null && config.getTabSwitchLimit() != null) ? config.getTabSwitchLimit() : 3;
        int fullscreenLimit = (config != null && config.getFullscreenExitLimit() != null) ? config.getFullscreenExitLimit() : 2;

        String severity = "INFO";
        String ruleTriggered = null;
        String warningMsg = null;

        // FR-S53, FR-S54: Focus loss / tab-change detection and duration
        if ("TAB_SWITCH".equals(eventType) || "WINDOW_BLUR".equals(eventType)) {
            int newTabCount = attempt.getTabSwitchCount() + 1;
            attempt.setTabSwitchCount(newTabCount);

            if (config != null && Boolean.TRUE.equals(config.getTabSwitchDetection())) {
                if (newTabCount >= tabLimit) {
                    severity = "VIOLATION";
                    ruleTriggered = "TAB_SWITCH_LIMIT_EXCEEDED";
                    warningMsg = "Warning: You have reached or exceeded the maximum allowed tab switches ("
                            + newTabCount + "/" + tabLimit + "). This session has been flagged for instructor review.";
                    appendMonitoringFlag(attempt, "FLAG_TAB_SWITCH_EXCEEDED");
                } else {
                    severity = "WARNING";
                    warningMsg = "Notice: Leaving the exam tab is prohibited. Remaining warnings: "
                            + (tabLimit - newTabCount);
                }
            }
        } else if ("FULLSCREEN_EXIT".equals(eventType)) {
            // FR-S58: Fullscreen mode tracking
            int newFsCount = attempt.getFullscreenExitCount() + 1;
            attempt.setFullscreenExitCount(newFsCount);

            if (config != null && Boolean.TRUE.equals(config.getRequireFullscreen())) {
                if (newFsCount >= fullscreenLimit) {
                    severity = "VIOLATION";
                    ruleTriggered = "FULLSCREEN_EXIT_LIMIT_EXCEEDED";
                    warningMsg = "Warning: Fullscreen exited " + newFsCount + " times. Limit is "
                            + fullscreenLimit + ". This event is recorded for proctor review.";
                    appendMonitoringFlag(attempt, "FLAG_FULLSCREEN_EXITS_EXCEEDED");
                } else {
                    severity = "WARNING";
                    warningMsg = "Notice: Fullscreen is required for this examination. Please return to fullscreen.";
                }
            }
        } else if ("SCREEN_SHARE_START".equals(eventType)) {
            // FR-S60: Screen sharing telemetry
            attempt.setScreenShareActive(true);
            severity = "INFO";
        } else if ("SCREEN_SHARE_STOP".equals(eventType)) {
            attempt.setScreenShareActive(false);
            severity = "WARNING";
            warningMsg = "Screen sharing stopped. Reconnect immediately to avoid examination penalties.";
        } else if ("SCREEN_SHARE_TERMINATED".equals(eventType)) {
            attempt.setScreenShareActive(false);
            severity = "VIOLATION";
            ruleTriggered = "SCREEN_SHARE_UNEXPECTED_TERMINATION";
            warningMsg = "Screen sharing was terminated unexpectedly. Attempt has been flagged.";
            appendMonitoringFlag(attempt, "FLAG_SCREEN_SHARE_TERMINATED");
        }

        // FR-S52: Maintain activity log for monitored exam attempt
        ExamMonitoringLog log = new ExamMonitoringLog(
                attempt, student, exam, eventType, eventTime, durationSec,
                request.getDetails(), severity, ruleTriggered
        );
        monitoringLogRepository.save(log);
        examAttemptRepository.save(attempt);

        // FR-S55: Notify students when monitoring rules are triggered
        ExamMonitoringEventResponse response = new ExamMonitoringEventResponse();
        response.setId(log.getId());
        response.setAttemptId(attempt.getId());
        response.setEventType(eventType);
        response.setEventTimestamp(eventTime);
        response.setDurationSeconds(durationSec);
        response.setSeverity(severity);
        response.setRuleTriggered(ruleTriggered);
        response.setWarningMessage(warningMsg);
        response.setCurrentTabSwitches(attempt.getTabSwitchCount());
        response.setMaxTabSwitches(tabLimit);
        response.setCurrentFullscreenExits(attempt.getFullscreenExitCount());
        response.setMaxFullscreenExits(fullscreenLimit);
        response.setScreenShareActive(attempt.getScreenShareActive());
        response.setRequiresAction("VIOLATION".equals(severity));

        return response;
    }

    @Transactional
    public ExamAttempt recordScreenShareConsent(User student, Integer attemptId, ScreenShareConsentRequest request) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam attempt not found: " + attemptId));

        if (!student.getId().equals(attempt.getStudent().getId())) {
            throw new AccessDeniedException("You do not have permission for this attempt");
        }

        boolean consent = request != null && Boolean.TRUE.equals(request.getConsentGiven());
        attempt.setScreenShareConsentGiven(consent);
        attempt.setScreenShareConsentAt(consent ? LocalDateTime.now() : null);

        // Log consent event (FR-S59)
        ExamMonitoringLog log = new ExamMonitoringLog(
                attempt, student, attempt.getExam(), "SCREEN_SHARE_CONSENT_" + (consent ? "GRANTED" : "DENIED"),
                LocalDateTime.now(), 0, "Student " + (consent ? "consented to" : "declined") + " screen sharing",
                "INFO", null
        );
        monitoringLogRepository.save(log);

        return examAttemptRepository.save(attempt);
    }

    @Transactional(readOnly = true)
    public List<ExamMonitoringLogDto> getAttemptLogs(User currentUser, Integer attemptId) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam attempt not found: " + attemptId));

        // DR-05: Student academic records visible only to student, course instructors, and admins
        boolean isOwner = currentUser.getId().equals(attempt.getStudent().getId());
        boolean isAdmin = "admin".equalsIgnoreCase(currentUser.getRole());
        boolean isInstructor = courseInstructorRepository != null &&
                courseInstructorRepository.existsByCourseIdAndInstructorId(attempt.getExam().getCourse().getId(), currentUser.getId());

        if (!isOwner && !isAdmin && !isInstructor) {
            throw new AccessDeniedException("You do not have permission to view activity logs for this attempt");
        }

        List<ExamMonitoringLog> logs = monitoringLogRepository.findByAttemptIdOrderByTimestampAsc(attemptId);
        return logs.stream().map(ExamMonitoringLogDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExamQuestionItemDto> getStudentAttemptQuestions(User student, Integer attemptId) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam attempt not found: " + attemptId));

        if (!student.getId().equals(attempt.getStudent().getId())) {
            throw new AccessDeniedException("You do not have permission to view questions for this attempt");
        }

        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(attempt.getExam().getId());
        Map<Integer, ExamQuestion> eqMap = examQuestions.stream()
                .collect(Collectors.toMap(eq -> eq.getQuestion().getId(), eq -> eq, (a, b) -> a));

        List<Integer> orderedQuestionIds = new ArrayList<>();
        if (attempt.getQuestionOrder() != null && !attempt.getQuestionOrder().trim().isEmpty()) {
            for (String part : attempt.getQuestionOrder().split(",")) {
                try {
                    orderedQuestionIds.add(Integer.parseInt(part.trim()));
                } catch (NumberFormatException ignored) {}
            }
        }

        Map<String, List<Integer>> shuffleMap = Collections.emptyMap();
        if (attempt.getOptionShuffleMap() != null && !attempt.getOptionShuffleMap().trim().isEmpty()) {
            try {
                shuffleMap = objectMapper.readValue(attempt.getOptionShuffleMap(), new TypeReference<Map<String, List<Integer>>>() {});
            } catch (Exception ignored) {}
        }

        List<ExamQuestionItemDto> result = new ArrayList<>();
        int displayIndex = 1;

        if (!orderedQuestionIds.isEmpty()) {
            for (Integer qId : orderedQuestionIds) {
                ExamQuestion eq = eqMap.get(qId);
                if (eq != null) {
                    Question q = eq.getQuestion();
                    String optionsJson = q.getOptionsJson();
                    if (shuffleMap.containsKey(String.valueOf(qId))) {
                        optionsJson = applyOptionPermutation(optionsJson, shuffleMap.get(String.valueOf(qId)));
                    }
                    result.add(new ExamQuestionItemDto(
                            eq.getId(), q.getId(), q.getQuestionText(),
                            q.getQuestionType() != null ? q.getQuestionType().name() : "MULTIPLE_CHOICE",
                            optionsJson, eq.getMarks(), displayIndex++
                    ));
                }
            }
        } else {
            for (ExamQuestion eq : examQuestions) {
                Question q = eq.getQuestion();
                result.add(new ExamQuestionItemDto(
                        eq.getId(), q.getId(), q.getQuestionText(),
                        q.getQuestionType() != null ? q.getQuestionType().name() : "MULTIPLE_CHOICE",
                        q.getOptionsJson(), eq.getMarks(), eq.getOrderIndex()
                ));
            }
        }

        return result;
    }

    private String applyOptionPermutation(String optionsJson, List<Integer> permutation) {
        if (optionsJson == null || permutation == null || permutation.isEmpty()) return optionsJson;
        try {
            List<String> original = objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
            List<String> permuted = new ArrayList<>();
            for (int idx : permutation) {
                if (idx >= 0 && idx < original.size()) {
                    permuted.add(original.get(idx));
                }
            }
            return objectMapper.writeValueAsString(permuted);
        } catch (Exception e) {
            return optionsJson;
        }
    }

    private void appendMonitoringFlag(ExamAttempt attempt, String flag) {
        if (attempt.getMonitoringFlags() == null || attempt.getMonitoringFlags().trim().isEmpty()) {
            attempt.setMonitoringFlags(flag);
        } else if (!attempt.getMonitoringFlags().contains(flag)) {
            attempt.setMonitoringFlags(attempt.getMonitoringFlags() + "," + flag);
        }
    }
}
