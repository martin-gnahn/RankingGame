package com.example.rankinggame.dto;

import java.util.UUID;

public record SubmitAnswerCommand(
        String roomCode,
        UUID roundId,
        UUID playerId,
        String answerText
) {
}
