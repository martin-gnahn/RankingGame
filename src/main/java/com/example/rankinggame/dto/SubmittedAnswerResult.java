package com.example.rankinggame.dto;

import java.util.UUID;

public record SubmittedAnswerResult(
        UUID answerId,
        UUID playerId,
        String nickname,
        String answerText
) {
}
