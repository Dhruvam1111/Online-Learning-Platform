package com.olp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StudentExamAnswerRequest(@NotNull Integer questionId,
                                       @NotNull @Size(max = 20000) String answerText) {}
