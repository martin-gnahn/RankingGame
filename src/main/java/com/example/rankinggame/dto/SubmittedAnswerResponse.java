package com.example.rankinggame.dto;

import java.util.UUID;

public record SubmittedAnswerResponse(
        UUID answerId,
        UUID playerId,
        String nickname,
        String answerText,
        int cardValue
) {
}
