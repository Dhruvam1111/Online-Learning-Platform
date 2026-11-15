package com.olp.dto;

import com.olp.entity.QuestionCorrectionPolicy;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for configuring or updating question correction policy (FR-I66, Step 41).
 */
public class QuestionCorrectionPolicyRequest {

    @NotNull(message = "Question correction policy is required")
    private QuestionCorrectionPolicy policy;

    public QuestionCorrectionPolicyRequest() {}

    public QuestionCorrectionPolicyRequest(QuestionCorrectionPolicy policy) {
        this.policy = policy;
    }

    public QuestionCorrectionPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(QuestionCorrectionPolicy policy) {
        this.policy = policy;
    }
}
