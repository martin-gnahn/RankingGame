package com.example.rankinggame.engine.exceptions;

public class RankingAssessmentInputRequiredException extends IllegalArgumentException {
    public RankingAssessmentInputRequiredException(RankingAssessmentInput missingInput) {
        super("Ranking assessment requires %s".formatted(missingInput.label()));
    }
}
