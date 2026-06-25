package com.example.rankinggame.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SortAnswersRequest(
        @NotNull(message = "Host player id is required")
        UUID hostPlayerId
) {
}
