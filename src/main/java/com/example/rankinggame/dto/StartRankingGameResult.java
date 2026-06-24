package com.example.rankinggame.dto;

import com.example.rankinggame.entities.GameType;

import java.util.UUID;

public record StartRankingGameResult(
        StartedRoom room,
        StartedGame game,
        StartedRound round
) {
    public record StartedRoom(UUID id, String code) {
    }

    public record StartedGame(UUID id, GameType gameType) {
    }

    public record StartedRound(UUID id, int number, UUID questionId) {
    }
}
