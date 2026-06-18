package com.example.rankinggame.dto;

import com.example.rankinggame.entities.GameType;

import java.util.UUID;

public record StartRankingGameResult(
        UUID roomId,
        String roomCode,
        UUID gameSessionId,
        GameType gameType,
        UUID roundId,
        int roundNumber,
        UUID questionId
) {
}
