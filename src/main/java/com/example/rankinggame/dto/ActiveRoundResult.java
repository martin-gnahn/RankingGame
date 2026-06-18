package com.example.rankinggame.dto;

import java.util.UUID;

public record ActiveRoundResult(
        UUID roomId,
        String roomCode,
        UUID gameSessionId,
        UUID roundId,
        int roundNumber,
        UUID questionId,
        String questionText,
        int assignedCardValue
) {
}
