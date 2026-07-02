package com.example.rankinggame.usecases;

public record AnswerSubmissionProgress(
        long submittedAnswerCount,
        long requiredAnswerCount,
        boolean sortingHasStarted
) {
}
