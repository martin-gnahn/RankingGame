package com.example.rankinggame.usecases;

import java.util.UUID;

public record StartRankingGameCommand(
        String roomCode,
        UUID hostPlayerId
) {
}
