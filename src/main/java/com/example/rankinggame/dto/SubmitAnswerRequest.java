package com.example.rankinggame.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitAnswerRequest(
        @NotBlank(message = "Answer text is required")
        @Size(max = 500, message = "Answer text must be 500 characters or fewer")
        String answerText
) {
}
