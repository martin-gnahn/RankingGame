package com.example.rankinggame.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartGameRequest(
        @JsonAlias("playerId")
        @NotNull(message = "Host player id is required")
        UUID hostPlayerId
) {
}
