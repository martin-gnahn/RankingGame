package com.example.rankinggame.websocket;

import java.util.UUID;

public record AnswerSubmittedPayload(
        UUID roundId,
        long submittedAnswerCount,
        long requiredAnswerCount,
        boolean allAnswersSubmitted
) {
}
