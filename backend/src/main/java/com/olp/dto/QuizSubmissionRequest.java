package com.olp.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO submitted by a student when completing a quiz attempt (FR-I22).
 */
public class QuizSubmissionRequest {

    private List<QuestionAnswerSubmission> answers = new ArrayList<>();
    private GradingPolicy policy;

    public QuizSubmissionRequest() {}

    public QuizSubmissionRequest(List<QuestionAnswerSubmission> answers) {
        this.answers = answers != null ? answers : new ArrayList<>();
    }

    public List<QuestionAnswerSubmission> getAnswers() {
        return answers;
    }

    public void setAnswers(List<QuestionAnswerSubmission> answers) {
        this.answers = answers != null ? answers : new ArrayList<>();
    }

    public GradingPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(GradingPolicy policy) {
        this.policy = policy;
    }
}

