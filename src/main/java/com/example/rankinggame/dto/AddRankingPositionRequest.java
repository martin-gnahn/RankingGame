package com.example.rankinggame.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddRankingPositionRequest(
        @NotNull(message = "Answer id is required")
        UUID answerId
) {
}
