package com.example.rankinggame.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SubmitAnswerRequest(
        @NotNull(message = "Player id is required")
        UUID playerId,

        @NotBlank(message = "Answer text is required")
        @Size(max = 500, message = "Answer text must be 500 characters or fewer")
        String answerText,

        @Min(value = 1, message = "Card value must be between 1 and 10")
        @Max(value = 10, message = "Card value must be between 1 and 10")
        int cardValue
) {
}
