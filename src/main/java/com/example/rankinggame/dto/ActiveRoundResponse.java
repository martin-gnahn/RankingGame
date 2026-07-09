package com.example.rankinggame.dto;

import java.util.UUID;

public record ActiveRoundResponse(
        UUID roomId,
        String roomCode,
        UUID gameSessionId,
        UUID roundId,
        int roundNumber,
        UUID questionId,
        String questionText,
        int assignedCardValue,
        boolean currentPlayerSubmitted,
        boolean currentPlayerIsCaptain) {
    public static ActiveRoundResponse from(ActiveRoundResult result) {
        return new ActiveRoundResponse(
                result.roomId(),
                result.roomCode(),
                result.gameSessionId(),
                result.roundId(),
                result.roundNumber(),
                result.questionId(),
                result.questionText(),
                result.assignedCardValue(),
                result.currentPlayerSubmitted(),
                result.currentPlayerIsCaptain()
        );
    }
}
