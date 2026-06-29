package com.example.rankinggame.events;

import java.util.UUID;

public record AnswerSubmittedEvent(
        String roomCode,
        UUID roundId,
        long submittedAnswerCount,
        long requiredAnswerCount,
        boolean allAnswersSubmitted
) {
}
