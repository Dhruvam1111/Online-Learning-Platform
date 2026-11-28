package com.olp.dto;

public record FlashcardDto(
    Integer id,
    String front,
    String back,
    String topic
) {}
