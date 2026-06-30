package com.example.rankinggame.dto;

import java.util.UUID;

public record ActiveRoundResult(
        UUID roomId,
        String roomCode,
        UUID gameSessionId,
        UUID roundId,
        /// roundNumber is 1-based
        int roundNumber,
        UUID questionId,
        String questionText,
        int assignedCardValue
) {
}
