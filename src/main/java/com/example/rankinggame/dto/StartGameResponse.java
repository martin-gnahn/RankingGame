package com.example.rankinggame.dto;

import java.util.UUID;

public record StartGameResponse(
        UUID roomId,
        String roomCode,
        UUID gameSessionId,
        String gameType,
        UUID roundId,
        int roundNumber,
        UUID questionId
) {
}
