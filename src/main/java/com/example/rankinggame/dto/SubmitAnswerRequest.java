package com.example.rankinggame.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SubmitAnswerRequest(
        @NotNull(message = "Player id is required")
        UUID playerId,

        @NotBlank(message = "Answer text is required")
        @Size(max = 500, message = "Answer text must be 500 characters or fewer")
        String answerText
) {
}
