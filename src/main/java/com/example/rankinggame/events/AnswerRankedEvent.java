package com.example.rankinggame.events;

import com.example.rankinggame.engine.AnswerId;

import java.util.UUID;

public record AnswerRankedEvent(
        String roomCode,
        UUID roundId,
        AnswerId answerId,
        int oneBasedPosition
) {
}
