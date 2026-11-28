package com.olp.dto;

public record GlossaryItemDto(
    String term,
    String formalDefinition,
    String intuition,
    String timestamp
) {}
