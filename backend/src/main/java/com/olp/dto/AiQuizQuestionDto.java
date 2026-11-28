package com.olp.dto;

import java.util.List;
import java.util.Map;

public record AiQuizQuestionDto(
    Integer id,
    String questionText,
    String questionType,
    List<String> options,
    List<String> correctAnswers,
    String explanation,
    Map<String, String> distractorRationales,
    String timestamp,
    String bloomLevel
) {}
