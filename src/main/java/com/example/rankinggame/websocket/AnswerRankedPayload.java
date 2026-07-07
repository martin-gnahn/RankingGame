package com.example.rankinggame.websocket;

import java.util.UUID;

public record AnswerRankedPayload(
        UUID roundId,
        UUID answerId,
        int oneBasedPosition
) {
}
