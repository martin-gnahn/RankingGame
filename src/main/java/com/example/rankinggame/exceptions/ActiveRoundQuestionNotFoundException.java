package com.example.rankinggame.exceptions;

import java.util.UUID;

public class ActiveRoundQuestionNotFoundException extends RuntimeException {
    public ActiveRoundQuestionNotFoundException(UUID roundId, UUID questionId) {
        super("Question '%s' for active round '%s' was not found.".formatted(questionId, roundId));
    }
}
