package com.example.rankinggame.usecases;

public record AnswerRankingProgress(
        long rankedAnswerCount,
        long submittedAnswerCount,
        boolean allSubmittedAnswersRanked,
        boolean resultHasStarted
) {
}
