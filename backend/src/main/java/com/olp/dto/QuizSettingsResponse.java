package com.olp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO exposing authoritative Quiz Settings and availability/attempt metadata
 * for instructor configuration and student-side quiz execution (FR-I20, S19, S22, S24).
 */
public class QuizSettingsResponse {

    private Integer quizId;
    private Integer courseId;
    private String title;
    private String status;
    private Integer timeLimitMinutes;
    private Integer numberOfQuestions;
    private Integer linkedQuestionCount;
    private BigDecimal totalMarks;
    private BigDecimal calculatedQuestionMarksSum;
    private boolean marksConsistent;
    private BigDecimal passingMarks;
    private Integer maxAttempts;
    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;
    private boolean currentlyAvailable;
    private String availabilityMessage;
    private LocalDateTime serverTime;
    private Long attemptsUsed;
    private Integer remainingAttempts;
    private Boolean feedbackReleased;
    private Boolean negativeMarking;
    private Boolean partialMarking;
    private BigDecimal numericTolerance;
    private String markingRulesSummary;
    private Boolean showCorrectAnswers;
    private Boolean showFeedback;
    private String answerReleaseTiming;
    private Boolean isExam;
    private Boolean correctAnswersVisible;
    private Boolean feedbackVisible;

    public QuizSettingsResponse() {}

    public Integer getQuizId() {
        return quizId;
    }

    public void setQuizId(Integer quizId) {
        this.quizId = quizId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTimeLimitMinutes() {
        return timeLimitMinutes;
    }

    public void setTimeLimitMinutes(Integer timeLimitMinutes) {
        this.timeLimitMinutes = timeLimitMinutes;
    }

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public Integer getLinkedQuestionCount() {
        return linkedQuestionCount;
    }

    public void setLinkedQuestionCount(Integer linkedQuestionCount) {
        this.linkedQuestionCount = linkedQuestionCount;
    }

    public BigDecimal getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(BigDecimal totalMarks) {
        this.totalMarks = totalMarks;
    }

    public BigDecimal getCalculatedQuestionMarksSum() {
        return calculatedQuestionMarksSum;
    }

    public void setCalculatedQuestionMarksSum(BigDecimal calculatedQuestionMarksSum) {
        this.calculatedQuestionMarksSum = calculatedQuestionMarksSum;
    }

    public boolean isMarksConsistent() {
        return marksConsistent;
    }

    public void setMarksConsistent(boolean marksConsistent) {
        this.marksConsistent = marksConsistent;
    }

    public BigDecimal getPassingMarks() {
        return passingMarks;
    }

    public void setPassingMarks(BigDecimal passingMarks) {
        this.passingMarks = passingMarks;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public LocalDateTime getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(LocalDateTime availableFrom) {
        this.availableFrom = availableFrom;
    }

    public LocalDateTime getAvailableUntil() {
        return availableUntil;
    }

    public void setAvailableUntil(LocalDateTime availableUntil) {
        this.availableUntil = availableUntil;
    }

    public boolean isCurrentlyAvailable() {
        return currentlyAvailable;
    }

    public void setCurrentlyAvailable(boolean currentlyAvailable) {
        this.currentlyAvailable = currentlyAvailable;
    }

    public String getAvailabilityMessage() {
        return availabilityMessage;
    }

    public void setAvailabilityMessage(String availabilityMessage) {
        this.availabilityMessage = availabilityMessage;
    }

    public LocalDateTime getServerTime() {
        return serverTime;
    }

    public void setServerTime(LocalDateTime serverTime) {
        this.serverTime = serverTime;
    }

    public Long getAttemptsUsed() {
        return attemptsUsed;
    }

    public void setAttemptsUsed(Long attemptsUsed) {
        this.attemptsUsed = attemptsUsed;
    }

    public Integer getRemainingAttempts() {
        return remainingAttempts;
    }

    public void setRemainingAttempts(Integer remainingAttempts) {
        this.remainingAttempts = remainingAttempts;
    }

    public Boolean getFeedbackReleased() {
        return feedbackReleased;
    }

    public void setFeedbackReleased(Boolean feedbackReleased) {
        this.feedbackReleased = feedbackReleased;
    }

    public Boolean getNegativeMarking() {
        return negativeMarking;
    }

    public void setNegativeMarking(Boolean negativeMarking) {
        this.negativeMarking = negativeMarking;
    }

    public Boolean getPartialMarking() {
        return partialMarking;
    }

    public void setPartialMarking(Boolean partialMarking) {
        this.partialMarking = partialMarking;
    }

    public BigDecimal getNumericTolerance() {
        return numericTolerance;
    }

    public void setNumericTolerance(BigDecimal numericTolerance) {
        this.numericTolerance = numericTolerance;
    }

    public String getMarkingRulesSummary() {
        return markingRulesSummary;
    }

    public void setMarkingRulesSummary(String markingRulesSummary) {
        this.markingRulesSummary = markingRulesSummary;
    }

    public Boolean getShowCorrectAnswers() {
        return showCorrectAnswers;
    }

    public void setShowCorrectAnswers(Boolean showCorrectAnswers) {
        this.showCorrectAnswers = showCorrectAnswers;
    }

    public Boolean getShowFeedback() {
        return showFeedback;
    }

    public void setShowFeedback(Boolean showFeedback) {
        this.showFeedback = showFeedback;
    }

    public String getAnswerReleaseTiming() {
        return answerReleaseTiming;
    }

    public void setAnswerReleaseTiming(String answerReleaseTiming) {
        this.answerReleaseTiming = answerReleaseTiming;
    }

    public Boolean getIsExam() {
        return isExam;
    }

    public void setIsExam(Boolean exam) {
        this.isExam = exam;
    }

    public Boolean getCorrectAnswersVisible() {
        return correctAnswersVisible;
    }

    public void setCorrectAnswersVisible(Boolean correctAnswersVisible) {
        this.correctAnswersVisible = correctAnswersVisible;
    }

    public Boolean getFeedbackVisible() {
        return feedbackVisible;
    }

    public void setFeedbackVisible(Boolean feedbackVisible) {
        this.feedbackVisible = feedbackVisible;
    }

    public static String computeMarkingRulesSummary(Boolean negativeMarking, Boolean partialMarking, BigDecimal numericTolerance) {
        java.util.List<String> rules = new java.util.ArrayList<>();
        if (Boolean.TRUE.equals(negativeMarking)) {
            rules.add("Negative marking is ENABLED (deductions apply for incorrect answers)");
        } else {
            rules.add("Negative marking is DISABLED (no marks deducted for incorrect answers)");
        }
        if (Boolean.TRUE.equals(partialMarking)) {
            rules.add("Partial marking is ENABLED for multiple-select questions");
        } else {
            rules.add("Partial marking is DISABLED (all-or-nothing for multiple-select questions)");
        }
        if (numericTolerance != null && numericTolerance.compareTo(BigDecimal.ZERO) > 0) {
            rules.add("Numerical tolerance: \u00b1" + numericTolerance.stripTrailingZeros().toPlainString());
        }
        return String.join(". ", rules) + ".";
    }
}

