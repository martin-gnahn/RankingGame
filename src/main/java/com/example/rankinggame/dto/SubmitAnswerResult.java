package com.example.rankinggame.dto;

import java.util.UUID;

public record SubmitAnswerResult(
        UUID answerId,
        UUID roundId,
        UUID playerId,
        boolean submitted
) {
}
