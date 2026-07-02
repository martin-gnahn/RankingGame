package com.example.rankinggame.usecases;

public record AnswerSubmissionProgress(
        long submittedAnswerCount,
        long requiredAnswerCount,
        boolean allAnswersSubmitted,
        boolean sortingHasStarted
) {
}
