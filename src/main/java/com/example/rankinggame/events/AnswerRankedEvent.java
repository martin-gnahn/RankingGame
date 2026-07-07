package com.example.rankinggame.events;

import com.example.rankinggame.engine.AnswerId;

import java.util.UUID;

public record AnswerRankedEvent(
        UUID roundId,
        AnswerId answerId,
        int oneBasedPosition
) {
}
