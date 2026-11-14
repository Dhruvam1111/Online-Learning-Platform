package com.olp.dto;

import com.olp.entity.EvaluationStatus;
import com.olp.entity.QuizAttemptAnswer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a descriptive question or assignment submission in the instructor's grading queue (FR-I30).
 */
public class DescriptiveSubmissionResponse {

    private Integer submissionId;
    private Integer answerId;
    private Integer attemptId;
    private Integer quizId;
    private String quizTitle;
    private Integer courseId;
    private String courseName;
    private String courseTitle;
    private Integer studentId;
    private String studentName;
    private String studentEmail;
    private Integer questionId;
    private String questionText;
    private String questionType;
    private String gradingRubric;
    private String submittedAnswer;
    private BigDecimal maxMarks;
    private BigDecimal awardedMarks;
    private String evaluationStatus;
    private String gradingStatus;
    private String feedback;
    private Integer graderId;
    private String graderName;
    private LocalDateTime evaluatedAt;
    private LocalDateTime gradingTimestamp;
    private LocalDateTime submissionDate;
    private LocalDateTime submittedAt;
    private boolean isGraded;
    private boolean isPublished;

    public DescriptiveSubmissionResponse() {}

    public static DescriptiveSubmissionResponse fromEntity(QuizAttemptAnswer ans) {
        if (ans == null) return null;
        DescriptiveSubmissionResponse dto = new DescriptiveSubmissionResponse();
        dto.setSubmissionId(ans.getId());
        dto.setAnswerId(ans.getId());

        if (ans.getAttempt() != null) {
            dto.setAttemptId(ans.getAttempt().getId());
            dto.setSubmissionDate(ans.getAttempt().getSubmittedAt());
            dto.setSubmittedAt(ans.getAttempt().getSubmittedAt());

            if (ans.getAttempt().getStudent() != null) {
                dto.setStudentId(ans.getAttempt().getStudent().getId());
                dto.setStudentName(ans.getAttempt().getStudent().getName());
                dto.setStudentEmail(ans.getAttempt().getStudent().getEmail());
            }

            if (ans.getAttempt().getQuiz() != null) {
                dto.setQuizId(ans.getAttempt().getQuiz().getId());
                dto.setQuizTitle(ans.getAttempt().getQuiz().getTitle());

                if (ans.getAttempt().getQuiz().getCourse() != null) {
                    dto.setCourseId(ans.getAttempt().getQuiz().getCourse().getId());
                    dto.setCourseTitle(ans.getAttempt().getQuiz().getCourse().getTitle());
                    dto.setCourseName(ans.getAttempt().getQuiz().getCourse().getTitle());
                }
            }
        }

        if (ans.getQuestion() != null) {
            dto.setQuestionId(ans.getQuestion().getId());
            dto.setQuestionText(ans.getQuestion().getQuestionText());
            dto.setQuestionType(ans.getQuestion().getQuestionType() != null ? ans.getQuestion().getQuestionType().name() : null);
            dto.setGradingRubric(ans.getQuestion().getGradingRubric());
        }

        dto.setSubmittedAnswer(ans.getSubmittedAnswer());
        dto.setMaxMarks(ans.getMaxMarks());
        dto.setAwardedMarks(ans.getAwardedMarks());

        EvaluationStatus status = ans.getEvaluationStatus();
        dto.setEvaluationStatus(status != null ? status.name() : null);
        dto.setGradingStatus(status != null ? status.name() : null);

        dto.setFeedback(ans.getFeedback());

        if (ans.getGrader() != null) {
            dto.setGraderId(ans.getGrader().getId());
            dto.setGraderName(ans.getGrader().getName());
        }

        dto.setEvaluatedAt(ans.getEvaluatedAt());
        dto.setGradingTimestamp(ans.getEvaluatedAt());

        boolean graded = status == EvaluationStatus.EVALUATED_UNPUBLISHED || status == EvaluationStatus.PUBLISHED
                || status == EvaluationStatus.CORRECT || status == EvaluationStatus.INCORRECT || status == EvaluationStatus.PARTIALLY_CORRECT;
        dto.setGraded(graded);
        dto.setPublished(status == EvaluationStatus.PUBLISHED);

        return dto;
    }

    public Integer getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Integer submissionId) {
        this.submissionId = submissionId;
        this.answerId = submissionId;
    }

    public Integer getAnswerId() {
        return answerId != null ? answerId : submissionId;
    }

    public void setAnswerId(Integer answerId) {
        this.answerId = answerId;
        if (this.submissionId == null) {
            this.submissionId = answerId;
        }
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public Integer getQuizId() {
        return quizId;
    }

    public void setQuizId(Integer quizId) {
        this.quizId = quizId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName != null ? courseName : courseTitle;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
        if (this.courseTitle == null) {
            this.courseTitle = courseName;
        }
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
        if (this.courseName == null) {
            this.courseName = courseTitle;
        }
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getGradingRubric() {
        return gradingRubric;
    }

    public void setGradingRubric(String gradingRubric) {
        this.gradingRubric = gradingRubric;
    }

    public String getSubmittedAnswer() {
        return submittedAnswer;
    }

    public void setSubmittedAnswer(String submittedAnswer) {
        this.submittedAnswer = submittedAnswer;
    }

    public BigDecimal getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(BigDecimal maxMarks) {
        this.maxMarks = maxMarks;
    }

    public BigDecimal getAwardedMarks() {
        return awardedMarks;
    }

    public void setAwardedMarks(BigDecimal awardedMarks) {
        this.awardedMarks = awardedMarks;
    }

    public String getEvaluationStatus() {
        return evaluationStatus;
    }

    public void setEvaluationStatus(String evaluationStatus) {
        this.evaluationStatus = evaluationStatus;
        this.gradingStatus = evaluationStatus;
    }

    public String getGradingStatus() {
        return gradingStatus != null ? gradingStatus : evaluationStatus;
    }

    public void setGradingStatus(String gradingStatus) {
        this.gradingStatus = gradingStatus;
        if (this.evaluationStatus == null) {
            this.evaluationStatus = gradingStatus;
        }
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Integer getGraderId() {
        return graderId;
    }

    public void setGraderId(Integer graderId) {
        this.graderId = graderId;
    }

    public String getGraderName() {
        return graderName;
    }

    public void setGraderName(String graderName) {
        this.graderName = graderName;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
        this.gradingTimestamp = evaluatedAt;
    }

    public LocalDateTime getGradingTimestamp() {
        return gradingTimestamp != null ? gradingTimestamp : evaluatedAt;
    }

    public void setGradingTimestamp(LocalDateTime gradingTimestamp) {
        this.gradingTimestamp = gradingTimestamp;
        if (this.evaluatedAt == null) {
            this.evaluatedAt = gradingTimestamp;
        }
    }

    public LocalDateTime getSubmissionDate() {
        return submissionDate != null ? submissionDate : submittedAt;
    }

    public void setSubmissionDate(LocalDateTime submissionDate) {
        this.submissionDate = submissionDate;
        if (this.submittedAt == null) {
            this.submittedAt = submissionDate;
        }
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
        if (this.submissionDate == null) {
            this.submissionDate = submittedAt;
        }
    }

    public boolean isGraded() {
        return isGraded;
    }

    public void setGraded(boolean graded) {
        isGraded = graded;
    }

    public boolean isPublished() {
        return isPublished;
    }

    public void setPublished(boolean published) {
        isPublished = published;
    }
}
